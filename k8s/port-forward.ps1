# Доступ до BookBarter у Kubernetes Docker Desktop з браузера.
# Kubernetes Docker Desktop (kind) не публікує NodePort-и на localhost Windows,
# тому сервіси прокидаються через kubectl port-forward.
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

$procs = foreach ($f in $forwards) {
    Start-Process kubectl -ArgumentList "-n", $ns, "port-forward", "svc/$($f.svc)", $f.ports -NoNewWindow -PassThru
}

Write-Host ""
Write-Host "API Gateway: http://localhost:8000/actuator/health"
Write-Host "Eureka:      http://localhost:8761"
Write-Host "Prometheus:  http://localhost:9091"
Write-Host "Ctrl+C - stop"

try {
    Wait-Process -Id $procs.Id
} finally {
    $procs | Stop-Process -ErrorAction SilentlyContinue
}
