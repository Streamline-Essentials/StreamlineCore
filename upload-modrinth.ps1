#Requires -Version 5.1
<#
.SYNOPSIS
  Uploads the StreamlineCore platform jars in deploy\ to Modrinth.

.DESCRIPTION
  Publishes one Modrinth version per platform jar to the StreamlineCore project
  (wEJy5rtv), authenticated with the personal access token in
  $env:MODRINTH_TOKEN (needs the VERSION_CREATE scope):

  - StreamlineCore-Spigot-<v>.jar     bukkit, folia, paper, purpur, spigot
  - StreamlineCore-Bungee-<v>.jar     bungeecord, waterfall
  - StreamlineCore-Velocity-<v>.jar   velocity
  - StreamlineCore-<Loader>-<mc>-<v>.jar   fabric / forge / neoforge, one
    Minecraft version each (1201 = 1.20.1, 1211 = 1.21.1, 12111 = 1.21.11,
    262 = 26.2, 263 = 26.3)

  The plugin jars list every Modrinth release game version from 1.8 (Spigot)
  or 1.7.2 (proxies) up to the newest one. A jar whose file name is already on
  one of the project's versions is skipped, so re-running after a partial
  upload only sends what is missing.

  API reference: https://docs.modrinth.com/api/operations/createversion/

.PARAMETER Platform
  Which jars to upload, matched against the part of the name after
  "StreamlineCore-" (e.g. Spigot, Velocity, Fabric, NeoForge-1211).
  Defaults to every platform jar in -DeployDir.

.PARAMETER Version
  Version to upload. Defaults to "version" in gradle.properties.

.PARAMETER Changelog
  Changelog text. Defaults to the contents of UPDATES.md.

.PARAMETER VersionType
  release, beta or alpha. Defaults to release.

.PARAMETER DeployDir
  Folder holding the built jars. Defaults to $env:DEPLOY_DIR, else .\deploy.

.EXAMPLE
  .\upload-modrinth.ps1                          # every platform jar
.EXAMPLE
  .\upload-modrinth.ps1 Spigot Bungee Velocity   # plugin jars only
.EXAMPLE
  .\upload-modrinth.ps1 Fabric -VersionType beta
.EXAMPLE
  .\upload-modrinth.ps1 -WhatIf                  # list what would be uploaded
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Position = 0, ValueFromRemainingArguments)]
    [string[]] $Platform,
    [string] $Version,
    [string] $Changelog,
    [ValidateSet('release', 'beta', 'alpha')]
    [string] $VersionType = 'release',
    [string] $DeployDir
)

$ErrorActionPreference = 'Stop'

# Also returned by BasePlugin#getModrinthId() on Spigot for BOU's version checker.
$ProjectId = 'wEJy5rtv'
$Api = 'https://api.modrinth.com/v2'
# Modrinth rejects requests without an identifying User-Agent.
$UserAgent = 'DrakDv/StreamlineCore-upload (github.com/Streamline-Essentials)'

# Windows PowerShell 5.1 offers only TLS 1.0/1.1 by default; Modrinth requires 1.2+.
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
Add-Type -AssemblyName System.Net.Http

$token = $env:MODRINTH_TOKEN
if ([string]::IsNullOrWhiteSpace($token) -and -not $WhatIfPreference) {
    throw 'Set $env:MODRINTH_TOKEN to a Modrinth personal access token with the VERSION_CREATE scope.'
}

if (-not $Version) {
    $line = Get-Content (Join-Path $PSScriptRoot 'gradle.properties') | Where-Object { $_ -match '^\s*version\s*=' } | Select-Object -First 1
    if (-not $line) { throw 'No version in gradle.properties; pass -Version.' }
    $Version = ($line -split '=', 2)[1].Trim()
}

if (-not $PSBoundParameters.ContainsKey('Changelog')) {
    $updates = Join-Path $PSScriptRoot 'UPDATES.md'
    $Changelog = if (Test-Path $updates) { (Get-Content $updates -Raw).Trim() } else { '' }
}

if (-not $DeployDir) {
    $DeployDir = if ($env:DEPLOY_DIR) { $env:DEPLOY_DIR } else { Join-Path $PSScriptRoot 'deploy' }
}

# Project ids of the dependencies listed on each version.
$Deps = @{
    BukkitOfUtils = '6owv5fWs'
    LuckPerms     = 'Vebnzrzj'
    EssentialsX   = 'hXiIvTyT'
    Geyser        = 'wKkoqHrH'
    Floodgate     = 'bWrNNfkb'
    TAB           = 'gG7VFbG0'
    FabricApi     = 'P7dR8mSH'
}

# Mod jar suffix -> the single Minecraft version it is built for (see each <loader>-<mc>\build.gradle).
$ModGameVersions = [ordered]@{
    '1201'  = '1.20.1'
    '1211'  = '1.21.1'
    '12111' = '1.21.11'
    '262'   = '26.2'
    '263'   = '26.3'
}

function Invoke-Modrinth([string] $Method, [string] $Path) {
    Invoke-RestMethod -Method $Method -Uri "$Api$Path" -UserAgent $UserAgent
}

# Every Modrinth release game version from $Floor up, oldest first.
$releaseVersions = $null
function Get-GameVersionsFrom([string] $Floor) {
    if (-not $script:releaseVersions) {
        # The tag list is newest first. Invoke-RestMethod emits a JSON array as one pipeline object until
        # parenthesized, and [string[]] unwraps the PSObjects that IndexOf would never match.
        [string[]] $all = @((Invoke-Modrinth Get '/tag/game_version') | Where-Object version_type -eq 'release' | ForEach-Object { $_.version })
        [array]::Reverse($all)
        $script:releaseVersions = $all
    }
    $start = [array]::IndexOf($script:releaseVersions, $Floor)
    if ($start -lt 0) { throw "Modrinth has no release game version '$Floor'." }
    $script:releaseVersions[$start..($script:releaseVersions.Count - 1)]
}

function New-Dependency([string] $Id, [string] $Type) {
    [ordered]@{ project_id = $Id; dependency_type = $Type }
}

# Modrinth metadata for one platform jar name, without the "StreamlineCore-" prefix and version suffix.
function Get-Target([string] $Key) {
    $proxyDeps = @(
        (New-Dependency $Deps.LuckPerms 'optional'),
        (New-Dependency $Deps.Geyser 'optional'),
        (New-Dependency $Deps.Floodgate 'optional'),
        (New-Dependency $Deps.TAB 'optional')
    )
    switch -Regex ($Key) {
        '^Spigot$' {
            return @{
                Label   = 'Spigot'
                Loaders = @('bukkit', 'folia', 'paper', 'purpur', 'spigot')
                Games   = @(Get-GameVersionsFrom '1.8')
                Deps    = @(
                    (New-Dependency $Deps.BukkitOfUtils 'required'),
                    (New-Dependency $Deps.LuckPerms 'optional'),
                    (New-Dependency $Deps.EssentialsX 'optional'),
                    (New-Dependency $Deps.Geyser 'optional'),
                    (New-Dependency $Deps.Floodgate 'optional'),
                    (New-Dependency $Deps.TAB 'optional')
                )
            }
        }
        '^Bungee$' {
            return @{ Label = 'Bungee'; Loaders = @('bungeecord', 'waterfall'); Games = @(Get-GameVersionsFrom '1.7.2'); Deps = $proxyDeps }
        }
        '^Velocity$' {
            return @{ Label = 'Velocity'; Loaders = @('velocity'); Games = @(Get-GameVersionsFrom '1.7.2'); Deps = $proxyDeps }
        }
        '^(Fabric|Forge|NeoForge)-(\d+)$' {
            $loader = $Matches[1]
            $game = $ModGameVersions[$Matches[2]]
            if (-not $game) { return $null }
            $modDeps = @(New-Dependency $Deps.LuckPerms 'optional')
            if ($loader -eq 'Fabric') { $modDeps = @(New-Dependency $Deps.FabricApi 'required') + $modDeps }
            return @{ Label = "$loader $game"; Loaders = @($loader.ToLower()); Games = @($game); Deps = $modDeps }
        }
    }
    $null
}

if (-not (Test-Path $DeployDir -PathType Container)) { throw "No deploy folder at $DeployDir; build first." }

# API, BAPI and Singularity are libraries for developers, not server jars.
$suffix = "-$([regex]::Escape($Version))\.jar$"
$uploads = foreach ($file in Get-ChildItem $DeployDir -File -Filter "StreamlineCore-*-$Version.jar") {
    $key = $file.Name -replace '^StreamlineCore-', '' -replace $suffix, ''
    if ($Platform -and -not ($Platform | Where-Object { $key -eq $_ -or $key -like "$_-*" })) { continue }
    $target = Get-Target $key
    if ($target) { $target.File = $file; $target }
}
$uploads = @($uploads | Sort-Object { $_.Label })

if (-not $uploads) { throw "No platform jars for $Version in $DeployDir$(if ($Platform) { " matching $($Platform -join ', ')" })." }

$published = @((Invoke-Modrinth Get "/project/$ProjectId/version") | ForEach-Object { $_.files } | ForEach-Object { $_.filename })

$client = New-Object System.Net.Http.HttpClient
$client.DefaultRequestHeaders.TryAddWithoutValidation('Authorization', [string] $token) | Out-Null
$client.DefaultRequestHeaders.TryAddWithoutValidation('User-Agent', $UserAgent) | Out-Null

$failed = 0
try {
    foreach ($u in $uploads) {
        $file = $u.File
        if ($published -contains $file.Name) {
            Write-Host "skipped $($file.Name): already on Modrinth" -ForegroundColor DarkGray
            continue
        }

        # Plugin jars keep the plain name earlier releases used; mod jars name their loader and game version.
        $name = if ($u.Loaders.Count -eq 1 -and $u.Games.Count -eq 1) { "StreamlineCore $Version ($($u.Label))" } else { "StreamlineCore $Version" }
        $data = [ordered]@{
            project_id     = $ProjectId
            name           = $name
            version_number = $Version
            changelog      = $Changelog
            version_type   = $VersionType
            status         = 'listed'
            featured       = $false
            loaders        = @($u.Loaders)
            game_versions  = @($u.Games)
            dependencies   = @($u.Deps)
            file_parts     = @('file')
            primary_file   = 'file'
        }

        $games = if ($u.Games.Count -gt 2) { "$($u.Games[0])-$($u.Games[-1])" } else { $u.Games -join ', ' }
        if (-not $PSCmdlet.ShouldProcess("$name [$($u.Loaders -join ', ')] [$games]", "POST $($file.Name)")) { continue }

        $form = New-Object System.Net.Http.MultipartFormDataContent
        $stream = [IO.File]::OpenRead($file.FullName)
        try {
            $json = New-Object System.Net.Http.StringContent(($data | ConvertTo-Json -Depth 5 -Compress), [Text.Encoding]::UTF8, 'application/json')
            $form.Add($json, 'data')
            $content = New-Object System.Net.Http.StreamContent($stream)
            $content.Headers.ContentType = [Net.Http.Headers.MediaTypeHeaderValue]::Parse('application/java-archive')
            $form.Add($content, 'file', $file.Name)

            $response = $client.PostAsync("$Api/version", $form).GetAwaiter().GetResult()
            $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            if ($response.IsSuccessStatusCode) {
                $result = $body | ConvertFrom-Json
                Write-Host ("uploaded {0} ({1}) <- {2}" -f $name, $result.id, $file.Name) -ForegroundColor Green
            } else {
                $failed++
                $detail = $body
                try { $err = $body | ConvertFrom-Json; $detail = "$($err.error): $($err.description)" } catch { }
                Write-Host "failed  $($file.Name): $([int]$response.StatusCode) $detail" -ForegroundColor Red
            }
        } catch {
            $failed++
            Write-Host "failed  $($file.Name): $($_.Exception.Message)" -ForegroundColor Red
        } finally {
            $stream.Dispose()
            $form.Dispose()
        }
    }
} finally {
    $client.Dispose()
}

if ($failed) { exit 1 }
