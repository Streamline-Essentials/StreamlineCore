#Requires -Version 5.1
<#
.SYNOPSIS
  Releases locks on Gradle/Loom build outputs and removes all build directories.

.DESCRIPTION
  Stops Gradle daemons, terminates Java processes tied to this project, then
  deletes every "build" folder under the project (root + fabric/forge/neoforge
  modules, etc.) with retries, attribute clearing, and a robocopy fallback for
  stubborn OneDrive / Loom locks.

.PARAMETER ProjectRoot
  Root of the StreamlineCore project. Defaults to the directory containing this script.

.PARAMETER BuildPath
  Optional single path to remove instead of scanning for all build directories.

.PARAMETER SkipKillJava
  Only run "gradlew --stop"; do not force-stop matching java/javaw processes.

.PARAMETER AlsoClean
  Extra relative paths to remove after build dirs (default: .gradle/caches when -Deep).

.PARAMETER Deep
  Also remove "<ProjectRoot>\.gradle" (forces a cold Gradle/Loom cache rebuild).
#>
[CmdletBinding()]
param(
    [string] $ProjectRoot = '',
    [string] $BuildPath = '',
    [switch] $SkipKillJava,
    [switch] $Deep
)

$ErrorActionPreference = 'Continue'

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
            if ($cmd -match $escaped -or $cmd -match 'gradle|GradleDaemon|org\.gradle|net\.fabricmc\.loom|LoomDaemon') {
                Write-Host "  Stopping PID $($proc.ProcessId) ($name)" -ForegroundColor Yellow
                Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

function Clear-DirectoryAttributes {
    param([string] $Path)
    if (-not (Test-Path -LiteralPath $Path)) { return }
    try {
        # Clear ReadOnly/System/Hidden on the tree so OneDrive / extract caches unlock.
        & cmd.exe /c "attrib -R -S -H `"$Path\*`" /S /D" 2>$null | Out-Null
    }
    catch {
        # ignore
    }
}

function Remove-DirectoryForce {
    param([string] $Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        Write-Host "  Already gone: $Path" -ForegroundColor DarkGray
        return $true
    }

    Clear-DirectoryAttributes -Path $Path

    for ($attempt = 1; $attempt -le 8; $attempt++) {
        try {
            # Delete deepest files first — helps with loom-cache / expanded zip locks.
            Get-ChildItem -LiteralPath $Path -Recurse -Force -ErrorAction SilentlyContinue |
                Sort-Object { $_.FullName.Length } -Descending |
                ForEach-Object {
                    try {
                        $_.Attributes = 'Normal'
                        Remove-Item -LiteralPath $_.FullName -Force -Recurse -ErrorAction SilentlyContinue
                    }
                    catch { }
                }

            Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction Stop
            if (-not (Test-Path -LiteralPath $Path)) { return $true }
        }
        catch {
            if ($attempt -eq 8) { break }
            $waitMs = [Math]::Min(2000, 250 * $attempt)
            Write-Host "  Delete attempt $attempt failed; retrying in ${waitMs}ms..." -ForegroundColor DarkYellow
            Start-Sleep -Milliseconds $waitMs
            Clear-DirectoryAttributes -Path $Path
        }
    }

    Write-Host "  Using robocopy mirror fallback..." -ForegroundColor Yellow
    $empty = Join-Path $env:TEMP ("empty_{0}" -f [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $empty -Force | Out-Null
    try {
        & robocopy $empty $Path /mir /r:3 /w:1 /njh /njs /ndl /nfl /np | Out-Null
        Start-Sleep -Milliseconds 300
        Clear-DirectoryAttributes -Path $Path
        Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction SilentlyContinue
        if (-not (Test-Path -LiteralPath $Path)) { return $true }

        # Last resort: rename then delete (breaks some open handles / OneDrive sync).
        $tomb = Join-Path (Split-Path -Parent $Path) ("_delete_me_{0}" -f [guid]::NewGuid().ToString('N'))
        try {
            Rename-Item -LiteralPath $Path -NewName (Split-Path -Leaf $tomb) -Force -ErrorAction Stop
            Remove-Item -LiteralPath $tomb -Recurse -Force -ErrorAction SilentlyContinue
            if (-not (Test-Path -LiteralPath $Path) -and -not (Test-Path -LiteralPath $tomb)) {
                return $true
            }
        }
        catch {
            # ignore
        }
        return -not (Test-Path -LiteralPath $Path)
    }
    finally {
        if (Test-Path -LiteralPath $empty) {
            Remove-Item -LiteralPath $empty -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
}

function Get-BuildDirectories {
    param([string] $Root)

    # Prefer shallow named "build" folders used by Gradle/Loom modules.
    $dirs = Get-ChildItem -LiteralPath $Root -Directory -Recurse -Force -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Name -eq 'build' -and
            $_.FullName -notmatch '[\\/]\.git[\\/]' -and
            $_.FullName -notmatch '[\\/]node_modules[\\/]'
        } |
        Sort-Object { $_.FullName.Length } -Descending

    return @($dirs | ForEach-Object { $_.FullName })
}

$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path

# Avoid deleting if our shell cwd is inside a target tree.
$here = (Get-Location).Path
if ($here.StartsWith($ProjectRoot, [StringComparison]::OrdinalIgnoreCase)) {
    # leave cwd; we will move out of specific build dirs below
}

Write-Step "Project: $ProjectRoot"

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
    Write-Step 'Stopping Java processes for this project / Gradle / Loom'
    Stop-ProjectJavaProcesses -Root $ProjectRoot
    Start-Sleep -Seconds 1
}

$targets = @()
if (-not [string]::IsNullOrWhiteSpace($BuildPath)) {
    $resolved = Resolve-Path -LiteralPath $BuildPath -ErrorAction SilentlyContinue
    if ($resolved) {
        $targets = @($resolved.Path)
    }
    else {
        $targets = @($BuildPath)
    }
}
else {
    Write-Step 'Scanning for build directories'
    $targets = Get-BuildDirectories -Root $ProjectRoot
    if ($targets.Count -eq 0) {
        Write-Host "  No build directories found." -ForegroundColor DarkGray
    }
    else {
        Write-Host "  Found $($targets.Count) build director$(if ($targets.Count -eq 1) { 'y' } else { 'ies' })" -ForegroundColor DarkGray
    }
}

if ($Deep) {
    $gradleUser = Join-Path $ProjectRoot '.gradle'
    if (Test-Path -LiteralPath $gradleUser) {
        $targets += $gradleUser
    }
}

# Leave any cwd that sits inside a target before deleting.
foreach ($t in $targets) {
    if ($here.StartsWith($t, [StringComparison]::OrdinalIgnoreCase)) {
        Set-Location -LiteralPath $ProjectRoot
        $here = $ProjectRoot
        break
    }
}

$failed = @()
foreach ($target in $targets) {
    Write-Step "Removing $target"
    if (-not (Remove-DirectoryForce -Path $target)) {
        $failed += $target
        Write-Host "  FAILED: $target" -ForegroundColor Red
    }
    else {
        Write-Host "  Removed." -ForegroundColor Green
    }
}

Write-Host ''
if ($failed.Count -eq 0) {
    Write-Host 'Done. All build directories removed.' -ForegroundColor Green
    if ($Deep) {
        Write-Host 'Deep clean included .gradle — next build will re-download caches.' -ForegroundColor DarkYellow
    }
    exit 0
}

Write-Host 'Could not remove some directories:' -ForegroundColor Red
foreach ($f in $failed) {
    Write-Host "  - $f" -ForegroundColor Red
}
Write-Host ''
Write-Host 'Common causes:' -ForegroundColor Yellow
Write-Host '  - IDE (IntelliJ / VS Code / Cursor) still indexing or holding class files'
Write-Host '  - Minecraft client still running from Loom/Forge run configs'
Write-Host '  - OneDrive syncing files under the project (pause sync, retry)'
Write-Host '  - A terminal whose cwd is inside a build folder'
Write-Host 'Close those, then run:  .\clean-build.ps1'
Write-Host 'For a colder wipe:       .\clean-build.ps1 -Deep'
exit 1
