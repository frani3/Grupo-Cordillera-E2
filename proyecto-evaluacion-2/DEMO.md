# Demo — Grupo Cordillera E2

## Antes de empezar

```powershell
cd proyecto-evaluacion-2
docker compose up -d
```

Abrir DOS terminales y correr los simuladores en paralelo:

```powershell
# Terminal 1 — ventas POS
.\simulador-pos.ps1

# Terminal 2 — ventas online
.\simulador-online.ps1
```

---

## 1. MS2 — ventas online (datos crudos)

```
GET http://localhost:8083/api/online/ventas
```

Muestra las ventas online tal como llegaron. MS2 solo recibe, limpia y guarda — sin filtros.

---

## 2. MS1 — ventas POS (datos crudos)

```
GET http://localhost:8081/api/pos/data
```

Muestra las ventas de tienda fisica tal como llegaron. Misma responsabilidad que MS2, distinto canal.

---

## 3. strategy=batch

```
GET http://localhost:8082/api/data?id=demo&strategy=batch
```

El orq consolida MS1 + MS2 en paralelo y suma todo:
```json
{
  "status": "ok",
  "data": "BATCH[demo]: 8 transacciones | total=$45230 | ids=[TRX-POS-0001, TRX-ONLINE-0001, ...]",
  "source": "orq-service"
}
```

---

## 4. strategy=stream

```
GET http://localhost:8082/api/data?id=demo&strategy=stream
```

Muestra solo la ultima transaccion ingresada (monitoreo en tiempo real):
```json
{
  "status": "ok",
  "data": "STREAM[demo]: ultima=TRX-ONLINE-0005 | monto=$149.97",
  "source": "orq-service"
}
```

---

## 5. strategy=cache

```
GET http://localhost:8082/api/data?id=demo&strategy=cache
```

Lista cada transaccion con su metodo de pago (auditoria):
```json
{
  "status": "ok",
  "data": "CACHE[demo]: 8 registros | TRX-POS-0001(DEBITO), TRX-ONLINE-0001(tarjeta), ...",
  "source": "orq-service"
}
```

---

## 6. BFF — sin token (debe dar 401)

```
GET http://localhost:8080/api/proxy/data?id=demo
```

Sin header Authorization. El Proxy bloquea el acceso:
```json
HTTP 401 Unauthorized
```

---

## 7. BFF — con token (pasa al orq)

```
GET http://localhost:8080/api/proxy/data?id=demo
Header: Authorization: Bearer token-cordillera
```

El Proxy valida el token y delega al orq. Devuelve los datos procesados.

---

## Que destacar por patron

| Patron | Donde | Que decir |
|---|---|---|
| **Singleton** | MS1 :8081 y MS2 :8083 | Cada MS tiene una unica lista en memoria compartida entre todos los threads. Un POST del simulador y un GET del orq leen exactamente los mismos datos |
| **Strategy** | orq :8082 | El mismo dato, tres algoritmos distintos. Cambiar `?strategy=` cambia el comportamiento sin tocar el codigo |
| **Proxy** | bff :8080 | Sin token: 401. Con token: pasa. Ningun request llega al orq sin pasar por el Proxy |
| **Factory** | frontend :3000 | `ApiServiceFactory.create("data", "production")` instancia el cliente HTTP segun el entorno automaticamente |
