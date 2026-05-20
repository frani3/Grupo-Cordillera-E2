# ============================================================
#  DEMO FLUJO COMPLETO — Grupo Cordillera E2
#  Manda datos al ms1-pos cada N segundos y muestra como
#  se ven en cada capa: ms1-pos → orq → bff → frontend
#
#  Uso: .\demo-flujo.ps1
#  Ctrl+C para detener
# ============================================================

$INTERVALO   = 4       # segundos entre cada transaccion
$TOKEN       = "Bearer token-cordillera"
$MS1_URL     = "http://localhost:8081"
$ORQ_URL     = "http://localhost:8082"
$BFF_URL     = "http://localhost:8080"
$FRONT_URL   = "http://localhost:3000"

$sucursales  = @("Santiago Centro", "Providencia", "Las Condes", "Maipu", "Pudahuel", "San Miguel")
$metodos     = @("DEBITO", "CREDITO", "EFECTIVO")
$skus        = @("LAPTOP-01", "TABLET-02", "AURICULARES-01", "MONITOR-01", "TECLADO-01", "MOUSE-01")
$estrategias = @("batch", "stream", "cache")

$contador = 1

function Separador($texto) {
    $linea = "=" * 60
    Write-Host ""
    Write-Host $linea -ForegroundColor Cyan
    Write-Host "  $texto" -ForegroundColor Cyan
    Write-Host $linea -ForegroundColor Cyan
}

function Tag($label, $color) {
    Write-Host "[$label]" -ForegroundColor $color -NoNewline
    Write-Host " " -NoNewline
}

function Get-JsonField($json, $campo) {
    try {
        return ($json | ConvertFrom-Json).$campo
    } catch {
        return $json
    }
}

Write-Host ""
Write-Host "  DEMO EN VIVO — datos fluyendo por toda la cadena" -ForegroundColor White -BackgroundColor DarkBlue
Write-Host "  Ctrl+C para detener" -ForegroundColor Gray
Write-Host ""

while ($true) {

    # ── Generar transaccion aleatoria ─────────────────────────────
    $id        = "TRX-{0:D4}" -f $contador
    $sucursal  = $sucursales | Get-Random
    $metodo    = $metodos    | Get-Random
    $sku       = $skus       | Get-Random
    $monto     = (Get-Random -Minimum 1990 -Maximum 99990)
    $cantidad  = (Get-Random -Minimum 1 -Maximum 5)
    $estrategia = $estrategias | Get-Random
    $fecha     = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")

    $body = @{
        trx_id     = $id
        sucursal   = $sucursal
        caja_id    = "CAJA-{0:D2}" -f (Get-Random -Minimum 1 -Maximum 10)
        fecha_hora = $fecha
        monto_total = $monto
        metodo_pago = $metodo
        vendedor_id = "VND-{0:D2}" -f (Get-Random -Minimum 1 -Maximum 15)
        productos  = @(
            @{ sku = $sku; cantidad = $cantidad; precio_unitario = [math]::Round($monto / $cantidad, 2) }
        )
    } | ConvertTo-Json -Depth 3

    Separador "TRANSACCION #$contador — $id"

    # ── CAPA 1: ms1-pos ──────────────────────────────────────────
    Tag "PASO 1" Yellow
    Write-Host "POST ms1-pos → guardando transaccion..." -ForegroundColor Yellow

    try {
        $r1 = Invoke-RestMethod -Uri "$MS1_URL/api/pos/simulate-mq" `
              -Method POST -ContentType "application/json" -Body $body
        Tag "ms1-pos" Green
        Write-Host "$r1"
    } catch {
        Tag "ms1-pos" Red; Write-Host "ERROR: $_"
    }

    Start-Sleep -Milliseconds 300

    Tag "PASO 1b" Yellow
    Write-Host "GET ms1-pos → datos crudos en el Singleton:" -ForegroundColor Yellow

    try {
        $rawData = Invoke-RestMethod -Uri "$MS1_URL/api/pos/data" -Method GET
        $total   = $rawData.Count
        $ultimo  = $rawData | Select-Object -Last 1
        Tag "ms1-pos" Green
        Write-Host "$total registros en memoria | ultimo: $($ultimo.transactionId) | $($ultimo.sucursal) | `$$($ultimo.montoTotal) | $($ultimo.metodoPago)"
    } catch {
        Tag "ms1-pos" Red; Write-Host "ERROR: $_"
    }

    # ── CAPA 2: orq-service ───────────────────────────────────────
    Write-Host ""
    Tag "PASO 2" Magenta
    Write-Host "GET orq-service → aplica Strategy '$estrategia':" -ForegroundColor Magenta

    try {
        $r2 = Invoke-RestMethod -Uri "$ORQ_URL/api/data?id=$id&strategy=$estrategia" -Method GET
        Tag "orq-service" Green
        Write-Host "$($r2.data)"
    } catch {
        Tag "orq-service" Red; Write-Host "ERROR: $_"
    }

    # ── CAPA 3: bff-service ───────────────────────────────────────
    Write-Host ""
    Tag "PASO 3" Blue
    Write-Host "GET bff-service → Proxy valida Bearer y delega a orq:" -ForegroundColor Blue

    try {
        $headers = @{ Authorization = $TOKEN }
        $r3 = Invoke-RestMethod -Uri "$BFF_URL/api/proxy/data?id=$id" `
              -Method GET -Headers $headers
        Tag "bff-service" Green
        Write-Host "$($r3.data) [source=$($r3.source)]"
    } catch {
        Tag "bff-service" Red; Write-Host "ERROR: $_"
    }

    # ── CAPA 4: frontend-app ──────────────────────────────────────
    Write-Host ""
    Tag "PASO 4" White
    Write-Host "GET frontend → recorre toda la cadena (Factory→BFF→orq→ms1):" -ForegroundColor White

    try {
        $headers = @{ Authorization = $TOKEN }
        $r4 = Invoke-RestMethod -Uri "$FRONT_URL/api/dashboard" `
              -Method GET -Headers $headers
        Tag "frontend" Green
        Write-Host "$($r4.data)"
    } catch {
        Tag "frontend" Red; Write-Host "ERROR: $_"
    }

    # ── Resumen de la vuelta ───────────────────────────────────────
    Write-Host ""
    Write-Host "  Proximo en $INTERVALO segundos...  (Ctrl+C para detener)" -ForegroundColor DarkGray

    $contador++
    Start-Sleep -Seconds $INTERVALO
}
