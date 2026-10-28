[CmdletBinding()]
param(
    [string]$Url='http://127.0.0.1:8081/actuator/health',
    [string]$OutputDirectory=(Join-Path $env:LOCALAPPDATA 'SafeWork\Monitor'),
    [ValidateRange(100,60000)][int]$SlowMilliseconds=2000
)
$ErrorActionPreference='Stop'
$uri=[Uri]$Url
if ($uri.UserInfo -or $uri.Query -or $uri.Fragment -or $uri.AbsolutePath -ne '/actuator/health' -or $uri.Scheme -notin @('http','https')) {throw 'Utiliza la ruta de salud sin credenciales ni parametros.'}
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$watch=[Diagnostics.Stopwatch]::StartNew()
$status='DOWN'
try {
    $response=Invoke-RestMethod -Uri $uri -TimeoutSec 15
    if ($response.status -eq 'UP') {$status='UP'}
} catch { $status='DOWN' }
$watch.Stop()
$result=[ordered]@{fecha=(Get-Date).ToString('o');estado=$status;milisegundos=$watch.ElapsedMilliseconds;alerta=($status -ne 'UP' -or $watch.ElapsedMilliseconds -gt $SlowMilliseconds)}
$json=$result | ConvertTo-Json -Compress
Add-Content -LiteralPath (Join-Path $OutputDirectory ('health-'+(Get-Date -Format 'yyyy-MM-dd')+'.jsonl')) -Value $json -Encoding UTF8
Write-Output $json
if ($result.alerta) {exit 1}
