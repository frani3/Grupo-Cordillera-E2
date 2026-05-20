# Demo — Grupo Cordillera E2

## Antes de empezar

```bash
docker compose up -d
```

Esperar ~20 segundos hasta que Spring Boot arranque, luego verificar:

| Servicio | URL | Esperado |
|---|---|---|
| frontend-app | http://localhost:3000/health | `{"status":"ok"}` |
| bff-service | http://localhost:8080/api/proxy/health | `{"status":"ok","source":"bff-service"}` |
| orq-service | http://localhost:8082/api/health | `{"status":"ok","source":"orq-service"}` |
| ms1-pos | http://localhost:8081/api/pos/data | `[]` |
| ms2-online | http://localhost:8083/api/online/health | `{"status":"UP","service":"ms2-online","totalVentas":0}` |

---

## PASO 1 — Insertar transacciones en ms1-pos

### En Postman

**Transacción 1**
- Método: `POST`
- URL: `http://localhost:8081/api/pos/simulate-mq`
- Body → raw → JSON:

```json
{
  "trx_id": "TRX-A",
  "sucursal": "Santiago Centro",
  "caja_id": "CAJA-01",
  "fecha_hora": "2026-05-12T09:00:00",
  "monto_total": 29990.0,
  "metodo_pago": "DEBITO",
  "vendedor_id": "VND-01",
  "productos": [
    { "sku": "LAPTOP-01", "cantidad": 1, "precio_unitario": 29990.0 }
  ]
}
```

Respuesta esperada: `Mensaje procesado y almacenado correctamente en la BD simulada (Singleton).`

---

**Transacción 2**
- Método: `POST`
- URL: `http://localhost:8081/api/pos/simulate-mq`
- Body → raw → JSON:

```json
{
  "trx_id": "TRX-B",
  "sucursal": "Providencia",
  "caja_id": "CAJA-02",
  "fecha_hora": "2026-05-12T10:30:00",
  "monto_total": 5990.0,
  "metodo_pago": "CREDITO",
  "vendedor_id": "VND-03",
  "productos": [
    { "sku": "AURICULARES-01", "cantidad": 1, "precio_unitario": 5990.0 }
  ]
}
```

---

**Transacción 3**
- Método: `POST`
- URL: `http://localhost:8081/api/pos/simulate-mq`
- Body → raw → JSON:

```json
{
  "trx_id": "TRX-C",
  "sucursal": "Las Condes",
  "caja_id": "CAJA-05",
  "fecha_hora": "2026-05-12T14:15:00",
  "monto_total": 12500.0,
  "metodo_pago": "EFECTIVO",
  "vendedor_id": "VND-07",
  "productos": [
    { "sku": "TABLET-02", "cantidad": 1, "precio_unitario": 12500.0 }
  ]
}
```

---

## PASO 1b — Insertar ventas online en ms2-online (simulador o manual)

### Opción A — Simulador Python (corre en paralelo con el demo)

```bash
python simulador-online.py --url http://localhost:8083
```

### Opción B — Manual en Postman

- Método: `POST`
- URL: `http://localhost:8083/api/online/venta`
- Body → raw → JSON:

```json
{
  "trx_id": "TRX-ONLINE-001",
  "fecha_hora": "2026-05-20T10:00:00Z",
  "monto_total": 149.97,
  "metodo_pago": "tarjeta",
  "canal": "online",
  "plataforma": "web",
  "email_cliente": "juan@mail.com",
  "direccion_envio": "Av. Providencia 123, Santiago",
  "productos": [
    { "sku": "MOUSE-002", "cantidad": 3, "precio_unitario": 29.99 },
    { "sku": "HUB-006",   "cantidad": 2, "precio_unitario": 34.99 }
  ]
}
```

Respuesta esperada: `{"status":"OK","trx_id":"TRX-ONLINE-001"}`

### Verificar ventas guardadas en ms2

- Método: `GET`
- URL: `http://localhost:8083/api/online/ventas?dias=7`

---

## PASO 2 — Verificar que ms1-pos guardó los datos

- Método: `GET`
- URL: `http://localhost:8081/api/pos/data`

Respuesta esperada: array con las 3 transacciones, cada una con sus items y status `PROCESADO_OK_ESPERANDO_ORQUESTADOR`.

---

## PASO 3 — orq-service lee y procesa (patrón Strategy)

### Estrategia BATCH — cuenta, suma montos y lista IDs

- Método: `GET`
- URL: `http://localhost:8082/api/data?id=reporte&strategy=batch`

```json
{
  "status": "ok",
  "data": "BATCH[reporte]: 3 transacciones | total=$48480 | ids=[TRX-A, TRX-B, TRX-C]",
  "source": "orq-service"
}
```

---

### Estrategia STREAM — muestra la última transacción ingresada

- Método: `GET`
- URL: `http://localhost:8082/api/data?id=reporte&strategy=stream`

```json
{
  "status": "ok",
  "data": "STREAM[reporte]: ultima=TRX-C | sucursal=Las Condes | monto=$12500",
  "source": "orq-service"
}
```

---

### Estrategia CACHE — lista IDs con método de pago

- Método: `GET`
- URL: `http://localhost:8082/api/data?id=reporte&strategy=cache`

```json
{
  "status": "ok",
  "data": "CACHE[reporte]: 3 registros servidos desde cache | TRX-A(DEBITO), TRX-B(CREDITO), TRX-C(EFECTIVO)",
  "source": "orq-service"
}
```

---

## PASO 4 — bff-service protege con Bearer token (patrón Proxy)

### Con token — flujo completo hasta ms1-pos

- Método: `GET`
- URL: `http://localhost:8080/api/proxy/data?id=ventas-dia`
- Headers: `Authorization: Bearer token-cordillera`

---

### Sin token — bloqueado por el Proxy de Protección

- Método: `GET`
- URL: `http://localhost:8080/api/proxy/data?id=ventas-dia`
- Sin header Authorization

Respuesta esperada: HTTP 401 + mensaje de error.

---

## PASO 5 — frontend recorre toda la cadena (patrón Factory)

### GET /api/dashboard → frontend → bff → orq → ms1-pos

- Método: `GET`
- URL: `http://localhost:3000/api/dashboard`
- Headers: `Authorization: Bearer token-cordillera`

Respuesta esperada: los datos reales de ms1-pos procesados por orq-service, llegando desde el frontend.

---

### GET /api/user/:id

- Método: `GET`
- URL: `http://localhost:3000/api/user/42`
- Headers: `Authorization: Bearer token-cordillera`

---

### Sin token — el frontend usa "Bearer demo-token" por defecto

- Método: `GET`
- URL: `http://localhost:3000/api/dashboard`
- Sin headers

Igual funciona porque el frontend inyecta un token de demo automáticamente.

---

## Puntos a destacar en la demo

| Patrón | Servicio | Qué mostrar |
|---|---|---|
| **Singleton** | ms1-pos (8081) | El repositorio usa `CopyOnWriteArrayList` como BD en memoria. Un solo `getDatabase()` global |
| **Singleton** | ms2-online (8083) | Mismo patrón Holder para ventas online — repositorio independiente |
| **Strategy** | orq-service (8082) | Cambiar `?strategy=batch/stream/cache` cambia el algoritmo sin tocar el código |
| **Strategy** | orq-service (8082) | El orq llama a MS1 y MS2 en paralelo con `CompletableFuture` y consolida la respuesta |
| **Proxy** | bff-service (8080) | Sin token → 401. Con token → pasa. Logs de auditoría visibles en Docker |
| **Factory** | frontend-app (3000) | `ApiServiceFactory.create("data", "production")` instancia el cliente según el entorno |

---

## Logs en tiempo real (para mostrar durante la demo)

```bash
# Auditoria del BFF — cada request que pasa por el Proxy
docker logs -f bff-service-app 2>&1 | grep AUDIT

# Procesamiento del ms1-pos — confirmacion de cada guardado
docker logs -f ms1-pos-app 2>&1 | grep -E "Iniciando|guardada"

# Ventas online — ms2 recibiendo datos del simulador
docker logs -f ms2-online-app 2>&1 | grep -E "procesando|guardada"
```

---

## Para detener todo

```bash
docker compose down
```
