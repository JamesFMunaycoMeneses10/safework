[CmdletBinding()]
param([string]$RunAt='20:00',[string]$OutputDirectory=(Join-Path $env:LOCALAPPDATA 'SafeWork\Backups\Docker'))
$ErrorActionPreference='Stop'
$time=[DateTime]::ParseExact($RunAt,'HH:mm',[Globalization.CultureInfo]::InvariantCulture)
$script=Join-Path $PSScriptRoot 'backup-docker.ps1'
if (-not (Test-Path -LiteralPath $script)) {throw 'Falta backup-docker.ps1.'}
$engine=Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
$arguments='-NoLogo -NoProfile -ExecutionPolicy Bypass -File "'+$script+'" -OutputDirectory "'+[IO.Path]::GetFullPath($OutputDirectory)+'"'
$action=New-ScheduledTaskAction -Execute $engine -Argument $arguments
$trigger=New-ScheduledTaskTrigger -Daily -At $time
$settings=New-ScheduledTaskSettingsSet -StartWhenAvailable -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Hours 1)
$principal=New-ScheduledTaskPrincipal -UserId ([Security.Principal.WindowsIdentity]::GetCurrent().Name) -LogonType Interactive -RunLevel Limited
$task=New-ScheduledTask -Action $action -Trigger $trigger -Settings $settings -Principal $principal -Description 'Respaldo privado del MySQL de Docker de SafeWork; requiere Docker Desktop encendido.'
Register-ScheduledTask -TaskName 'SafeWork Docker Backup' -InputObject $task -Force | Out-Null
Write-Output ('Respaldo Docker programado a las '+$RunAt+'. Requiere sesion de Windows y motor Docker en ejecucion. Mantener tambien una copia externa protegida.')
