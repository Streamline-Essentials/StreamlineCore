#Requires -Version 5.1
<#
.SYNOPSIS
  Removes outdated module jars from the deploy folder, keeping the newest of each.

.DESCRIPTION
  Groups the jars in deploy\modules by module name (the file name without its
  trailing "-<version>", the archiveFileName that modules\module.gradle writes) and
  keeps only the jar with the highest version in each group. Versions compare part
  by part as numbers (1.10.0 is newer than 1.9.0; missing parts count as 0), a
  release is newer than a pre-release of the same version (1.2.0 over
  1.2.0-SNAPSHOT), and equal versions fall back to the newer file time.

  Removed jars go to the Recycle Bin, so they can be restored. Jars whose names do
  not end in "-<version>.jar" are left alone and listed.

  The folder defaults to deploy\modules beside this script, or modules under
  $env:DEPLOY_DIR when it is set, matching where Gradle's deploy task copies them.

.PARAMETER Path
  Folder to clean. Defaults to deploy\modules (or $env:DEPLOY_DIR\modules).

.PARAMETER DryRun
  Lists what would be kept and removed without deleting anything.

.PARAMETER Force
  Deletes without asking for confirmation.

.EXAMPLE
  .\clean-deploy-modules.ps1 -DryRun          # preview
.EXAMPLE
  .\clean-deploy-modules.ps1                  # asks before deleting
.EXAMPLE
  .\clean-deploy-modules.ps1 -Force           # no prompt
.EXAMPLE
  .\clean-deploy-modules.ps1 -Path D:\server\plugins\StreamlineCore\modules -DryRun
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string] $Path,
    [switch] $DryRun,
    [switch] $Force
)

$ErrorActionPreference = 'Stop'

if (-not $Path) {
    $deployRoot = if ($env:DEPLOY_DIR) { $env:DEPLOY_DIR } else { Join-Path $PSScriptRoot 'deploy' }
    $Path = Join-Path $deployRoot 'modules'
}
if (-not (Test-Path -LiteralPath $Path -PathType Container)) {
    Write-Error "Folder not found: $Path"
    exit 1
}
$Path = (Resolve-Path -LiteralPath $Path).Path

# <name>-<version>.jar; a suffix must start with a letter, so a numeric segment in the
# name (StreamlineCore-Fabric-1201-2.6.1.8) stays part of the name.
$pattern = '^(?<name>.+?)-(?<ver>\d+(?:\.\d+)*)(?:[-+](?<pre>[A-Za-z][\w.\-]*))?\.jar$'

function Compare-JarVersion($a, $b) {
    $pa = $a.Parts; $pb = $b.Parts
    $n = [Math]::Max($pa.Count, $pb.Count)
    for ($i = 0; $i -lt $n; $i++) {
        $x = if ($i -lt $pa.Count) { $pa[$i] } else { [long]0 }
        $y = if ($i -lt $pb.Count) { $pb[$i] } else { [long]0 }
        if ($x -ne $y) { return [Math]::Sign($x - $y) }
    }
    # A release outranks a pre-release of the same numbers.
    if (-not $a.Pre -and $b.Pre) { return 1 }
    if ($a.Pre -and -not $b.Pre) { return -1 }
    if ($a.Pre -ne $b.Pre) { return [Math]::Sign([string]::CompareOrdinal($a.Pre, $b.Pre)) }
    return [Math]::Sign($a.File.LastWriteTimeUtc.CompareTo($b.File.LastWriteTimeUtc))
}

$jars = @()
$unparsed = @()
foreach ($file in Get-ChildItem -LiteralPath $Path -Filter '*.jar' -File) {
    $m = [regex]::Match($file.Name, $pattern)
    if (-not $m.Success) { $unparsed += $file; continue }
    $jars += [pscustomobject]@{
        File    = $file
        Name    = $m.Groups['name'].Value
        Version = $file.BaseName.Substring($m.Groups['name'].Value.Length + 1)
        Parts   = @($m.Groups['ver'].Value.Split('.') | ForEach-Object { [long]$_ })
        Pre     = $m.Groups['pre'].Value
    }
}

$remove = @()
foreach ($group in $jars | Group-Object Name | Sort-Object Name) {
    $newest = $group.Group[0]
    foreach ($jar in $group.Group | Select-Object -Skip 1) {
        if ((Compare-JarVersion $jar $newest) -gt 0) { $newest = $jar }
    }
    $old = @($group.Group | Where-Object { $_ -ne $newest })
    if ($old.Count -eq 0) { continue }
    $oldList = ($old | Sort-Object { $_.File.Name } | ForEach-Object { $_.Version }) -join ', '
    Write-Host ("{0}: keep {1}, remove {2}" -f $group.Name, $newest.Version, $oldList)
    $remove += $old
}

foreach ($file in $unparsed) {
    Write-Host "Skipped (no version in name): $($file.Name)" -ForegroundColor Yellow
}

if ($remove.Count -eq 0) {
    Write-Host "Nothing to remove in $Path; every module has one jar."
    exit 0
}

if ($DryRun) {
    Write-Host "Dry run: would move $($remove.Count) jar(s) from $Path to the Recycle Bin."
    exit 0
}

if (-not $Force) {
    $answer = Read-Host "Move $($remove.Count) old jar(s) to the Recycle Bin? [y/N]"
    if ($answer -notmatch '^(y|yes)$') {
        Write-Host 'Cancelled.'
        exit 0
    }
}

Add-Type -AssemblyName Microsoft.VisualBasic
$failed = 0
foreach ($jar in $remove) {
    try {
        [Microsoft.VisualBasic.FileIO.FileSystem]::DeleteFile(
            $jar.File.FullName, 'OnlyErrorDialogs', 'SendToRecycleBin')
    } catch {
        Write-Warning "Could not remove $($jar.File.Name): $($_.Exception.Message)"
        $failed++
    }
}

Write-Host "Moved $($remove.Count - $failed) jar(s) to the Recycle Bin."
if ($failed -gt 0) { exit 1 }
exit 0
