@echo off
setlocal
rem Removes outdated module jars from deploy\modules, keeping the newest of each.
rem Usage: clean-deploy-modules.bat (path) (-DryRun) (-Force)
rem   clean-deploy-modules.bat -DryRun     preview what would be removed
rem   clean-deploy-modules.bat -Force      remove without asking

set "SCRIPT=%~dp0clean-deploy-modules.ps1"
if not exist "%SCRIPT%" (
    echo clean-deploy-modules.ps1 not found next to this file.
    exit /b 1
)

where pwsh >nul 2>&1
if %ERRORLEVEL% equ 0 (
    pwsh -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" %*
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" %*
)
exit /b %ERRORLEVEL%
