#Requires -Version 5.1
<#
.SYNOPSIS
  Uploads built module jars to the Streamline module registry (modules.drak.gg).

.DESCRIPTION
  Finds the newest jar in modules\<Module>\build\libs for each module (skipping
  -plain, -sources and -javadoc jars) and POSTs it to
  <BaseUrl>/api/v1/modules/<Name>/upload, authenticated with the master key in
  $env:SLMODULES_MASTER_TOKEN. The registry reads the Plugin-Id, version and
  the rest of the metadata from the jar's manifest; uploading an existing
  version replaces it.

  <Name> is the jar's file name without the trailing "-<version>", which is the
  moduleName that modules\module.gradle uses for archiveFileName.

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
  .\upload-modules.ps1 -WhatIf                        # list what would be uploaded
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

$failed = 0
foreach ($file in $jars) {
    # "StreamlineMOTD-1.9.0" / "Foo-1.0.0-beta" -> module name; the version comes from the manifest.
    $name = $file.BaseName -replace '-\d+(\.\d+)*([-+.][A-Za-z0-9.+-]*)?$', ''
    $url = "$BaseUrl/api/v1/modules/$([Uri]::EscapeDataString($name))/upload"

    if (-not $PSCmdlet.ShouldProcess("$url", "Upload $($file.Name)")) { continue }

    try {
        $result = Invoke-RestMethod -Method Post -Uri $url -InFile $file.FullName `
            -ContentType 'application/java-archive' `
            -Headers @{ Authorization = "Bearer $token" }
        Write-Host ("uploaded {0} {1} ({2}) <- {3}" -f $result.name, $result.version, $result.id, $file.Name) -ForegroundColor Green
    } catch {
        $failed++
        $detail = $_.ErrorDetails.Message
        try { $detail = ($detail | ConvertFrom-Json).error } catch { }
        $status = $_.Exception.Response.StatusCode.value__
        Write-Host "failed  $($file.Name): $status $detail" -ForegroundColor Red
    }
}

if ($failed) { exit 1 }
