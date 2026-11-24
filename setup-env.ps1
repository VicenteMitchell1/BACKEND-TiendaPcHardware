# ==============================================================================
# Genera el archivo .env con secretos aleatorios (sólo si no existe).
# Uso: .\setup-env.ps1          (agregue -Force para regenerarlo)
# ==============================================================================
param([switch]$Force)

$envFile = Join-Path $PSScriptRoot ".env"
if ((Test-Path $envFile) -and -not $Force) {
    Write-Host ".env ya existe. Use -Force para regenerarlo (invalida sesiones y requiere recrear el volumen de MySQL)." -ForegroundColor Yellow
    return
}

function New-Secreto([int]$bytes) {
    $buffer = New-Object byte[] $bytes
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($buffer)
    return $buffer
}
function New-Clave([int]$bytes) {
    # Base64 URL-safe sin relleno: sólo letras, números, '-' y '_' (seguro para YAML y MySQL)
    return [Convert]::ToBase64String((New-Secreto $bytes)).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

$contenido = @"
# Generado por setup-env.ps1 el $(Get-Date -Format 'yyyy-MM-dd HH:mm'). NO subir a GitHub.
JWT_SECRET=$([Convert]::ToBase64String((New-Secreto 64)))
INTERNAL_API_KEY=$(New-Clave 32)
DB_PASSWORD=$(New-Clave 18)
MYSQL_ROOT_PASSWORD=$(New-Clave 18)
"@

Set-Content -Path $envFile -Value $contenido -Encoding ascii
Write-Host ".env generado en $envFile" -ForegroundColor Green
