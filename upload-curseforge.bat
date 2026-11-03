@echo off
setlocal
rem Uploads the StreamlineCore mod jars in deploy\ to CurseForge (see upload-curseforge.ps1).
rem Usage: upload-curseforge.bat [Platform ...] [-Version <v>] [-Changelog <text>]
rem                              [-ReleaseType release|beta|alpha] [-DeployDir <dir>]
rem                              [-ThrottleLimit <n>] [-WhatIf]
rem Needs CURSEFORGE_TOKEN set to a CurseForge upload API token.

if not exist "%~dp0upload-curseforge.ps1" (
    echo upload-curseforge.ps1 not found next to this file.
    exit /b 1
)

where pwsh >nul 2>nul
if %ERRORLEVEL%==0 (
    pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0upload-curseforge.ps1" %*
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0upload-curseforge.ps1" %*
)
exit /b %ERRORLEVEL%
