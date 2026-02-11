Write-Host "=== Kubernetes Load Balancing Test ===" -ForegroundColor Cyan
Write-Host ""

# Configuration
$REQUEST_COUNT = 100  # As per task requirements

# Get pod names
Write-Host "=== Pods ===" -ForegroundColor Yellow
kubectl get pods -l app=nginx-app -o wide
$pods = kubectl get pods -l app=nginx-app -o jsonpath='{.items[*].metadata.name}'
$podArray = $pods -split ' '
Write-Host "Count: $($podArray.Count)" -ForegroundColor Green
Write-Host ""

if ($podArray.Count -ne 3) {
    Write-Host "ERROR: Need 3 pods!" -ForegroundColor Red
    exit
}

# Get service IPs
$NODEPORT_IP = kubectl get svc nginx-nodeport -o jsonpath='{.spec.clusterIP}'
$INGRESS_IP = kubectl get svc nginx-clusterip -o jsonpath='{.spec.clusterIP}'

Write-Host "NodePort Service IP: $NODEPORT_IP" -ForegroundColor Green
Write-Host "Ingress Backend Service IP: $INGRESS_IP" -ForegroundColor Green
Write-Host ""

# Check Ingress
Write-Host "=== Ingress Status ===" -ForegroundColor Yellow
kubectl get ingress nginx-ingress
Write-Host ""

# TEST 1: NodePort
Write-Host "=== TEST 1: NodePort Service ===" -ForegroundColor Cyan
Write-Host "Sending $REQUEST_COUNT requests via NodePort service..."

# Get current log line counts BEFORE test
$beforeCountsNP = @{}
foreach ($pod in $podArray) {
    $count = (kubectl logs $pod 2>$null | Measure-Object -Line).Lines
    $beforeCountsNP[$pod] = $count
}

# Send requests from inside cluster
for ($i = 1; $i -le $REQUEST_COUNT; $i++) {
    kubectl exec $podArray[0] -- curl -s "http://${NODEPORT_IP}:80" > $null 2>&1
    
    if ($i % 20 -eq 0) {
        Write-Host "  Progress: $i/$REQUEST_COUNT"
    }
}

Start-Sleep -Seconds 2

# Get new log counts (difference)
$npResults = @{}
foreach ($pod in $podArray) {
    $afterCount = (kubectl logs $pod 2>$null | Measure-Object -Line).Lines
    $newRequests = $afterCount - $beforeCountsNP[$pod]
    $npResults[$pod] = $newRequests
}

Write-Host ""
Write-Host "Results NodePort:" -ForegroundColor Yellow
Write-Host "=========================================="

$npTotal = ($npResults.Values | Measure-Object -Sum).Sum

foreach ($pod in ($podArray | Sort-Object)) {
    $count = $npResults[$pod]
    if ($npTotal -gt 0) {
        $percent = [math]::Round(($count / $npTotal) * 100, 1)
    } else {
        $percent = 0
    }
    Write-Host "$pod : $count requests ($percent%)"
}
Write-Host "=========================================="
Write-Host "Total: $npTotal requests" -ForegroundColor Cyan

# Calculate stats
$npStdDev = 0
if ($npTotal -gt 0) {
    $npValues = @($npResults.Values)
    $npMean = ($npValues | Measure-Object -Average).Average
    $variance = ($npValues | ForEach-Object { [math]::Pow($_ - $npMean, 2) } | Measure-Object -Sum).Sum / $npValues.Count
    $npStdDev = [math]::Sqrt($variance)
    Write-Host "Mean: $([math]::Round($npMean, 2)), StdDev: $([math]::Round($npStdDev, 2))" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== TEST 2: LoadBalancer via Ingress ===" -ForegroundColor Cyan
Write-Host "Sending $REQUEST_COUNT requests via Ingress (Nginx Ingress Controller)..."

# Get current log line counts BEFORE test
$beforeCountsLB = @{}
foreach ($pod in $podArray) {
    $count = (kubectl logs $pod 2>$null | Measure-Object -Line).Lines
    $beforeCountsLB[$pod] = $count
}

# Send requests through Ingress backend service
for ($i = 1; $i -le $REQUEST_COUNT; $i++) {
    kubectl exec $podArray[0] -- curl -s -H "Host: nginx-app.local" "http://${INGRESS_IP}:80" > $null 2>&1
    
    if ($i % 20 -eq 0) {
        Write-Host "  Progress: $i/$REQUEST_COUNT"
    }
}

Start-Sleep -Seconds 2

# Get new log counts (difference)
$lbResults = @{}
foreach ($pod in $podArray) {
    $afterCount = (kubectl logs $pod 2>$null | Measure-Object -Line).Lines
    $newRequests = $afterCount - $beforeCountsLB[$pod]
    $lbResults[$pod] = $newRequests
}

Write-Host ""
Write-Host "Results LoadBalancer (via Ingress):" -ForegroundColor Yellow
Write-Host "=========================================="

$lbTotal = ($lbResults.Values | Measure-Object -Sum).Sum

foreach ($pod in ($podArray | Sort-Object)) {
    $count = $lbResults[$pod]
    if ($lbTotal -gt 0) {
        $percent = [math]::Round(($count / $lbTotal) * 100, 1)
    } else {
        $percent = 0
    }
    Write-Host "$pod : $count requests ($percent%)"
}
Write-Host "=========================================="
Write-Host "Total: $lbTotal requests" -ForegroundColor Cyan

# Calculate stats
$lbStdDev = 0
if ($lbTotal -gt 0) {
    $lbValues = @($lbResults.Values)
    $lbMean = ($lbValues | Measure-Object -Average).Average
    $variance = ($lbValues | ForEach-Object { [math]::Pow($_ - $lbMean, 2) } | Measure-Object -Sum).Sum / $lbValues.Count
    $lbStdDev = [math]::Sqrt($variance)
    Write-Host "Mean: $([math]::Round($lbMean, 2)), StdDev: $([math]::Round($lbStdDev, 2))" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== Comparison ===" -ForegroundColor Cyan
Write-Host "=================================================================="
Write-Host "Pod                            NodePort    LoadBalancer(Ingress)"
Write-Host "=================================================================="
foreach ($pod in ($podArray | Sort-Object)) {
    $np = if ($npResults[$pod]) { $npResults[$pod] } else { 0 }
    $lb = if ($lbResults[$pod]) { $lbResults[$pod] } else { 0 }
    Write-Host ("{0,-30} {1,10} {2,13}" -f $pod, $np, $lb)
}
Write-Host "=================================================================="

Write-Host ""
Write-Host "=== Detailed Analysis ===" -ForegroundColor Cyan
Write-Host "NodePort Service:"
Write-Host "  Total: $npTotal requests"
Write-Host "  StdDev: $([math]::Round($npStdDev, 2))"
if ($npTotal -gt 0) {
    Write-Host "  Variation: $([math]::Round($npStdDev / $npMean * 100, 2))%"
}
Write-Host ""
Write-Host "LoadBalancer via Ingress (Nginx Ingress Controller):"
Write-Host "  Total: $lbTotal requests"
Write-Host "  StdDev: $([math]::Round($lbStdDev, 2))"
if ($lbTotal -gt 0) {
    Write-Host "  Variation: $([math]::Round($lbStdDev / $lbMean * 100, 2))%"
}

Write-Host ""
Write-Host "=== Conclusion ===" -ForegroundColor Green
if ($lbTotal -gt 0 -and $npTotal -gt 0) {
    $npVariation = $npStdDev / $npMean * 100
    $lbVariation = $lbStdDev / $lbMean * 100
    
    if ($lbVariation -lt $npVariation) {
        Write-Host "LoadBalancer (Ingress) shows MORE UNIFORM distribution!" -ForegroundColor Green
    } elseif ($npVariation -lt $lbVariation) {
        Write-Host "NodePort shows MORE UNIFORM distribution!" -ForegroundColor Green
    } else {
        Write-Host "Both services show similar distribution" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=== Testing Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "Note: Ingress uses Nginx Ingress Controller with LoadBalancer type" -ForegroundColor Gray
