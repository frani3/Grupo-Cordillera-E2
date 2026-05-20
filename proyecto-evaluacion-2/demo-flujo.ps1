# DEMO FLUJO COMPLETO - Grupo Cordillera E2
# Manda datos al ms1-pos cada N segundos y muestra como
# se ven en cada capa: ms1-pos -> orq -> bff -> frontend
#
# Uso: .\demo-flujo.ps1
# Ctrl+C para detener

$INTERVALO  = 4
$TOKEN      = "Bearer token-cordillera"
$MS1_URL    = "http://localhost:8081"
$ORQ_URL    = "http://localhost:8082"
$BFF_URL    = "http://localhost:8080"
$FRONT_URL  = "http://localhost:3000"

$sucursales  = @("Santiago Centro", "Providencia", "Las Condes", "Maipu", "Pudahuel", "San Miguel")
$metodos     = @("DEBITO", "CREDITO", "EFECTIVO")
$skus        = @("LAPTOP-01", "TABLET-02", "AURICULARES-01", "MONITOR-01", "TECLADO-01", "MOUSE-01")
$estrategias = @("batch", "stream", "cache")
$contador    = 1

Write-Host ""
Write-Host "  DEMO EN VIVO - datos fluyendo por toda la cadena" -ForegroundColor White -BackgroundColor DarkBlue
Write-Host "  Ctrl+C para detener" -ForegroundColor Gray
Write-Host ""

while ($true) {

    $id         = "TRX-" + $contador.ToString("D4")
    $sucursal   = $sucursales | Get-Random
    $metodo     = $metodos    | Get-Random
    $sku        = $skus       | Get-Random
    $estrategia = $estrategias | Get-Random
    $monto      = Get-Random -Minimum 1990 -Maximum 99990
    $cantidad   = Get-Random -Minimum 1 -Maximum 5
    $caja       = "CAJA-" + (Get-Random -Minimum 1 -Maximum 10).ToString("D2")
    $vendedor   = "VND-"  + (Get-Random -Minimum 1 -Maximum 15).ToString("D2")
    $fecha      = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
    $pu         = [math]::Round($monto / $cantidad, 2)

    $body = '{"trx_id":"' + $id + '","sucursal":"' + $sucursal + '","caja_id":"' + $caja + '","fecha_hora":"' + $fecha + '","monto_total":' + $monto + ',"metodo_pago":"' + $metodo + '","vendedor_id":"' + $vendedor + '","productos":[{"sku":"' + $sku + '","cantidad":' + $cantidad + ',"precio_unitario":' + $pu + '}]}'

    Write-Host ""
    Write-Host ("=" * 62) -ForegroundColor Cyan
    Write-Host "  TRANSACCION #$contador  -  $id" -ForegroundColor Cyan
    Write-Host ("=" * 62) -ForegroundColor Cyan

    # --- CAPA 1: ms1-pos ---
    Write-Host ""
    Write-Host "[PASO 1]" -ForegroundColor Yellow -NoNewline
    Write-Host " POST ms1-pos guardando transaccion..." -ForegroundColor Yellow

    try {
        $r1 = Invoke-RestMethod -Uri "$MS1_URL/api/pos/simulate-mq" -Method POST -ContentType "application/json" -Body $body
        Write-Host "[ms1-pos] " -ForegroundColor Green -NoNewline
        Write-Host $r1
    } catch {
        Write-Host "[ms1-pos ERROR] " -ForegroundColor Red -NoNewline
        Write-Host "$_"
    }

    Start-Sleep -Milliseconds 400

    Write-Host "[PASO 1b]" -ForegroundColor Yellow -NoNewline
    Write-Host " GET ms1-pos datos crudos en Singleton:" -ForegroundColor Yellow

    try {
        $rawData = Invoke-RestMethod -Uri "$MS1_URL/api/pos/data" -Method GET
        $total   = $rawData.Count
        $ultimo  = $rawData | Select-Object -Last 1
        $montoFmt = '$' + $ultimo.montoTotal
        Write-Host "[ms1-pos] " -ForegroundColor Green -NoNewline
        Write-Host "$total registros  ultimo: $($ultimo.transactionId)  $($ultimo.sucursal)  $montoFmt  $($ultimo.metodoPago)"
    } catch {
        Write-Host "[ms1-pos ERROR] " -ForegroundColor Red -NoNewline
        Write-Host "$_"
    }

    # --- CAPA 2: orq-service ---
    Write-Host ""
    Write-Host "[PASO 2]" -ForegroundColor Magenta -NoNewline
    Write-Host " GET orq-service estrategia $estrategia :" -ForegroundColor Magenta

    try {
        $r2 = Invoke-RestMethod -Uri "$ORQ_URL/api/data?id=$id&strategy=$estrategia" -Method GET
        Write-Host "[orq-service] " -ForegroundColor Green -NoNewline
        Write-Host $r2.data
    } catch {
        Write-Host "[orq ERROR] " -ForegroundColor Red -NoNewline
        Write-Host "$_"
    }

    # --- CAPA 3: bff-service ---
    Write-Host ""
    Write-Host "[PASO 3]" -ForegroundColor Blue -NoNewline
    Write-Host " GET bff-service Proxy valida Bearer y delega a orq:" -ForegroundColor Blue

    try {
        $headers = @{ Authorization = $TOKEN }
        $r3 = Invoke-RestMethod -Uri "$BFF_URL/api/proxy/data?id=$id" -Method GET -Headers $headers
        Write-Host "[bff-service] " -ForegroundColor Green -NoNewline
        Write-Host "$($r3.data)  [source=$($r3.source)]"
    } catch {
        Write-Host "[bff ERROR] " -ForegroundColor Red -NoNewline
        Write-Host "$_"
    }

    # --- CAPA 4: frontend ---
    Write-Host ""
    Write-Host "[PASO 4]" -ForegroundColor White -NoNewline
    Write-Host " GET frontend recorre toda la cadena Factory-BFF-orq-ms1:" -ForegroundColor White

    try {
        $headers = @{ Authorization = $TOKEN }
        $r4 = Invoke-RestMethod -Uri "$FRONT_URL/api/dashboard" -Method GET -Headers $headers
        Write-Host "[frontend] " -ForegroundColor Green -NoNewline
        Write-Host $r4.data
    } catch {
        Write-Host "[frontend ERROR] " -ForegroundColor Red -NoNewline
        Write-Host "$_"
    }

    Write-Host ""
    Write-Host "  Proximo en $INTERVALO segundos...  (Ctrl+C para detener)" -ForegroundColor DarkGray

    $contador++
    Start-Sleep -Seconds $INTERVALO
}
