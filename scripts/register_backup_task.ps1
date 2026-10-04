<#
.SYNOPSIS
  Registers the "KasiGuru Daily Backup" scheduled task on this PC.

.DESCRIPTION
  Runs functions/backup_daily.js every day at -At, and again 10 minutes after you sign in, as you
  and without a window. A run missed because the PC was off starts as soon as it is back on. The
  backup skips itself when today's already exists, so the triggers add up to one backup a day.

  Each finished backup is also copied to -MirrorDir (your OneDrive by default), so it outlives this
  PC. Re-run this script to change the key, the folders or the time; it replaces the task.

  The task runs while you are signed in. "Run whether user is logged on or not" needs an elevated
  prompt and a stored password, which this script deliberately does not ask for.

.EXAMPLE
  .\scripts\register_backup_task.ps1 -KeyFile "C:\path\to\service-account.json"
#>
param(
  [Parameter(Mandatory = $true)] [string] $KeyFile,
  [string] $MirrorDir = $(if ($env:OneDrive) { Join-Path $env:OneDrive 'KasiGuruBackups' } else { '' }),
  [string] $At = '12:00',
  [int] $Keep = 14,
  [string] $TaskName = 'KasiGuru Daily Backup'
)

$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent $PSScriptRoot
$script = Join-Path $repo 'functions\backup_daily.js'
$node = (Get-Command node).Source
if (-not (Test-Path $KeyFile)) { throw "Key file not found: $KeyFile" }
$KeyFile = (Resolve-Path $KeyFile).Path

# Single-quoted inside the -Command string so paths with spaces survive; none of these paths can
# contain a single quote of their own on this machine, and the check below makes sure.
$parts = @($node, $script, $KeyFile, "--keep=$Keep")
if ($MirrorDir) { $parts += "--mirror=$MirrorDir" }
if ($parts | Where-Object { $_ -like "*'*" }) { throw 'A path contains a single quote; move it first.' }
$command = '& ' + (($parts | ForEach-Object { "'$_'" }) -join ' ') + '; exit $LASTEXITCODE'

$action = New-ScheduledTaskAction -Execute 'powershell.exe' `
  -Argument "-NoProfile -NonInteractive -WindowStyle Hidden -Command `"$command`"" `
  -WorkingDirectory (Join-Path $repo 'functions')

$user = "$env:USERDOMAIN\$env:USERNAME"
$daily = New-ScheduledTaskTrigger -Daily -At $At
$logon = New-ScheduledTaskTrigger -AtLogOn -User $user
$logon.Delay = 'PT10M'

$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -RunOnlyIfNetworkAvailable `
  -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
  -ExecutionTimeLimit (New-TimeSpan -Hours 1) -MultipleInstances IgnoreNew
$principal = New-ScheduledTaskPrincipal -UserId $user -LogonType Interactive -RunLevel Limited

Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $daily, $logon `
  -Settings $settings -Principal $principal -Force `
  -Description 'Full Firestore backup of KasiGuru (functions/backup_daily.js). See docs/BACKUP_AND_RESET.md.' | Out-Null

Write-Host "Registered '$TaskName': daily at $At and 10 minutes after sign-in."
if ($MirrorDir) { Write-Host "Copies go to: $MirrorDir" } else { Write-Host 'No -MirrorDir: backups stay on this PC only.' }
Write-Host "Run it now:  Start-ScheduledTask -TaskName '$TaskName'"
Write-Host "Last result: Get-ScheduledTaskInfo -TaskName '$TaskName'   (0 = fine, 1 = failed, 2 = looks like a wipe)"
Write-Host 'Log:         backup_daily.log in the backup folder'
