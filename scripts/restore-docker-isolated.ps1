[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$BackupFile)
$ErrorActionPreference='Stop'
$dockerCommand=Get-Command docker -ErrorAction SilentlyContinue
$dockerPath=if ($dockerCommand) {$dockerCommand.Source} else {Join-Path $env:ProgramFiles 'Docker\Docker\resources\bin\docker.exe'}
$BackupFile=[IO.Path]::GetFullPath($BackupFile)
if (-not (Test-Path -LiteralPath $BackupFile -PathType Leaf) -or [IO.Path]::GetExtension($BackupFile) -ne '.sql') { throw 'Selecciona un respaldo SQL.' }
$hashFile=[IO.Path]::ChangeExtension($BackupFile,'.sha256')
if (-not (Test-Path -LiteralPath $hashFile)) { throw 'Falta el archivo SHA256 del respaldo.' }
$expected=([IO.File]::ReadAllText($hashFile).Split(' ')[0]).Trim()
if ((Get-FileHash -LiteralPath $BackupFile -Algorithm SHA256).Hash -ne $expected) { throw 'La integridad SHA256 no coincide.' }
$name='safework-restauracion-'+[Guid]::NewGuid().ToString('N').Substring(0,12)
$bytes=New-Object byte[] 32
$rng=[Security.Cryptography.RandomNumberGenerator]::Create();$rng.GetBytes($bytes);$rng.Dispose()
$previous=$env:MYSQL_ROOT_PASSWORD
$env:MYSQL_ROOT_PASSWORD=[BitConverter]::ToString($bytes).Replace('-','').ToLowerInvariant()
$created=$false
try {
    # Contenedor aislado, sin puertos de host ni conexion a las bases existentes.
    & $dockerPath run --detach --name $name --label safework.purpose=restore-check --network none --memory 768m --env MYSQL_ROOT_PASSWORD --env MYSQL_DATABASE=restauracion mysql:8.0 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar el contenedor de restauracion.' }
    $created=$true;$ready=$false
    for ($i=0;$i -lt 60;$i++) {
        & $dockerPath exec $name sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -D restauracion -e "SELECT 1" >/dev/null 2>&1'
        if ($LASTEXITCODE -eq 0) {$ready=$true;break}
        Start-Sleep -Seconds 2
    }
    if (-not $ready) { throw 'La base aislada no inicio.' }
    & $dockerPath cp $BackupFile ($name+':/tmp/restaurar.sql')
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo copiar el respaldo.' }
    & $dockerPath exec $name sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -D restauracion < /tmp/restaurar.sql'
    if ($LASTEXITCODE -ne 0) { throw 'La restauracion fallo.' }
    & $dockerPath exec $name sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -D restauracion -e "SELECT COUNT(*) AS usuarios_recuperados FROM usuarios; SELECT COUNT(*) AS archivos_recuperados FROM archivos_adjuntos; SELECT version,success FROM flyway_schema_history ORDER BY installed_rank;"'
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la comprobacion posterior.' }
    Write-Output 'Restauracion SQL aislada completada. Comprobar tambien login, permisos y descargas al ensayar una liberacion.'
} finally {
    $env:MYSQL_ROOT_PASSWORD=$previous
    if ($created) {
        $label=& $dockerPath inspect --format '{{ index .Config.Labels "safework.purpose" }}' $name
        if ($LASTEXITCODE -eq 0 -and $label -eq 'restore-check' -and $name -match '^safework-restauracion-[a-f0-9]{12}$') {
            & $dockerPath rm --force --volumes $name | Out-Null
        }
    }
}
