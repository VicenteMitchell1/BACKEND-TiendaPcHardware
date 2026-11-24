# ==============================================================================
# Inicia los 6 microservicios en segundo plano SIN Docker (perfil dev, base H2 en memoria).
# Requisitos: .\build-all.ps1 ejecutado antes y JDK 21. Los secretos se leen de .env
# (si no existe, se genera con setup-env.ps1). Logs en la carpeta logs\.
# ==============================================================================
$base = $PSScriptRoot
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { (Get-Command java -ErrorAction Stop).Source }

# 1. Secretos desde .env -> variables de entorno de este proceso (los procesos hijos las heredan)
$envFile = Join-Path $base ".env"
if (-not (Test-Path $envFile)) { & (Join-Path $base "setup-env.ps1") }
Get-Content $envFile | Where-Object { $_ -match '^\s*[A-Z_]+=' } | ForEach-Object {
    $nombre, $valor = $_ -split '=', 2
    Set-Item -Path "env:$($nombre.Trim())" -Value $valor.Trim()
}

$services = @(
    @{ Name = "usuario-service";   Port = 8081 },
    @{ Name = "categoria-service"; Port = 8082 },
    @{ Name = "producto-service";  Port = 8083 },
    @{ Name = "detalle-service";   Port = 8085 },
    @{ Name = "boleta-service";    Port = 8084 },
    @{ Name = "gateway-api";       Port = 8080 }
)

$logs = Join-Path $base "logs"
New-Item -ItemType Directory -Force $logs | Out-Null

Write-Host "Levantando arquitectura de microservicios PC Hardware (perfil dev / H2)..." -ForegroundColor Cyan
foreach ($s in $services) {
    $jar = Join-Path $base "$($s.Name)\target\$($s.Name)-1.0.0.jar"
    if (-not (Test-Path $jar)) {
        Write-Host "No existe $jar. Ejecute primero .\build-all.ps1" -ForegroundColor Red
        exit 1
    }
    Write-Host "Iniciando $($s.Name) en puerto $($s.Port)..." -ForegroundColor Yellow
    Start-Process -FilePath $java -ArgumentList "-jar", "`"$jar`"" -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logs "$($s.Name).log") -RedirectStandardError (Join-Path $logs "$($s.Name).err.log")
}

# 2. Espera activa hasta que cada puerto responda (máximo 120 s)
Write-Host "`nEsperando que los servicios respondan..." -ForegroundColor Yellow
$limite = (Get-Date).AddSeconds(120)
foreach ($s in $services) {
    while (-not (Get-NetTCPConnection -LocalPort $s.Port -State Listen -ErrorAction SilentlyContinue)) {
        if ((Get-Date) -gt $limite) { break }
        Start-Sleep -Milliseconds 500
    }
}

$estado = $services | ForEach-Object {
    [PSCustomObject]@{ Servicio = $_.Name; Puerto = $_.Port
        Activo = [bool](Get-NetTCPConnection -LocalPort $_.Port -State Listen -ErrorAction SilentlyContinue) }
}
$estado | Format-Table -AutoSize | Out-Host

if ($estado.Activo -contains $false) {
    Write-Host "Algún servicio no inició. Revise logs\<servicio>.log" -ForegroundColor Red
} else {
    Write-Host "Todos los microservicios están activos." -ForegroundColor Green
    Write-Host "API Gateway: http://localhost:8080   Swagger: http://localhost:8080/swagger-ui.html" -ForegroundColor Green
}
