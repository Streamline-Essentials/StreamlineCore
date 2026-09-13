@echo off
setlocal EnableExtensions

REM Release locks on Gradle/Loom build outputs and delete all build folders.
REM Double-click or run from a terminal. Pass -Deep for also wiping .gradle.

cd /d "%~dp0"
set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"

echo.
echo ==^> StreamlineCore: clean locked build directories
echo.

where powershell >nul 2>&1
if %ERRORLEVEL% equ 0 (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0clean-build.ps1" -ProjectRoot "%PROJECT_ROOT%" %*
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
            echo %%C | findstr /i /c:"gradle" /c:"StreamlineCore" /c:"GradleDaemon" /c:"fabricmc.loom" >nul && (
                echo   Stopping PID %%P
                taskkill /F /PID %%P >nul 2>&1
            )
        )
    )
)

timeout /t 1 /nobreak >nul

echo ==^> Removing build directories under project
set "FAILED=0"
for /f "delims=" %%D in ('dir /s /b /ad "%PROJECT_ROOT%\build" 2^>nul') do (
    echo   Removing %%D
    attrib -R -S -H "%%D\*" /S /D >nul 2>&1
    rd /s /q "%%D" 2>nul
    if exist "%%D" (
        set "EMPTY_DIR=%TEMP%\empty_%RANDOM%%RANDOM%"
        mkdir "%EMPTY_DIR%" 2>nul
        robocopy "%EMPTY_DIR%" "%%D" /mir /r:3 /w:1 /njh /njs /ndl /nfl /np >nul
        rd /s /q "%EMPTY_DIR%" 2>nul
        rd /s /q "%%D" 2>nul
    )
    if exist "%%D" (
        echo   FAILED: %%D
        set "FAILED=1"
    )
)

if "%FAILED%"=="0" goto :success

echo.
echo Could not remove some build directories.
echo Close IDE, Minecraft run configs, and pause OneDrive sync, then run again.
set "EXIT_CODE=1"
goto :finish

:success
echo.
echo Done. Build directories removed.
set "EXIT_CODE=0"

:finish
echo.
if not "%NO_PAUSE%"=="1" pause
exit /b %EXIT_CODE%
