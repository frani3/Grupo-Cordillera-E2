$url = "http://localhost:8081/api/pos/simulate-mq"
$sucursales = @("Santiago Centro","Providencia","Las Condes","Maipu","Pudahuel","Nunoa")
$cajas = @("CAJA-01","CAJA-02","CAJA-03","CAJA-04","CAJA-05")
$metodos = @("DEBITO","CREDITO","EFECTIVO","TRANSFERENCIA")
$vendedores = @("VND-01","VND-02","VND-03","VND-04","VND-05","VND-07")
$skus = @("LAPTOP-01","AURICULARES-01","TABLET-02","TECLADO-03","MOUSE-04","MONITOR-05","SILLA-06")
$precios = @{
    "LAPTOP-01"=29990.0; "AURICULARES-01"=5990.0; "TABLET-02"=12500.0
    "TECLADO-03"=8990.0; "MOUSE-04"=3490.0;       "MONITOR-05"=19990.0; "SILLA-06"=45000.0
}
$counter = 0

Write-Host "Simulador POS iniciado -> $url (Ctrl+C para detener)"

while ($true) {
    $counter++
    $sku = $skus | Get-Random
    $precio = $precios[$sku]
    $cant = Get-Random -Minimum 1 -Maximum 4
    $total = [math]::Round($precio * $cant, 2)
    $sucursal = $sucursales | Get-Random
    $trx = "TRX-POS-$("{0:D4}" -f $counter)-$(Get-Random -Minimum 1000 -Maximum 9999)"
    $fecha = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")

    $body = "{`"trx_id`":`"$trx`",`"sucursal`":`"$sucursal`",`"caja_id`":`"$($cajas | Get-Random)`",`"fecha_hora`":`"$fecha`",`"monto_total`":$total,`"metodo_pago`":`"$($metodos | Get-Random)`",`"vendedor_id`":`"$($vendedores | Get-Random)`",`"productos`":[{`"sku`":`"$sku`",`"cantidad`":$cant,`"precio_unitario`":$precio}]}"

    try {
        $resp = Invoke-RestMethod -Method POST -Uri $url -ContentType "application/json" -Body $body
        Write-Host "[$((Get-Date).ToString('HH:mm:ss'))] OK  | $trx | $sucursal | $sku x$cant | `$$total"
    } catch {
        Write-Host "[$((Get-Date).ToString('HH:mm:ss'))] ERR | $trx | $($_.Exception.Message)"
    }

    Start-Sleep -Seconds 4
}
