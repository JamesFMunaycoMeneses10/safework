[CmdletBinding()]
param(
    [string]$Database = "safework_db",
    [string]$LoginPath = "safework-backup",
    [string]$OutputDirectory = (Join-Path $env:LOCALAPPDATA "SafeWork\Backups\MySQL"),
    [ValidateRange(1, 3650)]
    [int]$RetentionDays = 30
)

$ErrorActionPreference = "Stop"

if ($Database -notmatch '^[A-Za-z][A-Za-z0-9_]{0,63}$') {
    throw "Nombre de base de datos no válido."
}
if ($LoginPath -notmatch '^[A-Za-z0-9_-]{1,64}$') {
    throw "Nombre de login-path no válido."
}

$dumpCommand = Get-Command mysqldump -ErrorAction SilentlyContinue
if (-not $dumpCommand) {
    throw "No se encontró mysqldump. Instala MySQL Server/Client o agrega su carpeta bin al PATH."
}

$OutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
if (-not (Test-Path -LiteralPath $OutputDirectory -PathType Container)) {
    New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
    if ($env:OS -eq "Windows_NT") {
        $acl = Get-Acl -LiteralPath $OutputDirectory
        $acl.SetAccessRuleProtection($true, $false)
        $identity = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
        $currentUserRule = [System.Security.AccessControl.FileSystemAccessRule]::new(
            $identity, "FullControl", "ContainerInherit,ObjectInherit", "None", "Allow")
        $systemRule = [System.Security.AccessControl.FileSystemAccessRule]::new(
            "SYSTEM", "FullControl", "ContainerInherit,ObjectInherit", "None", "Allow")
        $acl.AddAccessRule($currentUserRule)
        $acl.AddAccessRule($systemRule)
        Set-Acl -LiteralPath $OutputDirectory -AclObject $acl
    }
}

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$baseName = "${Database}_${stamp}"
$finalFile = Join-Path $OutputDirectory "$baseName.sql"
$tempFile = Join-Path $OutputDirectory "$baseName.partial.sql"
$transcriptFile = Join-Path $OutputDirectory "backup-$(Get-Date -Format 'yyyyMMdd').log"
$transcriptStarted = $false

try {
    try {
        Start-Transcript -LiteralPath $transcriptFile -Append | Out-Null
        $transcriptStarted = $true
    } catch {
        Write-Warning "No se pudo iniciar el registro de actividad: $($_.Exception.Message)"
    }

    Write-Output "Iniciando respaldo de '$Database' en $OutputDirectory"
    $dumpArguments = @(
        "--login-path=$LoginPath",
        "--single-transaction",
        "--routines",
        "--triggers",
        "--events",
        "--hex-blob",
        "--default-character-set=utf8mb4",
        "--result-file=$tempFile",
        $Database
    )
    & $dumpCommand.Source @dumpArguments
    if ($LASTEXITCODE -ne 0) {
        throw "mysqldump terminó con código $LASTEXITCODE. No se publicará la copia incompleta."
    }
    if (-not (Test-Path -LiteralPath $tempFile -PathType Leaf) -or
        (Get-Item -LiteralPath $tempFile).Length -eq 0) {
        throw "mysqldump no generó un archivo de respaldo válido."
    }

    Move-Item -LiteralPath $tempFile -Destination $finalFile
    $hash = (Get-FileHash -LiteralPath $finalFile -Algorithm SHA256).Hash.ToLowerInvariant()
    [System.IO.File]::WriteAllText("$finalFile.sha256", "$hash  $([System.IO.Path]::GetFileName($finalFile))`r`n")
    Write-Output "Respaldo creado y verificado: $finalFile"
    Write-Output "SHA-256: $hash"

    $cutoff = (Get-Date).AddDays(-$RetentionDays)
    Get-ChildItem -LiteralPath $OutputDirectory -File -Filter "$Database`_*.sql" |
        Where-Object { $_.Name -match "^$([regex]::Escape($Database))_\d{8}_\d{6}\.sql$" -and $_.LastWriteTime -lt $cutoff } |
        ForEach-Object {
            Remove-Item -LiteralPath $_.FullName
            Remove-Item -LiteralPath "$($_.FullName).sha256" -ErrorAction SilentlyContinue
            Write-Output "Copia expirada eliminada por retención: $($_.Name)"
        }
} finally {
    if (Test-Path -LiteralPath $tempFile) {
        Remove-Item -LiteralPath $tempFile -ErrorAction SilentlyContinue
    }
    if ($transcriptStarted) {
        Stop-Transcript | Out-Null
    }
}
