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
| **Strategy** | orq-service (8082) | Cambiar `?strategy=batch/stream/cache` cambia el algoritmo sin tocar el código |
| **Proxy** | bff-service (8080) | Sin token → 401. Con token → pasa. Logs de auditoría visibles en Docker |
| **Factory** | frontend-app (3000) | `ApiServiceFactory.create("data", "production")` instancia el cliente según el entorno |

---

## Logs en tiempo real (para mostrar durante la demo)

```bash
# Auditoría del BFF — cada request que pasa por el Proxy
docker logs -f bff-service-app 2>&1 | grep AUDIT

# Procesamiento del ms1-pos — confirmación de cada guardado
docker logs -f ms1-pos-app 2>&1 | grep -E "Iniciando|guardada"
```

---

## Para detener todo

```bash
docker compose down
```
