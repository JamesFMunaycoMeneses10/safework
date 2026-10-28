[CmdletBinding()]
param(
    [string]$ComposeDirectory = '',
    [string]$OutputDirectory = (Join-Path $env:LOCALAPPDATA 'SafeWork\Backups\Docker'),
    [ValidateRange(1,3650)][int]$RetentionDays = 30
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($ComposeDirectory)) { $ComposeDirectory = Split-Path -Parent $PSScriptRoot }
$dockerCommand = Get-Command docker -ErrorAction SilentlyContinue
if (-not $dockerCommand) { $dockerPath = Join-Path $env:ProgramFiles 'Docker\Docker\resources\bin\docker.exe' }
else { $dockerPath = $dockerCommand.Source }
if (-not (Test-Path -LiteralPath $dockerPath)) { throw 'No se encuentra Docker Desktop.' }
$ComposeDirectory = [IO.Path]::GetFullPath($ComposeDirectory)
if (-not (Test-Path -LiteralPath (Join-Path $ComposeDirectory 'compose.yaml'))) { throw 'No existe compose.yaml.' }
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
if ($env:OS -eq 'Windows_NT') {
    $acl = Get-Acl -LiteralPath $OutputDirectory
    $currentIdentity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $allowedSids = @($currentIdentity.User.Value,'S-1-5-18')
    $extraRules = @($acl.Access | Where-Object {
        $_.AccessControlType -eq 'Allow' -and $_.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin $allowedSids
    })
    # No reescribir una DACL que ya es privada: Set-Acl puede exigir privilegios de SACL innecesarios.
    if (-not $acl.AreAccessRulesProtected -or $extraRules.Count -gt 0) {
        $privateAcl = [Security.AccessControl.DirectorySecurity]::new()
        $privateAcl.SetAccessRuleProtection($true,$false)
        foreach ($identity in @($currentIdentity.Name,'SYSTEM')) {
            $privateAcl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($identity,'FullControl','ContainerInherit,ObjectInherit','None','Allow'))
        }
        if ($PSVersionTable.PSEdition -eq 'Desktop') {
            ([IO.DirectoryInfo]::new($OutputDirectory)).SetAccessControl($privateAcl)
        } else {
            [IO.FileSystemAclExtensions]::SetAccessControl([IO.DirectoryInfo]::new($OutputDirectory),$privateAcl)
        }
    }
}
$mutex = [Threading.Mutex]::new($false,'Local\SafeWorkDockerBackup')
$locked = $false
$suffix = [Guid]::NewGuid().ToString('N').Substring(0,8)
$name = 'safework_piloto_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '_' + $suffix
$partial = Join-Path $OutputDirectory ($name + '.partial.sql')
$final = Join-Path $OutputDirectory ($name + '.sql')
$remote = '/tmp/' + $name + '.sql'
try {
    $locked=$mutex.WaitOne(0)
    if (-not $locked) { throw 'Ya hay un respaldo en ejecucion.' }
    Push-Location -LiteralPath $ComposeDirectory
    try {
        $dump = 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysqldump --user="$MYSQL_USER" --single-transaction --hex-blob --no-tablespaces --set-gtid-purged=OFF --default-character-set=utf8mb4 --result-file=' + $remote + ' "$MYSQL_DATABASE"'
        & $dockerPath compose exec -T mysql sh -c $dump
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar el respaldo MySQL.' }
        & $dockerPath compose cp ('mysql:'+$remote) $partial
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $partial) -or (Get-Item -LiteralPath $partial).Length -lt 100) { throw 'Respaldo incompleto.' }
        Move-Item -LiteralPath $partial -Destination $final
        $hash = (Get-FileHash -LiteralPath $final -Algorithm SHA256).Hash
        [IO.File]::WriteAllText((Join-Path $OutputDirectory ($name+'.sha256')), $hash+'  '+$name+'.sql', [Text.UTF8Encoding]::new($false))
    } finally {
        & $dockerPath compose exec -T mysql rm -f $remote 2>$null | Out-Null
        Pop-Location
    }
    $limit = (Get-Date).AddDays(-$RetentionDays)
    $rootPrefix = $OutputDirectory.TrimEnd('\') + '\'
    foreach ($file in Get-ChildItem -LiteralPath $OutputDirectory -File) {
        if ($file.Name -match '^safework_piloto_\d{8}_\d{6}_[a-f0-9]{8}\.(sql|sha256)$' -and $file.LastWriteTime -lt $limit) {
            $absolute = [IO.Path]::GetFullPath($file.FullName)
            if (-not $absolute.StartsWith($rootPrefix,[StringComparison]::OrdinalIgnoreCase)) { throw 'Ruta de retencion fuera de la carpeta de respaldos.' }
            Remove-Item -LiteralPath $absolute
        }
    }
    Write-Output ('Respaldo completo: ' + $final)
    Write-Output ('SHA256: ' + $hash)
} finally {
    if ($locked) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
