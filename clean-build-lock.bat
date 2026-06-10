@echo off
setlocal EnableExtensions

REM Release locks on Gradle build output and delete the build folder.
REM Double-click or run from a terminal in any directory.

cd /d "%~dp0"
set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"

echo.
echo ==^> QIO-Integrations: clean locked build directory
echo.

where powershell >nul 2>&1
if %ERRORLEVEL% equ 0 (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0clean-build-lock.ps1" -ProjectRoot "%PROJECT_ROOT%"
    set "EXIT_CODE=%ERRORLEVEL%"
    goto :finish
)

echo PowerShell not found; using batch fallback...
echo.

if exist "%PROJECT_ROOT%\gradlew.bat" (
    echo ==^> Stopping Gradle daemons
    pushd "%PROJECT_ROOT%"
    call gradlew.bat --stop
    popd
    timeout /t 2 /nobreak >nul
)

echo ==^> Stopping Java processes tied to Gradle / this project
for /f "skip=1 tokens=1" %%P in ('wmic process where "name='java.exe' or name='javaw.exe'" get ProcessId 2^>nul') do (
    if not "%%P"=="" (
        for /f "delims=" %%C in ('wmic process where "ProcessId=%%P" get CommandLine /value 2^>nul ^| findstr /i "^CommandLine="') do (
            echo %%C | findstr /i /c:"gradle" /c:"QIO-Integrations" /c:"GradleDaemon" >nul && (
                echo   Stopping PID %%P
                taskkill /F /PID %%P >nul 2>&1
            )
        )
    )
)

timeout /t 1 /nobreak >nul

set "BUILD_DIR=%PROJECT_ROOT%\build"
if not exist "%BUILD_DIR%" (
    echo Build folder does not exist.
    set "EXIT_CODE=0"
    goto :finish
)

echo ==^> Removing build directory
rd /s /q "%BUILD_DIR%" 2>nul
if not exist "%BUILD_DIR%" goto :success

echo   rd failed; trying robocopy mirror...
set "EMPTY_DIR=%TEMP%\empty_%RANDOM%%RANDOM%"
mkdir "%EMPTY_DIR%" 2>nul
robocopy "%EMPTY_DIR%" "%BUILD_DIR%" /mir /r:2 /w:1 /njh /njs /ndl /nfl /np >nul
rd /s /q "%EMPTY_DIR%" 2>nul
rd /s /q "%BUILD_DIR%" 2>nul
if not exist "%BUILD_DIR%" goto :success

echo.
echo Could not remove build directory.
echo Close IDE, Minecraft runClient, and pause OneDrive sync, then run again.
set "EXIT_CODE=1"
goto :finish

:success
echo.
echo Done. Build directory removed.
set "EXIT_CODE=0"

:finish
echo.
if not "%NO_PAUSE%"=="1" pause
exit /b %EXIT_CODE%
