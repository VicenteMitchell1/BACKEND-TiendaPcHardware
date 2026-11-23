# ==============================================================================
# Script para iniciar los 6 microservicios en segundo plano
# ==============================================================================
$java = "C:\Users\panchito\.jdks\temurin-21.0.12.1\bin\java.exe"
$base = "c:\panchicosas\pcHardware-MICROSERVICES"

$services = @(
    @{ Name = "usuario-service";   Port = 8081; Jar = "$base\usuario-service\target\usuario-service-1.0.0.jar" },
    @{ Name = "categoria-service"; Port = 8082; Jar = "$base\categoria-service\target\categoria-service-1.0.0.jar" },
    @{ Name = "producto-service";  Port = 8083; Jar = "$base\producto-service\target\producto-service-1.0.0.jar" },
    @{ Name = "detalle-service";   Port = 8085; Jar = "$base\detalle-service\target\detalle-service-1.0.0.jar" },
    @{ Name = "boleta-service";    Port = 8084; Jar = "$base\boleta-service\target\boleta-service-1.0.0.jar" },
    @{ Name = "gateway-api";       Port = 8080; Jar = "$base\gateway-api\target\gateway-api-1.0.0.jar" }
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Levantando Arquitectura de Microservicios PC Hardware   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

foreach ($s in $services) {
    Write-Host "Iniciando $($s.Name) en puerto $($s.Port)..." -ForegroundColor Yellow
    Start-Process -FilePath $java -ArgumentList "-jar", "`"$($s.Jar)`"" -WindowStyle Hidden
}

Write-Host "`nEsperando arranque de servicios..." -ForegroundColor Yellow
Start-Sleep -Seconds 6

Write-Host "`nEstado de puertos de microservicios:" -ForegroundColor Cyan
8080..8085 | ForEach-Object { [PSCustomObject]@{ Port = $_; Activo = (Get-NetTCPConnection -LocalPort $_ -ErrorAction SilentlyContinue | Measure-Object).Count -gt 0 } } | Format-Table -AutoSize

Write-Host "`nTodos los microservicios estan activos!" -ForegroundColor Green
Write-Host "API Gateway activo en: http://localhost:8080" -ForegroundColor Green
Write-Host "Swagger UI disponible en: http://localhost:8080/swagger-ui.html" -ForegroundColor Green
