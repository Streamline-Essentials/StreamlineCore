#Requires -Version 5.1
<#
.SYNOPSIS
  Publishes the StreamlineCore platform jars in deploy\ to the module registry's core
  builds (modules.drak.gg/api/v1/core), which the core's own updater checks.

.DESCRIPTION
  Uploads every platform jar of -Version (StreamlineCore-<Platform>-<version>.jar) as that
  version of its core target, authenticated with the master key in
  $env:SLMODULES_MASTER_TOKEN. The target is the platform part of the file name,
  lower-cased: spigot, bungee and velocity for plugins, <fabric|forge|neoforge>-<mc> for
  mods (StreamlineCore-Fabric-1211-2.6.1.8.jar is fabric-1211). The API, BAPI,
  Singularity and ModuleCore jars are libraries, not server jars, and are skipped.

  - a version the registry does not store yet is created with
    POST <BaseUrl>/api/v1/core/<target>/upload/<version>;
  - a version it already stores is replaced with
    PATCH <BaseUrl>/api/v1/core/<target>/upsert/<version>.

  GET /core/<target> answers from Modrinth for targets the registry holds nothing for,
  so only builds listed with source "registry" count as stored.

  Mod jars send their Minecraft version (mcVersion in <loader>\<loader>-<mc>\build.gradle)
  as ?minecraft=, and Forge 1.8.9 is marked ?client=true, since it runs on game clients
  only. API reference: https://modules.drak.gg/docs/

.PARAMETER Platform
  Which jars to publish, matched against the part of the name after "StreamlineCore-"
  (e.g. Spigot, Velocity, Fabric, NeoForge-1211). Defaults to every platform jar.

.PARAMETER Version
  Version to publish. Defaults to "version" in gradle.properties.

.PARAMETER DeployDir
  Folder holding the built jars. Defaults to $env:DEPLOY_DIR, else .\deploy.

.PARAMETER BaseUrl
  Registry root. Defaults to https://modules.drak.gg.

.PARAMETER ThrottleLimit
  How many jars are looked up and uploaded at once. Defaults to 10.

.EXAMPLE
  .\publish-core.ps1                             # every platform jar of the current version
.EXAMPLE
  .\publish-core.ps1 Spigot Bungee Velocity      # plugin jars only
.EXAMPLE
  .\publish-core.ps1 NeoForge -Version 2.6.1.8
.EXAMPLE
  .\publish-core.ps1 -WhatIf                     # list what would be published, and how
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Position = 0, ValueFromRemainingArguments)]
    [string[]] $Platform,
    [string] $Version,
    [string] $DeployDir,
    [string] $BaseUrl = 'https://modules.drak.gg',
    [ValidateRange(1, 64)]
    [int] $ThrottleLimit = 10
)

$ErrorActionPreference = 'Stop'
# Invoke-RestMethod otherwise redraws a progress bar for every request.
$ProgressPreference = 'SilentlyContinue'
. (Join-Path $PSScriptRoot 'upload-parallel.ps1')

$BaseUrl = $BaseUrl.TrimEnd('/')
# The registry's core target pattern.
$targetPattern = '^(spigot|bungee|velocity|(fabric|forge|neoforge)-[0-9]{2,6})$'
# Mod jars that run only on a game client (forge\forge-189 has no server side).
$clientOnly = @('forge-189')

# Windows PowerShell 5.1 offers only TLS 1.0/1.1 by default; the registry requires 1.2+.
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

$token = $env:SLMODULES_MASTER_TOKEN
if ([string]::IsNullOrWhiteSpace($token) -and -not $WhatIfPreference) {
    throw 'Set $env:SLMODULES_MASTER_TOKEN to the registry master key.'
}

if (-not $Version) {
    $line = Get-Content (Join-Path $PSScriptRoot 'gradle.properties') | Where-Object { $_ -match '^\s*version\s*=' } | Select-Object -First 1
    if (-not $line) { throw 'No version in gradle.properties; pass -Version.' }
    $Version = ($line -split '=', 2)[1].Trim()
}

if (-not $DeployDir) {
    $DeployDir = if ($env:DEPLOY_DIR) { $env:DEPLOY_DIR } else { Join-Path $PSScriptRoot 'deploy' }
}
if (-not (Test-Path $DeployDir -PathType Container)) { throw "No deploy folder at $DeployDir; build first." }

# mcVersion from a mod project's build.gradle (fabric\fabric-1211\build.gradle); $null for plugins.
function Get-MinecraftVersion([string] $target) {
    if ($target -eq 'forge-189') { return '1.8.9' }
    if ($target -notmatch '^(fabric|forge|neoforge)-') { return $null }
    $loader = $target -replace '-.*$', ''
    $gradle = Join-Path $PSScriptRoot "$loader\$target\build.gradle"
    if (-not (Test-Path $gradle)) { return $null }
    $match = Select-String -Path $gradle -Pattern "mcVersion\s*=\s*'([^']+)'" | Select-Object -First 1
    if ($match) { $match.Matches[0].Groups[1].Value } else { $null }
}

# The registry's error message for a failed request, with its HTTP status when there is one.
function Get-ErrorText($record) {
    $detail = $record.ErrorDetails.Message
    try { $detail = ($detail | ConvertFrom-Json).error } catch { }
    if (-not $detail) { $detail = $record.Exception.Message }
    $status = $record.Exception.Response.StatusCode.value__
    if ($status) { "$status $detail" } else { $detail }
}

# API, BAPI, Singularity and ModuleCore are libraries for developers, not server jars.
$suffix = "-$([regex]::Escape($Version))\.jar$"
$failed = 0
$pending = @(foreach ($file in Get-ChildItem $DeployDir -File -Filter "StreamlineCore-*-$Version.jar") {
    $key = $file.Name -replace '^StreamlineCore-', '' -replace $suffix, ''
    if (@('API', 'BAPI', 'Singularity', 'ModuleCore') -contains $key) { continue }
    if ($Platform -and -not ($Platform | Where-Object { $key -eq $_ -or $key -like "$_-*" })) { continue }

    $target = $key.ToLowerInvariant()
    if ($target -notmatch $targetPattern) {
        Write-Warning "$($file.Name): '$target' is not a core target, skipped."
        continue
    }

    $query = @()
    $minecraft = Get-MinecraftVersion $target
    if ($minecraft) { $query += "minecraft=$([Uri]::EscapeDataString($minecraft))" }
    if ($clientOnly -contains $target) { $query += 'client=true' }

    [pscustomobject]@{
        File = $file; Target = $target; Minecraft = $minecraft
        Client = $clientOnly -contains $target
        Path = "core/$target/{0}/$([Uri]::EscapeDataString($Version))" + $(if ($query) { '?' + ($query -join '&') } else { '' })
        Method = $null; Verb = $null; Url = $null
    }
})

if (-not $pending.Count) { throw "No platform jars of $Version in $DeployDir." }

$shared = @{ BaseUrl = $BaseUrl; token = $token; Version = $Version }

# Which targets already store this version. POST /upload never replaces one (409),
# so a stored version goes through PATCH /upsert.
$lookups = @(Invoke-Parallel -Items $pending -ThrottleLimit $ThrottleLimit -Variables $shared -Functions 'Get-ErrorText' -ScriptBlock {
    param($job)
    try {
        $remote = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/core/$($job.Target)"
        $stored = @($remote.versions | Where-Object { $_.source -eq 'registry' } | ForEach-Object { $_.version })
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 404) {
            Write-Host "failed  $($job.File.Name): $(Get-ErrorText $_)" -ForegroundColor Red
            return
        }
        $stored = @()
    }
    if ($stored -contains $Version) {
        $job.Method = 'Patch'; $job.Verb = 'replaced'
        $job.Url = "$BaseUrl/api/v1/$($job.Path -f 'upsert')"
    } else {
        $job.Method = 'Post'; $job.Verb = 'uploaded'
        $job.Url = "$BaseUrl/api/v1/$($job.Path -f 'upload')"
    }
    $job
})
$failed += $pending.Count - @($pending | Where-Object { $_.Url }).Count
foreach ($l in $lookups | Where-Object Error) { Write-Host "failed  $($l.Item.File.Name): $($l.Error)" -ForegroundColor Red }

# Asked on this thread, in file order, so -WhatIf and -Confirm read in order.
$approved = @($pending | Where-Object { $_.Url } | Where-Object {
    $details = @($_.Minecraft, $(if ($_.Client) { 'client only' })) | Where-Object { $_ }
    $PSCmdlet.ShouldProcess($_.Url, "$($_.Method.ToUpper()) $($_.File.Name) as $($_.Target) $Version$(if ($details) { " [$($details -join ', ')]" })")
})

if ($approved.Count) {
    $results = @(Invoke-Parallel -Items $approved -ThrottleLimit $ThrottleLimit -Variables $shared -Functions 'Get-ErrorText' -ScriptBlock {
        param($job)
        try {
            $result = Invoke-RestMethod -Method $job.Method -Uri $job.Url -InFile $job.File.FullName `
                -ContentType 'application/octet-stream' `
                -Headers @{ Authorization = "Bearer $token" }
            $mc = if ($result.minecraft) { " (Minecraft $($result.minecraft))" } else { '' }
            Write-Host ("{0} {1} {2}{3} <- {4}" -f $job.Verb.PadRight(8), $result.target, $result.version, $mc, $job.File.Name) -ForegroundColor Green
            $true
        } catch {
            Write-Host "failed  $($job.File.Name): $(Get-ErrorText $_)" -ForegroundColor Red
            $false
        }
    })
    foreach ($r in $results | Where-Object Error) { Write-Host "failed  $($r.Item.File.Name): $($r.Error)" -ForegroundColor Red }
    $failed += $approved.Count - @($results | Where-Object { $_ -eq $true }).Count
}

Write-Host 'Done!' -ForegroundColor Green
if ($failed) { exit 1 }
