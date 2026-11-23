# ==============================================================================
# Script para compilar y empaquetar los 6 microservicios con Maven
# ==============================================================================
$env:JAVA_HOME = "C:\Users\panchito\.jdks\temurin-21.0.12.1"
$base = "c:\panchicosas\pcHardware-MICROSERVICES"

$services = @("usuario-service", "categoria-service", "producto-service", "detalle-service", "boleta-service", "gateway-api")

Write-Host "Compilando los 6 microservicios..." -ForegroundColor Cyan

foreach ($name in $services) {
    Write-Host "`n>>> Compilando $name..." -ForegroundColor Yellow
    Set-Location "$base\$name"
    .\mvnw.cmd test-compile -q
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[$name] COMPILACION EXITOSA" -ForegroundColor Green
    } else {
        Write-Host "[$name] ERROR EN COMPILACION" -ForegroundColor Red
    }
}

Set-Location $base
Write-Host "`nProceso de compilacion completado!" -ForegroundColor Cyan
