#!/usr/bin/env python3
"""
Simulador de ventas online para MS2.
Envia transacciones aleatorias cada 4 segundos a ms2-online.
Uso: python simulador-online.py [--url http://localhost:8083]
"""

import json
import random
import time
import urllib.request
import urllib.error
import argparse
from datetime import datetime, timezone

MS2_URL = "http://localhost:8083"

PLATAFORMAS = ["web", "app", "marketplace"]
METODOS_PAGO = ["tarjeta", "transferencia", "paypal", "mercadopago"]
EMAILS = ["juan@mail.com", "maria@mail.com", "pedro@mail.com", "ana@mail.com", "luis@mail.com"]
DIRECCIONES = [
    "Av. Providencia 123, Santiago",
    "Calle Larga 456, Valparaiso",
    "San Martin 789, Mendoza",
    "Corrientes 1000, Buenos Aires",
]
PRODUCTOS = [
    {"sku": "LAPTOP-001", "nombre": "Laptop 15\"",     "precio": 799.99},
    {"sku": "MOUSE-002",  "nombre": "Mouse inalambrico", "precio": 29.99},
    {"sku": "TECLADO-003","nombre": "Teclado mecanico",  "precio": 89.99},
    {"sku": "AURIF-004",  "nombre": "Audifonos BT",      "precio": 59.99},
    {"sku": "CAMARA-005", "nombre": "Webcam HD",         "precio": 49.99},
    {"sku": "HUB-006",    "nombre": "Hub USB-C",         "precio": 34.99},
]

counter = 0

def generar_venta():
    global counter
    counter += 1

    num_productos = random.randint(1, 3)
    seleccionados = random.sample(PRODUCTOS, num_productos)

    items = []
    total = 0.0
    for prod in seleccionados:
        cantidad = random.randint(1, 4)
        items.append({
            "sku": prod["sku"],
            "cantidad": cantidad,
            "precio_unitario": prod["precio"]
        })
        total += prod["precio"] * cantidad

    now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    trx_id = f"TRX-ONLINE-{counter:04d}-{random.randint(1000,9999)}"

    return {
        "trx_id": trx_id,
        "fecha_hora": now,
        "monto_total": round(total, 2),
        "metodo_pago": random.choice(METODOS_PAGO),
        "canal": "online",
        "plataforma": random.choice(PLATAFORMAS),
        "email_cliente": random.choice(EMAILS),
        "direccion_envio": random.choice(DIRECCIONES),
        "productos": items
    }


def post_venta(url, venta):
    body = json.dumps(venta).encode("utf-8")
    req = urllib.request.Request(
        url + "/api/online/venta",
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, {"error": e.reason}
    except Exception as e:
        return 0, {"error": str(e)}


def get_health(url):
    try:
        with urllib.request.urlopen(url + "/api/online/health", timeout=3) as resp:
            return json.loads(resp.read().decode())
    except Exception as e:
        return {"error": str(e)}


def main(ms2_url):
    print(f"[SIMULADOR] Iniciando envio de ventas online -> {ms2_url}")
    print(f"[SIMULADOR] Ctrl+C para detener")
    print("-" * 60)

    health = get_health(ms2_url)
    print(f"[HEALTH] {health}")
    print("-" * 60)

    while True:
        venta = generar_venta()
        status, resp = post_venta(ms2_url, venta)

        ts = datetime.now().strftime("%H:%M:%S")
        productos_str = ", ".join(p["sku"] for p in venta["productos"])

        if status == 200:
            print(f"[{ts}] OK  | {venta['trx_id']} | {venta['plataforma']:12s} | "
                  f"${venta['monto_total']:8.2f} | {productos_str}")
        else:
            print(f"[{ts}] ERR {status} | {venta['trx_id']} | {resp}")

        time.sleep(4)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Simulador ventas online MS2")
    parser.add_argument("--url", default=MS2_URL, help="URL base de ms2-online")
    args = parser.parse_args()
    try:
        main(args.url)
    except KeyboardInterrupt:
        print(f"\n[SIMULADOR] Detenido. Total enviadas: {counter}")
