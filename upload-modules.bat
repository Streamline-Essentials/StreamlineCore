@echo off
setlocal
rem Uploads built module jars to the Streamline module registry (see upload-modules.ps1).
rem Usage: upload-modules.bat [Module ...] [-Jar <jar> ...] [-BaseUrl <url>] [-Build]
rem                           [-ThrottleLimit <n>] [-WhatIf]
rem Needs SLMODULES_MASTER_TOKEN set to the registry master key.

if not exist "%~dp0upload-modules.ps1" (
    echo upload-modules.ps1 not found next to this file.
    exit /b 1
)

where pwsh >nul 2>nul
if %ERRORLEVEL%==0 (
    pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0upload-modules.ps1" %*
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0upload-modules.ps1" %*
)
exit /b %ERRORLEVEL%
