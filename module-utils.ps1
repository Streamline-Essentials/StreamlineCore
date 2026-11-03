#Requires -Version 5.1
<#
.SYNOPSIS
    Lists modules changed since their last version bump, and bumps their versions.

.DESCRIPTION
    Each module's version is the `version = ...` line in modules/<Module>/gradle.properties.
    A module's last bump is the newest commit that changed that line. Commits after it are
    "code" changes when they touch anything besides gradle.properties, otherwise
    "metadata" changes (platform tags, group, license, authors).

    With no module names and no -Changed, the script only lists the modules that changed
    since their last bump. Otherwise it bumps each chosen module and commits each bump on
    its own as `bump: <module> version`, touching only the version line.

.PARAMETER Module
    Module folder names under modules/ to bump (case-insensitive). `<Module>=<version>`
    sets that module to an exact version instead.

.PARAMETER Get
    Print versions instead of bumping: just the version for one named module,
    `<Module>=<version>` lines for several (all modules when none are named).

.PARAMETER Changed
    Bump every module with code changes since its last bump.

.PARAMETER IncludeMetadata
    With -Changed (or when listing), also count modules whose only changes are to
    gradle.properties.

.PARAMETER Part
    Which part to raise: Major (x+1.0.0), Minor (x.y+1.0, the default) or Patch (x.y.z+1).

.PARAMETER Version
    An exact version to set instead of raising a part. Only with a single module.

.PARAMETER NoCommit
    Edit the files but do not commit.

.PARAMETER CoAuthor
    Adds a `Co-Authored-By: <value>` trailer to each commit.

.PARAMETER DryRun
    Show what would change without editing or committing anything.

.EXAMPLE
    .\module-utils.ps1
    Lists modules with commits since their last bump.

.EXAMPLE
    .\module-utils.ps1 -Changed -DryRun
    Previews minor bumps of every module with code changes.

.EXAMPLE
    .\module-utils.ps1 StreamQuests TacoEssentials -Part Patch
    Patch-bumps two modules, one commit each.

.EXAMPLE
    .\module-utils.ps1 StreamlineHUD -Version 2.0.0
    Sets one module's version exactly.

.EXAMPLE
    .\module-utils.ps1 StreamQuests=1.8.0 TacoEssentials=1.2.1
    Sets several modules to exact versions, one commit each.

.EXAMPLE
    .\module-utils.ps1 -Get StreamQuests
    Prints StreamQuests' version (e.g. 1.7.0).
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0, ValueFromRemainingArguments = $true)]
    [string[]]$Module,
    [switch]$Get,
    [switch]$Changed,
    [switch]$IncludeMetadata,
    [ValidateSet('Major', 'Minor', 'Patch')]
    [string]$Part = 'Minor',
    [string]$Version,
    [switch]$NoCommit,
    [string]$CoAuthor,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$modulesDir = Join-Path $root 'modules'
$versionPattern = '(?m)^(version[ \t]*=[ \t]*)(\S+)'
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Invoke-Git {
    $out = & git -C $root @args 2>&1
    if ($LASTEXITCODE -ne 0) { throw "git $($args -join ' ') failed: $out" }
    $out
}

function Get-ModuleState([System.IO.DirectoryInfo]$dir) {
    $props = Join-Path $dir.FullName 'gradle.properties'
    if (-not (Test-Path $props)) { return $null }
    $m = [regex]::Match([IO.File]::ReadAllText($props), $versionPattern)
    if (-not $m.Success) { return $null }

    $rel = "modules/$($dir.Name)"
    $bump = Invoke-Git log -1 --format=%h "-G^version\s*=" -- "$rel/gradle.properties"
    $range = if ($bump) { "$bump..HEAD" } else { 'HEAD' }
    $commits = @(Invoke-Git log --format=%h $range -- $rel | Where-Object { $_ })
    $code = @(Invoke-Git log --format=%h $range -- $rel ":(exclude)$rel/gradle.properties" | Where-Object { $_ })

    [pscustomobject]@{
        Name     = $dir.Name
        File     = $props
        Version  = $m.Groups[2].Value
        LastBump = $bump
        Commits  = $commits.Count
        Code     = $code.Count
    }
}

function Get-NextVersion([string]$current) {
    $nums = @($current.Split('.') | ForEach-Object { [int]$_ })
    while ($nums.Count -lt 3) { $nums += 0 }
    switch ($Part) {
        'Major' { $nums[0]++; $nums[1] = 0; $nums[2] = 0 }
        'Minor' { $nums[1]++; $nums[2] = 0 }
        'Patch' { $nums[2]++ }
    }
    ($nums[0..2] -join '.')
}

try {
    $all = Get-ChildItem $modulesDir -Directory | ForEach-Object { Get-ModuleState $_ } | Where-Object { $_ }
    $isPending = { param($s) $s.Code -gt 0 -or ($IncludeMetadata -and $s.Commits -gt 0) }

    if (-not $Module -and -not $Changed -and -not $Get) {
        $pending = @($all | Where-Object { $_.Commits -gt 0 })
        if (-not $pending) { Write-Host 'Every module is up to date with its last bump.'; exit 0 }
        foreach ($s in $pending) {
            $kind = if ($s.Code -gt 0) { "$($s.Code) code" } else { 'metadata only' }
            Write-Host ("{0,-24} {1,-9} {2} commit(s) since {3} ({4})" -f $s.Name, $s.Version, $s.Commits, $s.LastBump, $kind)
        }
        exit 0
    }

    $targets = @()
    $exact = @{}
    if ($Changed) { $targets += @($all | Where-Object { & $isPending $_ }) }
    foreach ($arg in $Module) {
        $name, $set = $arg -split '=', 2
        $s = $all | Where-Object { $_.Name -ieq $name.Trim() }
        if (-not $s) { throw "No module '$name' with a version in modules/<name>/gradle.properties." }
        if ($set) { $exact[$s.Name] = $set.Trim() }
        if ($targets.Name -notcontains $s.Name) { $targets += $s }
    }

    if ($Get) {
        $shown = @(if ($targets) { $targets } else { $all })
        if ($Module -and $shown.Count -eq 1) { Write-Output $shown[0].Version; exit 0 }
        foreach ($s in $shown) { Write-Output "$($s.Name)=$($s.Version)" }
        exit 0
    }

    if (-not $targets) { Write-Host 'Nothing to bump.'; exit 0 }
    if ($Version -and $targets.Count -ne 1) { throw '-Version needs exactly one module; use <Module>=<version> for several.' }
    if ($Version) { $exact[$targets[0].Name] = $Version }
    foreach ($v in $exact.Values) {
        if ($v -notmatch '^\d+(\.\d+)*([-+][0-9A-Za-z.-]+)?$') { throw "'$v' is not a version." }
    }

    if (-not $NoCommit -and -not $DryRun) {
        $staged = Invoke-Git diff --cached --name-only
        if ($staged) { throw "Staged changes would end up in the bump commits; commit or unstage them first:`n$staged" }
    }

    foreach ($s in $targets) {
        $new = if ($exact.ContainsKey($s.Name)) { $exact[$s.Name] } else { Get-NextVersion $s.Version }
        if ($new -eq $s.Version) { Write-Host "$($s.Name) is already $new"; continue }
        if ($DryRun) { Write-Host "$($s.Name) $($s.Version) -> $new (dry run)"; continue }

        $text = [IO.File]::ReadAllText($s.File)
        $v = [regex]::Match($text, $versionPattern).Groups[2]
        $text = $text.Substring(0, $v.Index) + $new + $text.Substring($v.Index + $v.Length)
        [IO.File]::WriteAllText($s.File, $text, $utf8)

        if (-not $NoCommit) {
            $msg = @('-m', "bump: $($s.Name.ToLowerInvariant()) version")
            if ($CoAuthor) { $msg += @('-m', "Co-Authored-By: $CoAuthor") }
            Invoke-Git add -- $s.File | Out-Null
            Invoke-Git commit -q @msg -- $s.File | Out-Null
        }
        Write-Host "$($s.Name) $($s.Version) -> $new"
    }
    exit 0
}
catch {
    [Console]::Error.WriteLine("module-utils: $($_.Exception.Message)")
    exit 1
}
