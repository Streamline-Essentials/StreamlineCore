@echo off
setlocal
rem Publishes the StreamlineCore platform jars to the module registry's core builds (see publish-core.ps1).
rem Usage: publish-core.bat [Platform ...] [-Version <version>] [-DeployDir <dir>] [-BaseUrl <url>]
rem                         [-ThrottleLimit <n>] [-WhatIf]
rem Needs SLMODULES_MASTER_TOKEN set to the registry master key.

if not exist "%~dp0publish-core.ps1" (
    echo publish-core.ps1 not found next to this file.
    exit /b 1
)

where pwsh >nul 2>nul
if %ERRORLEVEL%==0 (
    pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0publish-core.ps1" %*
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0publish-core.ps1" %*
)
exit /b %ERRORLEVEL%
