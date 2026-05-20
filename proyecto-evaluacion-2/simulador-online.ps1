$url = "http://localhost:8083/api/online/venta"
$plataformas = @("web","app","marketplace")
$metodos = @("tarjeta","transferencia","paypal","mercadopago")
$emails = @("juan@mail.com","maria@mail.com","pedro@mail.com","ana@mail.com","luis@mail.com")
$skus = @("LAPTOP-001","MOUSE-002","TECLADO-003","AURIF-004","CAMARA-005","HUB-006")
$precios = @{
    "LAPTOP-001"=799.99; "MOUSE-002"=29.99; "TECLADO-003"=89.99
    "AURIF-004"=59.99;   "CAMARA-005"=49.99; "HUB-006"=34.99
}
$counter = 0

Write-Host "Simulador ONLINE iniciado -> $url (Ctrl+C para detener)"

while ($true) {
    $counter++
    $sku = $skus | Get-Random
    $precio = $precios[$sku]
    $cant = Get-Random -Minimum 1 -Maximum 4
    $total = [math]::Round($precio * $cant, 2)
    $trx = "TRX-ONLINE-$("{0:D4}" -f $counter)-$(Get-Random -Minimum 1000 -Maximum 9999)"
    $fecha = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    $plataforma = $plataformas | Get-Random

    $body = "{`"trx_id`":`"$trx`",`"fecha_hora`":`"$fecha`",`"monto_total`":$total,`"metodo_pago`":`"$($metodos | Get-Random)`",`"canal`":`"online`",`"plataforma`":`"$plataforma`",`"email_cliente`":`"$($emails | Get-Random)`",`"direccion_envio`":`"Av. Test 123`",`"productos`":[{`"sku`":`"$sku`",`"cantidad`":$cant,`"precio_unitario`":$precio}]}"

    try {
        $resp = Invoke-RestMethod -Method POST -Uri $url -ContentType "application/json" -Body $body
        Write-Host "[$((Get-Date).ToString('HH:mm:ss'))] OK  | $trx | $plataforma | $sku x$cant | `$$total"
    } catch {
        Write-Host "[$((Get-Date).ToString('HH:mm:ss'))] ERR | $trx | $($_.Exception.Message)"
    }

    Start-Sleep -Seconds 4
}
