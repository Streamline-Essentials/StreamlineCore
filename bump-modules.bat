@echo off
setlocal
rem Lists modules changed since their last version bump, or bumps them (see bump-modules.ps1).
rem Usage: bump-modules.bat [Module|Module=<version> ...] [-Get] [-Changed]
rem                         [-IncludeMetadata] [-Part Major|Minor|Patch]
rem                         [-Version <version>] [-NoCommit]
rem                         [-CoAuthor <name and email>] [-DryRun]
rem With no modules, -Get or -Changed it only lists what changed.

if not exist "%~dp0bump-modules.ps1" (
    echo bump-modules.ps1 not found next to this file.
    exit /b 1
)

where pwsh >nul 2>nul
if %ERRORLEVEL%==0 (
    pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0bump-modules.ps1" %*
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0bump-modules.ps1" %*
)
exit /b %ERRORLEVEL%
