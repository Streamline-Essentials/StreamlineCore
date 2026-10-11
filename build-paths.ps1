#Requires -Version 5.1
<#
.SYNOPSIS
  Where Gradle puts this checkout's build output. Dot-source it from other scripts.

.DESCRIPTION
  Mirrors the build-output redirect in build.gradle. A checkout inside OneDrive builds
  under %LOCALAPPDATA%\StreamlineCore\build\<folder name>-<hex of Java's String.hashCode
  of the lowercased checkout path>\<project path>, because OneDrive turns synced build
  folders into read-only placeholders Gradle cannot delete. STREAMLINE_BUILD_ROOT picks
  another root; "project" keeps the usual build\ folder in each project.
#>

# The redirected build root for a checkout, or $null when each project keeps build\ in place.
function Get-RedirectedBuildRoot {
    param([Parameter(Mandatory)][string] $Root)
    $custom = $env:STREAMLINE_BUILD_ROOT
    if (-not [string]::IsNullOrWhiteSpace($custom)) {
        if ($custom.Trim() -eq 'project') { return $null }
        return $custom.Trim()
    }
    if ([string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) { return $null }
    $lower = $Root.ToLowerInvariant()
    $inOneDrive = $false
    foreach ($od in @($env:OneDrive, $env:OneDriveConsumer, $env:OneDriveCommercial)) {
        if (-not [string]::IsNullOrWhiteSpace($od) -and $lower.StartsWith($od.ToLowerInvariant().TrimEnd('\') + '\')) { $inOneDrive = $true }
    }
    if (-not $inOneDrive) { return $null }
    # Java's String.hashCode, kept to 32 bits; the literal 0xFFFFFFFF would be -1 here.
    [int64] $mask = 4294967295
    [int64] $h = 0
    foreach ($c in $lower.ToCharArray()) { $h = ($h * 31 + [int][char]$c) -band $mask }
    $hex = '{0:x}' -f $h
    return Join-Path $env:LOCALAPPDATA ('StreamlineCore\build\' + (Split-Path -Leaf $Root) + '-' + $hex)
}

# The build directory of one project, given the checkout root and the project's folder.
function Get-ProjectBuildDir {
    param(
        [Parameter(Mandatory)][string] $Root,
        [Parameter(Mandatory)][string] $ProjectDir
    )
    $redirected = Get-RedirectedBuildRoot -Root $Root
    if (-not $redirected) { return Join-Path $ProjectDir 'build' }
    $rootFull = [IO.Path]::GetFullPath($Root).TrimEnd('\')
    $projectFull = [IO.Path]::GetFullPath($ProjectDir).TrimEnd('\')
    if ($projectFull -eq $rootFull) { return Join-Path $redirected '_root' }
    return Join-Path $redirected $projectFull.Substring($rootFull.Length + 1)
}
