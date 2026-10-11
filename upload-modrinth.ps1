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
    Minecraft version line each (1201 = 1.20.1, 1211 = 1.21.1, ...), plus
    StreamlineCore-Forge-189-<v>.jar, the client-only Forge 1.8.9 build

  The plugin jars list every Modrinth release game version from 1.8 (Spigot)
  or 1.7.2 (proxies) up to the newest one. A mod jar lists the release game
  versions its own metadata accepts (depends.minecraft in fabric.mod.json, the
  minecraft versionRange in mods.toml / neoforge.mods.toml, mcversion in
  mcmod.info), and its environment: client-only for Forge 1.8.9, client or
  server for the rest, which load on either side without the other.

  Every version is named "StreamlineCore <v> (<platform>)" and keeps the plain
  version number: BukkitOfUtils' update check (api.drak.gg) compares the
  installed version against the project's latest version number regardless of
  loader, and ranks "2.6.0.0+fabric-1.21.1" above "2.6.0.0", so a per-loader
  suffix would tell every up-to-date Spigot server to update.

  A jar whose file name is already on one of the project's versions is
  skipped, so re-running after a partial upload only sends what is missing.
  -WhatIf prints the metadata each jar would be published with.

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

# Mod jars that run only on a game client (forge\forge-189 has no server side).
$ClientOnlyMods = @('Forge-189')

function Invoke-Modrinth([string] $Method, [string] $Path) {
    Invoke-RestMethod -Method $Method -Uri "$Api$Path" -UserAgent $UserAgent
}

# Every Modrinth release game version, oldest first.
$releaseVersions = $null
function Get-ReleaseVersions {
    if (-not $script:releaseVersions) {
        # The tag list is newest first. Invoke-RestMethod emits a JSON array as one pipeline object until
        # parenthesized, and [string[]] unwraps the PSObjects that IndexOf would never match.
        [string[]] $all = @((Invoke-Modrinth Get '/tag/game_version') | Where-Object version_type -eq 'release' | ForEach-Object { $_.version })
        [array]::Reverse($all)
        $script:releaseVersions = $all
    }
    $script:releaseVersions
}

# Every Modrinth release game version from $Floor up, oldest first.
function Get-GameVersionsFrom([string] $Floor) {
    $all = Get-ReleaseVersions
    $start = [array]::IndexOf($all, $Floor)
    if ($start -lt 0) { throw "Modrinth has no release game version '$Floor'." }
    $all[$start..($all.Count - 1)]
}

# Numeric comparison of dotted game versions ("1.21.11" > "1.21.2", "26.2" > "1.21.11"); missing parts count as 0.
function Compare-GameVersion([string] $A, [string] $B) {
    $x = @($A.Split('.') | ForEach-Object { [int] $_ })
    $y = @($B.Split('.') | ForEach-Object { [int] $_ })
    for ($i = 0; $i -lt [Math]::Max($x.Count, $y.Count); $i++) {
        $p = if ($i -lt $x.Count) { $x[$i] } else { 0 }
        $q = if ($i -lt $y.Count) { $y[$i] } else { 0 }
        if ($p -ne $q) { return [Math]::Sign($p - $q) }
    }
    0
}

# The release game versions a version range accepts, oldest first. Takes Fabric's space-separated
# comparators (">=1.21.1 <1.21.2", "*") and Maven ranges ("[1.21.1,1.21.2)", "[1.8.9]"); a bare
# version ("1.8.9", Forge's soft requirement) means exactly that version.
function Get-GameVersionsIn([string] $Range) {
    $Range = $Range.Trim()
    $tests = @()
    if ($Range -eq '*' -or $Range -eq '') {
        $tests = @()
    } elseif ($Range -match '^([\[\(])\s*([^,\]\)]*?)\s*(,\s*([^\]\)]*?)\s*)?([\]\)])$') {
        $open = $Matches[1]; $low = $Matches[2]; $hasComma = [bool] $Matches[3]; $high = $Matches[4]; $close = $Matches[5]
        if (-not $hasComma) {
            $tests += @{ Op = '='; Version = $low }
        } else {
            if ($low) { $tests += @{ Op = $(if ($open -eq '[') { '>=' } else { '>' }); Version = $low } }
            if ($high) { $tests += @{ Op = $(if ($close -eq ']') { '<=' } else { '<' }); Version = $high } }
        }
    } else {
        foreach ($token in ($Range -split '\s+')) {
            if ($token -notmatch '^(>=|<=|>|<|=)?(\d+(\.\d+)*)$') { throw "Unsupported game version range '$Range'." }
            $op = if ($Matches[1]) { $Matches[1] } else { '=' }
            $tests += @{ Op = $op; Version = $Matches[2] }
        }
    }
    @(Get-ReleaseVersions | Where-Object {
        $game = $_
        foreach ($t in $tests) {
            $c = Compare-GameVersion $game $t.Version
            $ok = switch ($t.Op) { '>=' { $c -ge 0 } '>' { $c -gt 0 } '<=' { $c -le 0 } '<' { $c -lt 0 } default { $c -eq 0 } }
            if (-not $ok) { return $false }
        }
        $true
    })
}

# The Minecraft version range a mod jar declares in its own loader metadata.
function Get-ModGameRange([IO.FileInfo] $File) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($File.FullName)
    try {
        $read = {
            param($name)
            $entry = $zip.GetEntry($name)
            if (-not $entry) { return $null }
            $reader = New-Object IO.StreamReader($entry.Open())
            try { $reader.ReadToEnd() } finally { $reader.Dispose() }
        }
        $fabric = & $read 'fabric.mod.json'
        if ($fabric) {
            $range = ($fabric | ConvertFrom-Json).depends.minecraft
            if ($range) { return [string] $range }
        }
        foreach ($toml in @('META-INF/neoforge.mods.toml', 'META-INF/mods.toml')) {
            $text = & $read $toml
            if (-not $text) { continue }
            # The [[dependencies.<mod>]] table whose modId is "minecraft", up to the next table header.
            $m = [regex]::Match($text, '(?ms)^\s*\[\[dependencies\.[^\]]+\]\](?:(?!^\s*\[).)*?modId\s*=\s*"minecraft"(?:(?!^\s*\[).)*?versionRange\s*=\s*"([^"]*)"')
            if ($m.Success) { return $m.Groups[1].Value }
        }
        $mcmod = & $read 'mcmod.info'
        if ($mcmod) {
            $range = @($mcmod | ConvertFrom-Json)[0].mcversion
            if ($range) { return [string] $range }
        }
        $null
    } finally {
        $zip.Dispose()
    }
}

function New-Dependency([string] $Id, [string] $Type) {
    [ordered]@{ project_id = $Id; dependency_type = $Type }
}

# Modrinth metadata for one platform jar, keyed by its name without the "StreamlineCore-" prefix and
# version suffix. Environment is set only for mod loaders; Modrinth has no such field for plugin loaders.
function Get-Target([string] $Key, [IO.FileInfo] $File) {
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
            return @{ Label = 'BungeeCord'; Loaders = @('bungeecord', 'waterfall'); Games = @(Get-GameVersionsFrom '1.7.2'); Deps = $proxyDeps }
        }
        '^Velocity$' {
            return @{ Label = 'Velocity'; Loaders = @('velocity'); Games = @(Get-GameVersionsFrom '1.7.2'); Deps = $proxyDeps }
        }
        '^(Fabric|Forge|NeoForge)-\d+$' {
            $loader = $Matches[1]
            $range = Get-ModGameRange $File
            if (-not $range) { throw "$($File.Name) declares no Minecraft version in its mod metadata." }
            $games = @(Get-GameVersionsIn $range)
            if (-not $games.Count) { throw "$($File.Name): no Modrinth release game version matches '$range'." }

            $clientOnly = $ClientOnlyMods -contains $Key
            # LuckPerms has no client build, so a client-only jar lists no dependencies.
            $modDeps = @()
            if ($loader -eq 'Fabric') { $modDeps += ,(New-Dependency $Deps.FabricApi 'required') }
            if (-not $clientOnly) { $modDeps += ,(New-Dependency $Deps.LuckPerms 'optional') }

            $gameLabel = if ($games.Count -eq 1) { $games[0] } else { "$($games[0])-$($games[-1])" }
            return @{
                Label       = "$loader $gameLabel$(if ($clientOnly) { ' client' })"
                Loaders     = @($loader.ToLower())
                Games       = $games
                Deps        = $modDeps
                Environment = $(if ($clientOnly) { 'client_only' } else { 'client_or_server' })
            }
        }
    }
    $null
}

if (-not (Test-Path $DeployDir -PathType Container)) { throw "No deploy folder at $DeployDir; build first." }

# API, BAPI and Singularity are libraries for developers, not server jars.
$suffix = "-$([regex]::Escape($Version))\.jar$"
$uploads = foreach ($file in Get-ChildItem $DeployDir -File -Filter "StreamlineCore-*-$Version.jar") {
    $key = $file.Name -replace '^StreamlineCore-', '' -replace $suffix, ''
    if (@('API', 'BAPI', 'Singularity', 'ModuleCore') -contains $key) { continue }
    if ($Platform -and -not ($Platform | Where-Object { $key -eq $_ -or $key -like "$_-*" })) { continue }
    $target = Get-Target $key $file
    if ($target) { $target.File = $file; $target }
    else { Write-Warning "$($file.Name): not a known platform jar, skipped." }
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
        $name = "StreamlineCore $Version ($($u.Label))"
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
        if ($u.Environment) { $data.environment = $u.Environment }

        if ($WhatIfPreference) {
            # Everything but the changelog, which is the same for every jar.
            $preview = [ordered]@{}
            foreach ($k in $data.Keys) { if ($k -ne 'changelog') { $preview[$k] = $data[$k] } }
            if ($u.Games.Count -gt 2) { $preview.game_versions = "$($u.Games[0]) .. $($u.Games[-1]) ($($u.Games.Count) versions)" }
            Write-Host "$($file.Name)$(if ($published -contains $file.Name) { ' (already on Modrinth)' })" -ForegroundColor Cyan
            Write-Host ($preview | ConvertTo-Json -Depth 5)
        }

        if ($published -contains $file.Name) {
            Write-Host "skipped $($file.Name): already on Modrinth" -ForegroundColor DarkGray
            continue
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
