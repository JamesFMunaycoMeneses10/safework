[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,
    [string]$TargetDatabase,
    [string]$LoginPath = "safework-backup",
    [string]$OutputDirectory = (Join-Path $env:LOCALAPPDATA "SafeWork\Backups\MySQL"),
    [switch]$ReplaceDatabase,
    [string]$ConfirmDatabaseName
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $BackupFile -PathType Leaf)) {
    throw "No se encontró el archivo de respaldo: $BackupFile"
}
$BackupFile = (Resolve-Path -LiteralPath $BackupFile).Path
if ([System.IO.Path]::GetExtension($BackupFile) -ne ".sql") {
    throw "El archivo debe ser un respaldo .sql de SafeWork."
}
if (-not $TargetDatabase) {
    $TargetDatabase = "safework_restore_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
}
if ($TargetDatabase -notmatch '^[A-Za-z][A-Za-z0-9_]{0,63}$') {
    throw "Nombre de base de datos no válido."
}
if ($LoginPath -notmatch '^[A-Za-z0-9_-]{1,64}$') {
    throw "Nombre de login-path no válido."
}

$mysqlCommand = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysqlCommand) {
    throw "No se encontró mysql. Instala MySQL Server/Client o agrega su carpeta bin al PATH."
}

$checksumFile = "$BackupFile.sha256"
if (Test-Path -LiteralPath $checksumFile -PathType Leaf) {
    $checksumLine = Get-Content -LiteralPath $checksumFile -TotalCount 1
    if ($checksumLine -notmatch '^([0-9a-fA-F]{64})\s+') {
        throw "El archivo SHA-256 tiene un formato inválido."
    }
    $expectedHash = $Matches[1].ToLowerInvariant()
    $actualHash = (Get-FileHash -LiteralPath $BackupFile -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actualHash -ne $expectedHash) {
        throw "La copia no coincide con su SHA-256; se cancela la restauración."
    }
    Write-Output "Integridad SHA-256 comprobada."
} else {
    Write-Warning "La copia no tiene archivo SHA-256 asociado; solo se validará que MySQL pueda importarla."
}

function Invoke-MySqlStatement([string]$Statement) {
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $mysqlCommand.Source
    $startInfo.Arguments = "--login-path=$LoginPath --batch --skip-column-names --execute=`"$Statement`""
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    [void]$process.Start()
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    $process.WaitForExit()
    $stdout = $stdoutTask.GetAwaiter().GetResult()
    $stderr = $stderrTask.GetAwaiter().GetResult()
    if ($process.ExitCode -ne 0) {
        throw "MySQL rechazó la operación: $stderr"
    }
    return $stdout.Trim()
}

function Import-MySqlFile([string]$File, [string]$Database) {
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $mysqlCommand.Source
    $startInfo.Arguments = "--login-path=$LoginPath --default-character-set=utf8mb4 $Database"
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    [void]$process.Start()
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    try {
        $stream = [System.IO.File]::OpenRead($File)
        try { $stream.CopyTo($process.StandardInput.BaseStream) } finally { $stream.Dispose() }
    } finally {
        $process.StandardInput.Close()
    }
    $process.WaitForExit()
    $stderr = $stderrTask.GetAwaiter().GetResult()
    if ($process.ExitCode -ne 0) {
        throw "La importación SQL falló (código $($process.ExitCode)): $stderr"
    }
}

$existingDatabase = Invoke-MySqlStatement "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='$TargetDatabase'"
$replacingExisting = -not [string]::IsNullOrWhiteSpace($existingDatabase)
$preRestoreFile = $null

if ($replacingExisting) {
    if (-not $ReplaceDatabase) {
        throw "La base '$TargetDatabase' ya existe. Usa otro nombre para probar la copia o especifica -ReplaceDatabase para reemplazarla."
    }
    if ($ConfirmDatabaseName -cne $TargetDatabase) {
        throw "Para reemplazar una base existente, -ConfirmDatabaseName debe coincidir exactamente con '$TargetDatabase'."
    }
    Write-Warning "Se reemplazará '$TargetDatabase'. Primero crearé un respaldo de seguridad de su contenido actual."
    $preRestoreDirectory = Join-Path ([System.IO.Path]::GetFullPath($OutputDirectory)) "pre-restore"
    $backupStartedAt = Get-Date
    & (Join-Path $PSScriptRoot "backup-mysql.ps1") -Database $TargetDatabase `
        -LoginPath $LoginPath -OutputDirectory $preRestoreDirectory
    $preRestoreFile = Get-ChildItem -LiteralPath $preRestoreDirectory -File -Filter "$TargetDatabase`_*.sql" |
        Where-Object {
            $_.Name -match "^$([regex]::Escape($TargetDatabase))_\d{8}_\d{6}\.sql$" -and
            $_.LastWriteTime -ge $backupStartedAt
        } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
    if (-not $preRestoreFile) {
        throw "No se encontró el respaldo previo. No se modificó '$TargetDatabase'."
    }
    $confirmation = Read-Host "Escribe RESTAURAR $TargetDatabase para continuar"
    if ($confirmation -cne "RESTAURAR $TargetDatabase") {
        throw "Restauración cancelada. La base existente sigue intacta."
    }
} elseif ($ReplaceDatabase) {
    throw "La base '$TargetDatabase' no existe; quita -ReplaceDatabase para restaurar en una base nueva."
}

try {
    if ($replacingExisting) {
        [void](Invoke-MySqlStatement "DROP DATABASE ``$TargetDatabase``")
    }
    [void](Invoke-MySqlStatement "CREATE DATABASE ``$TargetDatabase`` CHARACTER SET utf8mb4")
    Write-Output "Importando $BackupFile en '$TargetDatabase'..."
    Import-MySqlFile $BackupFile $TargetDatabase
    $tables = Invoke-MySqlStatement "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='$TargetDatabase'"
    if ([int]$tables -le 0) {
        throw "La importación terminó sin crear tablas."
    }
    Write-Output "Restauración completada. Base '$TargetDatabase' contiene $tables tablas."
} catch {
    $restoreError = $_.Exception.Message
    try {
        [void](Invoke-MySqlStatement "DROP DATABASE IF EXISTS ``$TargetDatabase``")
    } catch {
        if (-not $replacingExisting) {
            throw "Falló la restauración y no se pudo limpiar la base parcial '$TargetDatabase'. Error: $restoreError. Limpieza: $($_.Exception.Message)"
        }
    }
    if ($replacingExisting -and $preRestoreFile) {
        try {
            [void](Invoke-MySqlStatement "CREATE DATABASE ``$TargetDatabase`` CHARACTER SET utf8mb4")
            Import-MySqlFile $preRestoreFile $TargetDatabase
            throw "La restauración de la copia solicitada falló y se recuperó la base anterior desde '$preRestoreFile'. Error original: $restoreError"
        } catch {
            if ($_.Exception.Message -like "La restauración de la copia solicitada falló*") { throw }
            throw "Falló la importación y también la recuperación automática. El respaldo previo está en '$preRestoreFile'. Error de importación: $restoreError. Error de recuperación: $($_.Exception.Message)"
        }
    }
    throw $restoreError
}
