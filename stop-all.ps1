# ==============================================================================
# Script para detener los 6 microservicios cerrando los puertos 8080-8085
# ==============================================================================
$ports = @(8080, 8081, 8082, 8083, 8084, 8085)

Write-Host "Deteniendo microservicios en puertos 8080-8085..." -ForegroundColor Yellow

foreach ($port in $ports) {
    $conns = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($conns) {
        foreach ($c in $conns) {
            $procId = $c.OwningProcess
            if ($procId -gt 0) {
                Write-Host "Terminando proceso en puerto $port (PID: $procId)..." -ForegroundColor Red
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

Write-Host "Todos los microservicios han sido detenidos con exito." -ForegroundColor Green
