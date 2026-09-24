[CmdletBinding()]
param(
    [string]$RunAt = "02:00",
    [string]$OutputDirectory = (Join-Path $env:LOCALAPPDATA "SafeWork\Backups\MySQL"),
    [string]$TaskName = "SafeWork MySQL Backup"
)

$ErrorActionPreference = "Stop"
if ($env:OS -ne "Windows_NT") {
    throw "La programación automática está disponible en Windows."
}
try {
    $runTime = [DateTime]::ParseExact($RunAt, "HH:mm", [Globalization.CultureInfo]::InvariantCulture)
} catch {
    throw "La hora debe usar formato de 24 horas HH:mm, por ejemplo 02:00."
}

$backupScript = Join-Path $PSScriptRoot "backup-mysql.ps1"
if (-not (Test-Path -LiteralPath $backupScript -PathType Leaf)) {
    throw "No se encontró el script de respaldo: $backupScript"
}
$powerShell = Join-Path $PSHOME "powershell.exe"
$scriptArgument = $backupScript.Replace('"', '\"')
$directoryArgument = [System.IO.Path]::GetFullPath($OutputDirectory).Replace('"', '\"')
$taskArguments = "-NoLogo -NoProfile -ExecutionPolicy Bypass -File `"$scriptArgument`" -OutputDirectory `"$directoryArgument`""

$action = New-ScheduledTaskAction -Execute $powerShell -Argument $taskArguments
$trigger = New-ScheduledTaskTrigger -Daily -At $runTime
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -MultipleInstances IgnoreNew
$principal = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" `
    -LogonType Interactive -RunLevel Limited
$task = New-ScheduledTask -Action $action -Trigger $trigger -Settings $settings -Principal $principal `
    -Description "Respaldo diario de la base MySQL de SafeWork."
Register-ScheduledTask -TaskName $TaskName -InputObject $task -Force | Out-Null

Write-Output "Tarea '$TaskName' programada diariamente a las $RunAt."
Write-Output "Se ejecutará con la cuenta actual cuando inicie sesión en Windows."
Write-Output "Carpeta de respaldos: $directoryArgument"
