# ==============================================================================
# Detiene los microservicios iniciados con start-all.ps1 (puertos 8080-8085).
# Sólo termina procesos Java: no afecta otros programas que usen esos puertos.
# ==============================================================================
$ports = 8080..8085

Write-Host "Deteniendo microservicios en puertos 8080-8085..." -ForegroundColor Yellow
foreach ($port in $ports) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($procId in ($conns.OwningProcess | Sort-Object -Unique)) {
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        if ($proc -and $proc.ProcessName -match '^javaw?$') {
            Write-Host "Terminando $($proc.ProcessName) en puerto $port (PID $procId)" -ForegroundColor Red
            Stop-Process -Id $procId -Force
        } elseif ($proc) {
            Write-Host "Puerto $port usado por '$($proc.ProcessName)' (no es Java): no se detiene." -ForegroundColor DarkYellow
        }
    }
}
Write-Host "Listo." -ForegroundColor Green
