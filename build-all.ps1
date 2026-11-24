# ==============================================================================
# Compila, ejecuta los tests y empaqueta los 6 microservicios (genera target\*.jar).
# Uso: .\build-all.ps1            (con tests y reporte de cobertura JaCoCo)
#      .\build-all.ps1 -SkipTests (más rápido, sin tests)
# Requiere JDK 21 (JAVA_HOME o java en el PATH). No requiere Maven: usa mvnw.cmd.
# ==============================================================================
param([switch]$SkipTests)

$base = $PSScriptRoot
$services = @("usuario-service", "categoria-service", "producto-service", "detalle-service", "boleta-service", "gateway-api")
$goal = if ($SkipTests) { "package -DskipTests" } else { "package" }
$fallidos = @()

if (-not $env:JAVA_HOME -and -not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "No se encontró Java. Instale JDK 21 y configure JAVA_HOME." -ForegroundColor Red
    exit 1
}

Write-Host "Compilando los 6 microservicios ($goal)..." -ForegroundColor Cyan
foreach ($name in $services) {
    Write-Host "`n>>> $name" -ForegroundColor Yellow
    Push-Location (Join-Path $base $name)
    cmd /c ".\mvnw.cmd -B -q clean $goal"
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[$name] OK -> target\$name-1.0.0.jar" -ForegroundColor Green
    } else {
        Write-Host "[$name] ERROR (revise la salida anterior)" -ForegroundColor Red
        $fallidos += $name
    }
    Pop-Location
}

if ($fallidos.Count -gt 0) {
    Write-Host "`nFallaron: $($fallidos -join ', ')" -ForegroundColor Red
    exit 1
}
Write-Host "`nCompilación completa. Reportes de cobertura: <servicio>\target\site\jacoco\index.html" -ForegroundColor Cyan
