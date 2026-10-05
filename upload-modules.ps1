#Requires -Version 5.1
<#
.SYNOPSIS
  Uploads built module jars to the Streamline module registry (modules.drak.gg).

.DESCRIPTION
  Finds the newest jar in modules\<Module>\build\libs for each module (skipping
  -plain, -sources and -javadoc jars) and publishes it under the jar's own
  Plugin-Version, authenticated with the master key in
  $env:SLMODULES_MASTER_TOKEN:

  - a version the registry does not have yet is created with
    POST <BaseUrl>/api/v1/<Name>/upload/<version>;
  - a version it already has is replaced with
    PATCH <BaseUrl>/api/v1/<Name>/upsert/<version>.

  The registry reads the Plugin-Id and the rest of the metadata from the jar's
  manifest. API reference: https://modules.drak.gg/docs/

  <Name> is the jar's file name without the trailing "-<version>", which is the
  moduleName that modules\module.gradle uses for archiveFileName. The version
  is the manifest's Plugin-Version, falling back to that file-name suffix.

.PARAMETER Module
  Module project folders to upload (e.g. StreamlineMOTD, TacoEssentials).
  Defaults to every folder under modules\ that has a built jar.

.PARAMETER Jar
  Explicit jar files to upload instead of searching modules\*\build\libs.

.PARAMETER BaseUrl
  Registry root. Defaults to https://modules.drak.gg.

.PARAMETER Build
  Runs "gradlew shadowJar" for the selected modules before uploading.

.EXAMPLE
  .\upload-modules.ps1                                # every built module
.EXAMPLE
  .\upload-modules.ps1 StreamlineMOTD TacoEssentials -Build
.EXAMPLE
  .\upload-modules.ps1 -Jar .\deploy\StreamlineMOTD-1.9.0.jar
.EXAMPLE
  .\upload-modules.ps1 -WhatIf                        # list what would be uploaded, and how
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Position = 0, ValueFromRemainingArguments)]
    [string[]] $Module,
    [string[]] $Jar,
    [string] $BaseUrl = 'https://modules.drak.gg',
    [switch] $Build
)

$ErrorActionPreference = 'Stop'
$modulesRoot = Join-Path $PSScriptRoot 'modules'
$BaseUrl = $BaseUrl.TrimEnd('/')

# Windows PowerShell 5.1 offers only TLS 1.0/1.1 by default; the registry requires 1.2+.
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

$token = $env:SLMODULES_MASTER_TOKEN
if ([string]::IsNullOrWhiteSpace($token) -and -not $WhatIfPreference) {
    throw 'Set $env:SLMODULES_MASTER_TOKEN to the registry master key.'
}

function Get-ModuleFolders {
    if ($Module) {
        foreach ($name in $Module) {
            $dir = Join-Path $modulesRoot $name
            if (-not (Test-Path $dir -PathType Container)) { throw "No module folder '$name' under $modulesRoot." }
            Get-Item $dir
        }
    } else {
        Get-ChildItem $modulesRoot -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'build.gradle') }
    }
}

if ($Build -and -not $Jar) {
    # @() keeps a single task an array; splatting a lone string passes it character by character.
    $tasks = @(Get-ModuleFolders | ForEach-Object { ":modules:$($_.Name):shadowJar" })
    if ($PSCmdlet.ShouldProcess($tasks -join ' ', 'gradlew')) {
        # Gradle writes warnings to stderr, which Windows PowerShell can turn into terminating errors under 'Stop'.
        $ErrorActionPreference = 'Continue'
        & (Join-Path $PSScriptRoot 'gradlew.bat') @tasks
        $ErrorActionPreference = 'Stop'
        if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed.' }
    }
}

$jars = if ($Jar) {
    $Jar | ForEach-Object { Get-Item $_ }
} else {
    Get-ModuleFolders | ForEach-Object {
        $found = Get-ChildItem (Join-Path $_.FullName 'build\libs') -Filter *.jar -ErrorAction SilentlyContinue |
            Where-Object Name -NotMatch '-(sources|javadoc|plain)\.jar$' |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($found) { $found } else { Write-Warning "$($_.Name): no built jar in build\libs (use -Build)." }
    }
}

if (-not $jars) { throw 'Nothing to upload.' }

# Plugin-Version from META-INF/MANIFEST.MF, or $null when the jar has none.
function Get-PluginVersion([IO.FileInfo] $file) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($file.FullName)
    try {
        $entry = $zip.GetEntry('META-INF/MANIFEST.MF')
        if (-not $entry) { return $null }
        $reader = New-Object IO.StreamReader($entry.Open())
        try { $text = $reader.ReadToEnd() } finally { $reader.Dispose() }
        # Manifest continuation lines start with a single space.
        $text = $text -replace '\r?\n ', ''
        $match = [regex]::Match($text, '(?m)^Plugin-Version:[ \t]*(.+?)[ \t]*\r?$')
        if ($match.Success) { $match.Groups[1].Value } else { $null }
    } finally {
        $zip.Dispose()
    }
}

# Versions the registry already stores for a module; empty when the module is new.
function Get-RemoteVersions([string] $name) {
    try {
        $remote = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/modules/$([Uri]::EscapeDataString($name))"
        @($remote.versions | ForEach-Object { $_.version })
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 404) { return @() }
        throw
    }
}

$failed = 0
foreach ($file in $jars) {
    # "StreamlineMOTD-1.9.0" / "Foo-1.0.0-beta" -> name "StreamlineMOTD" / "Foo", suffix "1.9.0" / "1.0.0-beta".
    $suffix = '-(\d+(\.\d+)*([-+.][A-Za-z0-9.+-]*)?)$'
    $name = $file.BaseName -replace $suffix, ''
    $version = Get-PluginVersion $file
    if (-not $version -and $file.BaseName -match $suffix) { $version = $Matches[1] }
    if (-not $version) {
        $failed++
        Write-Host "failed  $($file.Name): no Plugin-Version in the manifest and no version in the file name" -ForegroundColor Red
        continue
    }

    $path = "$([Uri]::EscapeDataString($name))/{0}/$([Uri]::EscapeDataString($version))"

    try {
        # POST /upload never replaces a version (409), so an existing one goes through PATCH /upsert.
        if ((Get-RemoteVersions $name) -contains $version) {
            $method = 'Patch'; $verb = 'patched'
            $url = "$BaseUrl/api/v1/$($path -f 'upsert')"
        } else {
            $method = 'Post'; $verb = 'uploaded'
            $url = "$BaseUrl/api/v1/$($path -f 'upload')"
        }

        if (-not $PSCmdlet.ShouldProcess($url, "$($method.ToUpper()) $($file.Name) as $name $version")) { continue }

        $result = Invoke-RestMethod -Method $method -Uri $url -InFile $file.FullName `
            -ContentType 'application/java-archive' `
            -Headers @{ Authorization = "Bearer $token" }
        Write-Host ("{0} {1} {2} ({3}) <- {4}" -f $verb.PadRight(8), $result.name, $version, $result.id, $file.Name) -ForegroundColor Green
    } catch {
        $failed++
        $detail = $_.ErrorDetails.Message
        try { $detail = ($detail | ConvertFrom-Json).error } catch { }
        if (-not $detail) { $detail = $_.Exception.Message }
        $status = $_.Exception.Response.StatusCode.value__
        Write-Host "failed  $($file.Name): $status $detail" -ForegroundColor Red
    }
}

if ($failed) { exit 1 }
