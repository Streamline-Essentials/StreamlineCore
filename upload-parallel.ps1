#Requires -Version 5.1
<#
.SYNOPSIS
  Invoke-Parallel for the upload scripts: runs a script block over items on a runspace pool.

.DESCRIPTION
  Dot-sourced by upload-modules.ps1, upload-modrinth.ps1 and upload-curseforge.ps1.
  Works on Windows PowerShell 5.1, which has no ForEach-Object -Parallel.

  Each item runs in its own runspace, at most -ThrottleLimit at a time. The runspaces
  share the console host, so Write-Host from a job prints straight away, one whole line
  at a time; other output is returned to the caller as each job finishes. A job that
  throws returns an object with Item and Error instead.
#>

function Invoke-Parallel {
    param(
        [Parameter(Mandatory)] [AllowEmptyCollection()] [object[]] $Items,
        # Receives one item as its first argument.
        [Parameter(Mandatory)] [scriptblock] $ScriptBlock,
        # Variables copied into every runspace, by name.
        [hashtable] $Variables = @{},
        # Functions of the calling script made available in every runspace.
        [string[]] $Functions = @(),
        [int] $ThrottleLimit = 10
    )

    $state = [Management.Automation.Runspaces.InitialSessionState]::CreateDefault()
    $shared = @{ ErrorActionPreference = 'Stop'; ProgressPreference = 'SilentlyContinue' }
    foreach ($k in $Variables.Keys) { $shared[$k] = $Variables[$k] }
    foreach ($k in $shared.Keys) {
        $state.Variables.Add((New-Object Management.Automation.Runspaces.SessionStateVariableEntry($k, $shared[$k], $null)))
    }
    foreach ($f in $Functions) {
        $definition = (Get-Command $f -CommandType Function).Definition
        $state.Commands.Add((New-Object Management.Automation.Runspaces.SessionStateFunctionEntry($f, $definition)))
    }

    $pool = [RunspaceFactory]::CreateRunspacePool(1, [Math]::Max(1, $ThrottleLimit), $state, $Host)
    $pool.Open()
    $jobs = New-Object System.Collections.Generic.List[object]
    try {
        foreach ($item in $Items) {
            $ps = [PowerShell]::Create()
            $ps.RunspacePool = $pool
            [void] $ps.AddScript($ScriptBlock.ToString()).AddArgument($item)
            $jobs.Add([pscustomobject]@{ PS = $ps; Handle = $ps.BeginInvoke(); Item = $item })
        }
        while ($jobs.Count) {
            $done = @($jobs | Where-Object { $_.Handle.IsCompleted })
            if (-not $done.Count) {
                [void] $jobs[0].Handle.AsyncWaitHandle.WaitOne(100)
                continue
            }
            foreach ($job in $done) {
                [void] $jobs.Remove($job)
                try {
                    $job.PS.EndInvoke($job.Handle)
                } catch {
                    $inner = $_.Exception
                    while ($inner.InnerException) { $inner = $inner.InnerException }
                    [pscustomobject]@{ Item = $job.Item; Error = $inner.Message }
                } finally {
                    $job.PS.Dispose()
                }
            }
        }
    } finally {
        foreach ($job in $jobs) { $job.PS.Stop(); $job.PS.Dispose() }
        $pool.Close()
        $pool.Dispose()
    }
}
