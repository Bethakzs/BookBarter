# Доступ до BookBarter у Kubernetes Docker Desktop з браузера.
# Kubernetes Docker Desktop (kind) не публікує NodePort-и на localhost Windows,
# тому сервіси прокидаються через kubectl port-forward. Після кожного деплою з Jenkins
# поди замінюються — скрипт автоматично перепідключається.
#
#   powershell -ExecutionPolicy Bypass -File k8s\port-forward.ps1
#
#   API Gateway:  http://localhost:8000/actuator/health
#   Eureka:       http://localhost:8761
#   Prometheus:   http://localhost:9091  (Prometheus у кластері; CI-Prometheus — http://localhost:9090)

$ns = "bookbarter"
$forwards = @(
    @{ svc = "api-gateway"; ports = "8000:8000" },
    @{ svc = "eureka";      ports = "8761:8761" },
    @{ svc = "prometheus";  ports = "9091:9090" }
)

$jobs = foreach ($f in $forwards) {
    Start-Job -ArgumentList $ns, $f.svc, $f.ports -ScriptBlock {
        param($ns, $svc, $ports)
        while ($true) {
            kubectl -n $ns port-forward "svc/$svc" $ports 2>&1
            Write-Output "[$svc] reconnecting..."
            Start-Sleep -Seconds 3
        }
    }
}

Write-Host ""
Write-Host "API Gateway: http://localhost:8000/actuator/health"
Write-Host "Eureka:      http://localhost:8761"
Write-Host "Prometheus:  http://localhost:9091"
Write-Host "Ctrl+C - stop"

try {
    while ($true) {
        $jobs | Receive-Job
        Start-Sleep -Seconds 2
    }
} finally {
    $jobs | Stop-Job
    $jobs | Remove-Job -Force
}
