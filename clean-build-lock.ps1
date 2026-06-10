#Requires -Version 5.1
<#
.SYNOPSIS
  Releases locks on the Gradle build output and removes the build directory.

.DESCRIPTION
  Stops Gradle daemons, terminates Java processes tied to this project, then
  deletes build output (with retries and a robocopy fallback for stubborn locks).

.PARAMETER ProjectRoot
  Root of the mod project. Defaults to the directory containing this script.

.PARAMETER BuildPath
  Path to remove. Defaults to "<ProjectRoot>\build".

.PARAMETER SkipKillJava
  Only run "gradlew --stop"; do not force-stop matching java/javaw processes.
#>
[CmdletBinding()]
param(
    [string] $ProjectRoot = '',
    [string] $BuildPath = '',
    [switch] $SkipKillJava
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = $PSScriptRoot
    if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
        $ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
    }
}

function Write-Step([string] $Message) {
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Stop-ProjectJavaProcesses {
    param([string] $Root)
    $escaped = [regex]::Escape($Root)
    $names = @('java.exe', 'javaw.exe')
    foreach ($name in $names) {
        $procs = Get-CimInstance Win32_Process -Filter "Name = '$name'" -ErrorAction SilentlyContinue
        foreach ($proc in $procs) {
            $cmd = $proc.CommandLine
            if ([string]::IsNullOrWhiteSpace($cmd)) { continue }
            if ($cmd -match $escaped -or $cmd -match 'gradle|GradleDaemon|org\.gradle') {
                Write-Host "  Stopping PID $($proc.ProcessId) ($name)" -ForegroundColor Yellow
                Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

function Remove-DirectoryForce {
    param([string] $Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        Write-Host "  Already gone: $Path" -ForegroundColor DarkGray
        return $true
    }

    for ($attempt = 1; $attempt -le 5; $attempt++) {
        try {
            Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction Stop
            return $true
        }
        catch {
            if ($attempt -eq 5) { break }
            $waitMs = 400 * $attempt
            Write-Host "  Delete attempt $attempt failed; retrying in ${waitMs}ms..." -ForegroundColor DarkYellow
            Start-Sleep -Milliseconds $waitMs
        }
    }

    Write-Host "  Using robocopy mirror fallback..." -ForegroundColor Yellow
    $empty = Join-Path $env:TEMP ("empty_{0}" -f [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $empty -Force | Out-Null
    try {
        & robocopy $empty $Path /mir /r:2 /w:1 /njh /njs /ndl /nfl /np | Out-Null
        Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction Stop
        return $true
    }
    finally {
        if (Test-Path -LiteralPath $empty) {
            Remove-Item -LiteralPath $empty -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
}

$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
if ([string]::IsNullOrWhiteSpace($BuildPath)) {
    $BuildPath = Join-Path $ProjectRoot 'build'
}
else {
    $resolved = Resolve-Path -LiteralPath $BuildPath -ErrorAction SilentlyContinue
    if ($resolved) { $BuildPath = $resolved.Path }
}

# Avoid deleting if our shell cwd is inside the target tree.
$here = (Get-Location).Path
if ($here.StartsWith($BuildPath, [StringComparison]::OrdinalIgnoreCase)) {
    Set-Location -LiteralPath $ProjectRoot
}

Write-Step "Project: $ProjectRoot"
Write-Step "Target:  $BuildPath"

$gradlew = Join-Path $ProjectRoot 'gradlew.bat'
if (Test-Path -LiteralPath $gradlew) {
    Write-Step 'Stopping Gradle daemons (gradlew --stop)'
    Push-Location -LiteralPath $ProjectRoot
    try {
        & $gradlew --stop 2>&1 | ForEach-Object { Write-Host "  $_" }
    }
    finally {
        Pop-Location
    }
    Start-Sleep -Seconds 2
}
else {
    Write-Host "  gradlew.bat not found; skipping --stop" -ForegroundColor DarkGray
}

if (-not $SkipKillJava) {
    Write-Step 'Stopping Java processes for this project / Gradle'
    Stop-ProjectJavaProcesses -Root $ProjectRoot
    Start-Sleep -Seconds 1
}

Write-Step 'Removing build directory'
if (Remove-DirectoryForce -Path $BuildPath) {
    Write-Host ''
    Write-Host 'Done. Build directory removed.' -ForegroundColor Green
    exit 0
}

Write-Host ''
Write-Host 'Could not remove build directory. Common causes:' -ForegroundColor Red
Write-Host '  - IDE (IntelliJ / VS Code / Cursor) still indexing or holding class files'
Write-Host '  - Minecraft client still running from gradlew runClient'
Write-Host '  - OneDrive syncing files under the project'
Write-Host 'Close those, then run this script again.'
exit 1
