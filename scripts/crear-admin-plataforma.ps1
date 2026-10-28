[CmdletBinding()]
param([ValidateRange(1024,65535)][int]$Port = 8080)
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$jar = Get-ChildItem -LiteralPath (Join-Path $project 'target') -Filter 'safework-*.jar' -File |
    Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
if (-not $jar) { throw 'Primero ejecuta .\mvnw.cmd clean verify desde la carpeta del proyecto.' }
$localJdk = Get-ChildItem -LiteralPath (Join-Path $env:LOCALAPPDATA 'Programs\Eclipse Adoptium') -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending | Select-Object -First 1
$java = if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    Join-Path $env:JAVA_HOME 'bin\java.exe'
} elseif ($localJdk -and (Test-Path -LiteralPath (Join-Path $localJdk.FullName 'bin\java.exe'))) {
    Join-Path $localJdk.FullName 'bin\java.exe'
} else { (Get-Command java -ErrorAction Stop).Source }
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
    throw "El puerto $Port está ocupado. Detén SafeWork antes de crear la cuenta."
}
Write-Host 'Creación única de una cuenta NUEVA de plataforma. No convierte ni reemplaza tus cuentas actuales.'
Write-Host 'No escribas estos datos en el chat ni guardes la contraseña en documentos.'
$values = @{
    SAFEWORK_PLATFORM_USERNAME = (Read-Host 'Nuevo usuario de plataforma')
    SAFEWORK_PLATFORM_NOMBRE = (Read-Host 'Nombre')
    SAFEWORK_PLATFORM_APELLIDO = (Read-Host 'Apellido')
    SAFEWORK_PLATFORM_CORREO = (Read-Host 'Correo')
}
$secure = Read-Host 'Contraseña nueva (mínimo 12 caracteres; no se muestra)' -AsSecureString
$values.SAFEWORK_PLATFORM_PASSWORD = [System.Net.NetworkCredential]::new('', $secure).Password
$previous = @{}
foreach ($name in $values.Keys) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    foreach ($name in $values.Keys) { [Environment]::SetEnvironmentVariable($name, $values[$name], 'Process') }
    Write-Host 'Se iniciará SafeWork. Si el inicio termina correctamente, entra con la nueva cuenta.'
    Write-Host 'Después pulsa Ctrl+C aquí y vuelve a iniciar SafeWork normalmente desde tu IDE.'
    & $java '-jar' $jar.FullName "--server.port=$Port" '--safework.plataforma.crear-administrador=true' '--spring.devtools.restart.enabled=false'
    if ($LASTEXITCODE -ne 0) { throw 'El arranque no terminó correctamente. Revisa el mensaje sin compartir credenciales.' }
} finally {
    foreach ($name in $values.Keys) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
    $values.Clear()
    $secure.Dispose()
}
