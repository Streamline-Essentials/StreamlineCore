#Requires -Version 5.1
<#
.SYNOPSIS
  Uploads the StreamlineCore mod jars in deploy\ to CurseForge.

.DESCRIPTION
  Publishes one CurseForge file per mod jar to the StreamlineCore project
  (1780822), authenticated with the upload API token in
  $env:CURSEFORGE_TOKEN (https://legacy.curseforge.com/account/api-tokens):

  - StreamlineCore-<Loader>-<mc>-<v>.jar   fabric / forge / neoforge, one
    Minecraft version line each (1201 = 1.20.1, 1211 = 1.21.1, ...), plus
    StreamlineCore-Forge-189-<v>.jar, the client-only Forge 1.8.9 build

  The project is a CurseForge mod project, whose only loaders are mod loaders,
  so the Spigot, BungeeCord and Velocity jars are never uploaded here (use
  upload-modrinth.ps1 for those).

  A jar lists the Minecraft release versions its own metadata accepts
  (depends.minecraft in fabric.mod.json, the minecraft versionRange in
  mods.toml / neoforge.mods.toml, mcversion in mcmod.info), its loader, and
  its environment: Client for Forge 1.8.9, Client and Server for the rest.
  Fabric jars require Fabric API; server-capable jars list LuckPerms as
  optional.

  Every file is named "StreamlineCore <v> (<platform>)". The changelog is
  sent as markdown.

  A jar whose file name is already among the project's public files gets
  that file's metadata updated in place (update-file: name, changelog,
  release type, game versions, dependencies) instead of a second upload.
  CurseForge cannot replace a file's jar, so a rebuilt jar under the same
  name is not re-sent; delete the file on CurseForge first to re-upload it.
  Files still awaiting moderation may be missing from the public listing,
  so re-running right after an upload can upload them twice.

  -WhatIf prints the metadata each jar would be published with, and whether
  it would be uploaded or updated.

  API reference:
  https://support.curseforge.com/en/support/solutions/articles/9000197321-curseforge-upload-api

.PARAMETER Platform
  Which jars to upload, matched against the part of the name after
  "StreamlineCore-" (e.g. Fabric, NeoForge-1211, Forge-189).
  Defaults to every mod jar in -DeployDir.

.PARAMETER Version
  Version to upload. Defaults to "version" in gradle.properties.

.PARAMETER Changelog
  Changelog text (markdown). Defaults to the contents of UPDATES.md.

.PARAMETER ReleaseType
  release, beta or alpha. Defaults to release.

.PARAMETER DeployDir
  Folder holding the built jars. Defaults to $env:DEPLOY_DIR, else .\deploy.

.PARAMETER ThrottleLimit
  How many jars are sent at once. Defaults to 10.

.EXAMPLE
  .\upload-curseforge.ps1                        # every mod jar
.EXAMPLE
  .\upload-curseforge.ps1 Fabric NeoForge-1211   # some jars only
.EXAMPLE
  .\upload-curseforge.ps1 Forge -ReleaseType beta
.EXAMPLE
  .\upload-curseforge.ps1 -WhatIf                # list what would be uploaded
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Position = 0, ValueFromRemainingArguments)]
    [string[]] $Platform,
    [string] $Version,
    [string] $Changelog,
    [ValidateSet('release', 'beta', 'alpha')]
    [string] $ReleaseType = 'release',
    [string] $DeployDir,
    [ValidateRange(1, 64)]
    [int] $ThrottleLimit = 10
)

$ErrorActionPreference = 'Stop'
# Invoke-RestMethod otherwise redraws a progress bar for every request.
$ProgressPreference = 'SilentlyContinue'
. (Join-Path $PSScriptRoot 'upload-parallel.ps1')

$ProjectId = 1780822
$Api = 'https://minecraft.curseforge.com/api'
# The site's own JSON API, which lists a project's public files without a key.
$FilesApi = "https://www.curseforge.com/api/v1/mods/$ProjectId/files"
$UserAgent = 'DrakDv/StreamlineCore-upload (github.com/Streamline-Essentials)'

# Windows PowerShell 5.1 offers only TLS 1.0/1.1 by default; CurseForge requires 1.2+.
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
Add-Type -AssemblyName System.Net.Http

$token = $env:CURSEFORGE_TOKEN
if ([string]::IsNullOrWhiteSpace($token)) {
    # -WhatIf needs it too: game version ids come from the authenticated API.
    throw 'Set $env:CURSEFORGE_TOKEN to a CurseForge upload API token (https://legacy.curseforge.com/account/api-tokens).'
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

# CurseForge slugs of the projects listed as relations.
$Deps = @{
    FabricApi = 'fabric-api'
    LuckPerms = 'luckperms'
}

# Mod jars that run only on a game client (forge\forge-189 has no server side).
$ClientOnlyMods = @('Forge-189')

$headers = @{ 'X-Api-Token' = [string] $token }
# Invoke-RestMethod emits a JSON array as one pipeline object; piping it on enumerates the elements.
$versionTypes = @((Invoke-RestMethod -Uri "$Api/game/version-types" -Headers $headers -UserAgent $UserAgent) | ForEach-Object { $_ })
$gameVersions = @((Invoke-RestMethod -Uri "$Api/game/versions" -Headers $headers -UserAgent $UserAgent) | ForEach-Object { $_ })

function Get-GameVersionId([string] $TypeSlug, [string] $Name) {
    $type = $versionTypes | Where-Object slug -eq $TypeSlug | Select-Object -First 1
    if (-not $type) { throw "CurseForge has no game version type '$TypeSlug'." }
    $match = $gameVersions | Where-Object { $_.gameVersionTypeID -eq $type.id -and $_.name -eq $Name } | Select-Object -First 1
    if (-not $match) { throw "CurseForge has no $TypeSlug version '$Name'." }
    [int] $match.id
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

# Every Minecraft release version CurseForge offers, oldest first, as @{ Name; Id }. Release
# versions live in the "minecraft-<line>" types; the same names also exist under unrelated types
# (Bukkit, Addons), so ids are taken from the Minecraft types only.
$mcTypes = @($versionTypes | Where-Object { $_.slug -like 'minecraft-*' -and $_.slug -notmatch 'snapshot|beta' } | ForEach-Object { $_.id })
$releaseVersions = @(
    $gameVersions |
        Where-Object { $mcTypes -contains $_.gameVersionTypeID -and $_.name -match '^\d+(\.\d+)+$' } |
        ForEach-Object { @{ Name = [string] $_.name; Id = [int] $_.id } } |
        Sort-Object { ($_.Name.Split('.') | ForEach-Object { '{0:D6}' -f [int] $_ }) -join '.' }
)

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
    @($releaseVersions | Where-Object {
        $game = $_.Name
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

function New-Relation([string] $Slug, [string] $Type) {
    [ordered]@{ slug = $Slug; type = $Type }
}

# CurseForge metadata for one mod jar, keyed by its name without the "StreamlineCore-" prefix and
# version suffix; $null for jars that are not mods.
function Get-Target([string] $Key, [IO.FileInfo] $File) {
    if ($Key -notmatch '^(Fabric|Forge|NeoForge)-\d+$') { return $null }
    $loader = $Matches[1]
    $range = Get-ModGameRange $File
    if (-not $range) { throw "$($File.Name) declares no Minecraft version in its mod metadata." }
    $games = @(Get-GameVersionsIn $range)
    if (-not $games.Count) { throw "$($File.Name): no CurseForge release game version matches '$range'." }

    $clientOnly = $ClientOnlyMods -contains $Key
    # LuckPerms has no client build, so a client-only jar lists no dependencies.
    $relations = @()
    if ($loader -eq 'Fabric') { $relations += ,(New-Relation $Deps.FabricApi 'requiredDependency') }
    if (-not $clientOnly) { $relations += ,(New-Relation $Deps.LuckPerms 'optionalDependency') }

    $environments = if ($clientOnly) { @('Client') } else { @('Client', 'Server') }
    $ids = @($games | ForEach-Object { $_.Id })
    $ids += Get-GameVersionId 'modloader' $loader
    foreach ($e in $environments) { $ids += Get-GameVersionId 'environment' $e }

    $gameLabel = if ($games.Count -eq 1) { $games[0].Name } else { "$($games[0].Name)-$($games[-1].Name)" }
    @{
        Label        = "$loader $gameLabel$(if ($clientOnly) { ' client' })"
        Games        = @($games | ForEach-Object { $_.Name })
        Loader       = $loader
        Environments = $environments
        VersionIds   = $ids
        Relations    = $relations
    }
}

if (-not (Test-Path $DeployDir -PathType Container)) { throw "No deploy folder at $DeployDir; build first." }

$suffix = "-$([regex]::Escape($Version))\.jar$"
$uploads = foreach ($file in Get-ChildItem $DeployDir -File -Filter "StreamlineCore-*-$Version.jar") {
    $key = $file.Name -replace '^StreamlineCore-', '' -replace $suffix, ''
    if ($Platform -and -not ($Platform | Where-Object { $key -eq $_ -or $key -like "$_-*" })) { continue }
    $target = Get-Target $key $file
    if ($target) { $target.File = $file; $target }
}
$uploads = @($uploads | Sort-Object { $_.Label })

if (-not $uploads) { throw "No mod jars for $Version in $DeployDir$(if ($Platform) { " matching $($Platform -join ', ')" })." }

# The project's public files by file name.
$published = @{}
$page = 0
do {
    $listing = Invoke-RestMethod -Uri "$($FilesApi)?pageIndex=$page&pageSize=50" -UserAgent $UserAgent
    foreach ($f in @($listing.data)) { if (-not $published.ContainsKey($f.fileName)) { $published[$f.fileName] = $f } }
    $page++
} while (@($listing.data).Count -gt 0 -and $page * 50 -lt [int] $listing.pagination.totalCount)

$client = New-Object System.Net.Http.HttpClient
$client.DefaultRequestHeaders.TryAddWithoutValidation('X-Api-Token', [string] $token) | Out-Null
$client.DefaultRequestHeaders.TryAddWithoutValidation('User-Agent', $UserAgent) | Out-Null

# Posts a multipart form; throws with CurseForge's error on failure, else returns the parsed body.
function Send-CurseForge([string] $Path, [System.Net.Http.MultipartFormDataContent] $Form) {
    $response = $client.PostAsync("$Api$Path", $Form).GetAwaiter().GetResult()
    $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if (-not $response.IsSuccessStatusCode) {
        $detail = $body
        try { $err = $body | ConvertFrom-Json; if ($err.errorMessage) { $detail = "$($err.errorCode): $($err.errorMessage)" } } catch { }
        throw "POST $Path -> $([int]$response.StatusCode) $detail"
    }
    $body | ConvertFrom-Json
}

function New-JsonPart($Value) {
    New-Object System.Net.Http.StringContent(($Value | ConvertTo-Json -Depth 6 -Compress), [Text.Encoding]::UTF8, 'application/json')
}

$failed = 0
try {
    # Previews and -Confirm prompts run here, in file order; the requests run on the pool below.
    $jobs = foreach ($u in $uploads) {
        $file = $u.File
        $name = "StreamlineCore $Version ($($u.Label))"
        $metadata = [ordered]@{
            changelog     = $Changelog
            changelogType = 'markdown'
            displayName   = $name
            gameVersions  = @($u.VersionIds)
            releaseType   = $ReleaseType
        }
        if ($u.Relations.Count) { $metadata.relations = [ordered]@{ projects = @($u.Relations) } }

        $existing = $published[$file.Name]

        if ($WhatIfPreference) {
            $preview = [ordered]@{
                displayName  = $name
                releaseType  = $ReleaseType
                loader       = $u.Loader
                environment  = $u.Environments -join ', '
                games        = $(if ($u.Games.Count -gt 2) { "$($u.Games[0]) .. $($u.Games[-1]) ($($u.Games.Count) versions)" } else { $u.Games -join ', ' })
                gameVersions = @($u.VersionIds)
                relations    = @($u.Relations)
            }
            $state = if ($existing) { " (on CurseForge as file $($existing.id): update metadata)" } else { '' }
            Write-Host "$($file.Name)$state" -ForegroundColor Cyan
            Write-Host ($preview | ConvertTo-Json -Depth 5)
        }

        if ($existing) {
            if (-not $PSCmdlet.ShouldProcess("$name (file $($existing.id))", 'update-file')) { continue }
            $metadata.fileID = [int] $existing.id
        } else {
            if (-not $PSCmdlet.ShouldProcess("$name [$($u.Loader)] [$($u.Games -join ', ')]", "upload-file $($file.Name)")) { continue }
        }
        [pscustomobject]@{ File = $file; Name = $name; Metadata = $metadata; Existing = $existing }
    }
    $jobs = @($jobs)

    if ($jobs.Count) {
        $results = @(Invoke-Parallel -Items $jobs -ThrottleLimit $ThrottleLimit `
            -Variables @{ client = $client; Api = $Api; ProjectId = $ProjectId } `
            -Functions 'Send-CurseForge', 'New-JsonPart' `
            -ScriptBlock {
            param($job)
            $file = $job.File; $name = $job.Name; $existing = $job.Existing
            $form = New-Object System.Net.Http.MultipartFormDataContent
            $stream = $null
            try {
                $form.Add((New-JsonPart $job.Metadata), 'metadata')
                if ($existing) {
                    Send-CurseForge "/projects/$ProjectId/update-file" $form | Out-Null
                    Write-Host ("updated {0} (file {1}), jar unchanged" -f $name, $existing.id) -ForegroundColor Green
                } else {
                    $stream = [IO.File]::OpenRead($file.FullName)
                    $content = New-Object System.Net.Http.StreamContent($stream)
                    $content.Headers.ContentType = [Net.Http.Headers.MediaTypeHeaderValue]::Parse('application/java-archive')
                    $form.Add($content, 'file', $file.Name)
                    $result = Send-CurseForge "/projects/$ProjectId/upload-file" $form
                    Write-Host ("uploaded {0} (file {1}) <- {2}" -f $name, $result.id, $file.Name) -ForegroundColor Green
                }
                $true
            } catch {
                Write-Host "failed  $($file.Name): $($_.Exception.Message)" -ForegroundColor Red
                $false
            } finally {
                if ($stream) { $stream.Dispose() }
                $form.Dispose()
            }
        })
        foreach ($r in $results | Where-Object Error) { Write-Host "failed  $($r.Item.File.Name): $($r.Error)" -ForegroundColor Red }
        $failed += $jobs.Count - @($results | Where-Object { $_ -eq $true }).Count
    }
} finally {
    $client.Dispose()
}

Write-Host 'Done!' -ForegroundColor Green
if ($failed) { exit 1 }
