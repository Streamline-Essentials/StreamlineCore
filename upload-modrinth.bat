@echo off
setlocal
rem Uploads the StreamlineCore platform jars in deploy\ to Modrinth (see upload-modrinth.ps1).
rem Usage: upload-modrinth.bat [Platform ...] [-Version <v>] [-Changelog <text>]
rem                            [-VersionType release|beta|alpha] [-DeployDir <dir>]
rem                            [-ThrottleLimit <n>] [-WhatIf]
rem Needs MODRINTH_TOKEN set to a Modrinth personal access token.

if not exist "%~dp0upload-modrinth.ps1" (
    echo upload-modrinth.ps1 not found next to this file.
    exit /b 1
)

where pwsh >nul 2>nul
if %ERRORLEVEL%==0 (
    pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0upload-modrinth.ps1" %*
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0upload-modrinth.ps1" %*
)
exit /b %ERRORLEVEL%
