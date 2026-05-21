# Análisis Técnico — Justificación de Patrones de Diseño
## Proyecto Evaluación 2 — Arquitectura de Software

---

## Contexto del Sistema

El sistema implementa una arquitectura de microservicios en cinco capas. Cada capa tiene una responsabilidad delimitada y adopta un patrón de diseño GoF que resuelve un problema arquitectónico concreto, no como elección arbitraria, sino como respuesta a requisitos reales del cliente:

> *"El sistema debe soportar múltiples entornos, garantizar la seguridad entre capas, ser mantenible por un equipo distribuido y escalar sin reescribir código existente."*

```
┌─────────────────────────────────────────────────────┐
│   frontend-app  (Node.js)  — Patron FACTORY METHOD  │
└────────────────────────┬────────────────────────────┘
                         │ HTTP / JSON
┌────────────────────────▼────────────────────────────┐
│   bff-service   (Spring Boot) — Patron PROXY        │
└────────────────────────┬────────────────────────────┘
                         │ HTTP / JSON (red interna)
┌────────────────────────▼────────────────────────────┐
│   orq-service   (Spring Boot) — Patron STRATEGY     │
│   (llama a MS1 y MS2 en paralelo — CompletableFuture)│
└──────────────┬─────────────────┬───────────────────┘
               │ HTTP            │ HTTP
┌──────────────▼──────┐  ┌───────▼─────────────────┐
│  ms1-pos            │  │  ms2-online              │
│  (Spring Boot)      │  │  (Spring Boot)           │
│  Patron SINGLETON   │  │  Patron SINGLETON        │
│  Ventas POS         │  │  Ventas Online           │
│  Puerto :8081       │  │  Puerto :8083            │
└─────────────────────┘  └──────────────────────────┘
```

**Responsabilidades por capa:**

| Capa | Responsabilidad |
|---|---|
| MS1 / MS2 | Recibir datos, validarlos, limpiarlos y almacenarlos. Sin logica de negocio. |
| orq-service | Consultar ambos MS en paralelo, consolidar y aplicar la estrategia de procesamiento |
| bff-service | Validar token Bearer, auditar y delegar al orq |
| frontend-app | Crear el cliente HTTP segun el entorno y consumir el BFF |

---

## 1. Frontend App — Patrón Factory Method

### Categoría GoF
Creacional.

### El Problema sin el Patrón

El frontend debe comunicarse con el BFF desde tres entornos distintos (desarrollo local, CI/CD, producción Docker). Las URLs y timeouts varían por entorno. Sin un patrón, el código cliente quedaría así:

```javascript
// CÓDIGO FRÁGIL sin Factory:
if (process.env.NODE_ENV === 'production') {
  this.url = 'http://bff-service:8080';
  this.timeout = 5000;
} else {
  this.url = 'http://localhost:8080';
  this.timeout = 10000;
}
// Duplicado en cada componente que hace HTTP.
// Agregar un nuevo entorno rompe múltiples archivos.
// Los tests necesitan manipular process.env globalmente (frágil).
```

### La Solución con Factory Method

```
ApiServiceFactory.create(tipo, entorno)
       │
       ├── 'data'  + 'production'  → new DataService({ url: 'http://bff-service:8080', timeout: 5000 })
       ├── 'data'  + 'development' → new DataService({ url: 'http://localhost:8080', timeout: 10000 })
       ├── 'auth'  + 'production'  → new AuthService({ url: 'http://bff-service:8080', timeout: 5000 })
       └── 'report' + cualquier    → new ReportService(...)  ← extensible sin modificar la factory
```

**Implementación real:**
```javascript
// PATRÓN FACTORY METHOD: Justificación técnica para evaluación parcial 2
static create(serviceType = 'data', environment = 'production', overrides = {}) {
  const envConfig = ApiServiceFactory.ENVIRONMENTS[environment];
  const ServiceClass = ApiServiceFactory.REGISTRY[serviceType];
  return new ServiceClass({ ...envConfig, ...overrides });
}
```

### Por qué mejora la Mantenibilidad

| Dimensión | Sin Factory | Con Factory Method |
|-----------|------------|-------------------|
| Cambiar URL de producción | Editar N archivos | Editar 1 línea en `ENVIRONMENTS` |
| Agregar tipo de servicio nuevo | Copiar configuración en varios sitios | `factory.register('report', ReportService)` |
| Tests unitarios de HTTP | Requieren mock global de `process.env` | Inyectar `fetcher` mock en `overrides` |
| Onboarding de nuevo desarrollador | Buscar en qué archivo se crean los clientes | Leer un único archivo de factory |

### Por qué mejora la Seguridad

El parámetro `overrides.fetcher` permite tests que **nunca hacen HTTP real**, eliminando el riesgo de que tests en CI llamen accidentalmente a servicios de producción. Además, los headers de autenticación se configuran en la factory (un único punto), no dispersos en componentes.

### Principios SOLID Aplicados

- **OCP (Open/Closed):** `register()` permite extender con `ReportService` sin modificar `create()`.
- **DIP (Dependency Inversion):** Los componentes dependen de `ApiServiceFactory` (abstracción), nunca de `DataService` directamente.
- **SRP (Single Responsibility):** La factory crea; los servicios hacen HTTP; los componentes renderizan.

### Alternativa Descartada: Abstract Factory

Abstract Factory crea **familias** de objetos relacionados (ej: tema claro/oscuro). Para este sistema donde solo necesitamos variar configuración HTTP entre entornos, Abstract Factory añade clases innecesarias (viola el principio YAGNI). Factory Method es el mínimo que resuelve el problema de forma elegante.

---

## 2. BFF Service — Patrón Proxy

### Categoría GoF
Estructural.

### El Problema sin el Patrón

El BFF necesita validar tokens, auditar operaciones y manejar errores de servicios internos. Sin Proxy, todas esas responsabilidades contaminarían el Controller:

```java
// CÓDIGO FRÁGIL sin Proxy — Controller con 4 responsabilidades:
@GetMapping("/data")
public ResponseEntity<DataResponse> getData(@RequestHeader String token) {
  if (token == null || !token.startsWith("Bearer ")) {   // Seguridad en Controller
    return ResponseEntity.status(401).build();
  }
  System.out.println("[LOG] Request: " + token);         // Auditoría en Controller
  try {
    DataResponse r = client.fetchData(requestId, token);
    System.out.println("[LOG] Response: " + r.status()); // Más auditoría en Controller
    return ResponseEntity.ok(r);
  } catch (Exception e) {
    return ResponseEntity.status(500).build();            // Error handling en Controller
  }
}
// → Imposible testear la validación sin levantar HTTP
// → Cambiar el esquema de auth requiere editar todos los Controllers
```

### La Solución con Proxy

```
BffController (Client)
       │ llama a
ServiceProxy (Proxy) implements IOrqService
       │
       ├── validateBearerToken()   ← Proxy de Protección
       ├── auditLog("REQUEST")     ← Proxy de Auditoría
       ├── realSubject.fetchData() ← Delegación al RealSubject
       └── auditLog("RESPONSE")   ← Post-interceptación
              │
       OrqServiceClient (RealSubject) implements IOrqService
              │ HTTP
       orq-service:8081
```

**Implementación real:**
```java
// PATRÓN PROXY: Justificación técnica para evaluación parcial 2
@Override
public DataResponse fetchData(String requestId, String authToken) {
    validateBearerToken(authToken);           // PRE: Protección
    auditLog("REQUEST", requestId, authToken);

    DataResponse response;
    try {
        response = realSubject.fetchData(requestId, authToken); // Delegación
    } catch (Exception e) {
        return DataResponse.error("Error interno: " + e.getMessage());
    }

    auditLog("RESPONSE", requestId, response.status()); // POST: Auditoría
    return response;
}
```

### Por qué mejora la Mantenibilidad

| Dimensión | Sin Proxy | Con Proxy |
|-----------|----------|-----------|
| Agregar nuevo header de auditoría | Modificar todos los Controllers | Solo modificar `auditLog()` en ServiceProxy |
| Cambiar Bearer → mTLS | Buscar en todos los Controllers | Solo modificar `validateBearerToken()` |
| Tests de seguridad | Requieren contexto HTTP completo | `ServiceProxyTest` puro con JUnit + Mockito |
| Nuevo endpoint | Seguridad/auditoría ya incluidas por el Proxy | Cero código extra de seguridad en el Controller |

### Por qué mejora la Seguridad

**Un único punto de control:** Ninguna petición llega al `OrqServiceClient` sin pasar por el Proxy. Si hubiera múltiples Controllers sin Proxy, cada uno debería duplicar la validación, y la omisión en uno crearía una vulnerabilidad. El Proxy garantiza que esto sea imposible.

**Sin stack traces al cliente:** El Proxy captura excepciones del servicio real y devuelve `DataResponse.error(mensaje)` en lugar de información interna del sistema (CVE-style information disclosure).

**Trazabilidad forense:** El audit log registra cada operación con timestamp, permitiendo reconstruir incidentes post-facto (requisito de cumplimiento GDPR/OWASP).

### Diferencia entre Proxy y Decorator

| Proxy | Decorator |
|-------|-----------|
| Controla **acceso** al objeto real | Agrega **funcionalidad visible** al cliente |
| Puede crear el RealSubject internamente | Siempre recibe el objeto envuelto del exterior |
| El cliente no sabe si hay un proxy | El cliente generalmente conoce los decoradores |
| Casos de uso: seguridad, caché, lazy init | Casos de uso: compresión, cifrado, formateo |

**Conclusión:** En este sistema el objetivo es **controlar y proteger el acceso** al orq-service → Proxy. Si el objetivo fuera agregar compresión de respuesta visible al cliente, sería Decorator.

### Principios SOLID Aplicados

- **SRP:** Controller → HTTP; ServiceProxy → seguridad/auditoría; OrqServiceClient → llamada interna.
- **OCP:** Agregar rate limiting solo requiere un nuevo método privado en `ServiceProxy`.
- **LSP:** `ServiceProxy` implementa `IOrqService`, es intercambiable con `OrqServiceClient` sin que el Controller lo note.
- **DIP:** `BffController` depende de `IOrqService` (abstracción), no de `OrqServiceClient` (implementación).

---

## 3. Orq Service — Patrón Strategy

### Categoría GoF
Comportamiento.

### El Problema sin el Patrón

El servicio orquestador procesa solicitudes con distintos algoritmos según el tipo de dato y la carga actual. Sin Strategy:

```java
// CÓDIGO FRÁGIL sin Strategy:
public String process(String data, String type) {
  if (type.equals("batch")) {
    // 50 líneas de lógica batch
  } else if (type.equals("stream")) {
    // 50 líneas de lógica streaming
  } else if (type.equals("cache")) {
    // 50 líneas de lógica de caché
  }
  // → Imposible testear un algoritmo sin ejecutar todos los otros
  // → Agregar "ml-processing" requiere modificar este método
}
```

### La Solución con Strategy

```
ProcessingContext
  └── setStrategy(IProcessingStrategy)    ← cambia el algoritmo en RUNTIME
  └── executeStrategy(data) → delega
          │
          ├── BatchProcessingStrategy.process()   → lotes de N registros
          ├── StreamProcessingStrategy.process()  → tiempo real, registro a registro
          └── CacheProcessingStrategy.process()   → hit/miss → evita reprocesamiento
```

**Implementación real:**
```java
// PATRÓN STRATEGY: Justificación técnica para evaluación parcial 2
// El contexto delega; la estrategia encapsula el algoritmo.
public String executeStrategy(String data) {
    System.out.println("[Context] Ejecutando: " + strategy.getStrategyName());
    return strategy.process(data);
}

// Cambio de algoritmo en runtime según carga del sistema:
context.setStrategy(new BatchProcessingStrategy(1000)); // alto volumen
context.setStrategy(new StreamProcessingStrategy());    // tiempo real
```

### Por qué mejora la Mantenibilidad

- **Algoritmos aislados:** Cada Strategy tiene su propia clase con una única razón para cambiar (SRP). Modificar `BatchProcessingStrategy` no afecta a `StreamProcessingStrategy`.
- **Extensibilidad:** Agregar `MLProcessingStrategy` no modifica el contexto ni las estrategias existentes (OCP).
- **Testabilidad:** Cada Strategy se unit-testea de forma completamente independiente con datos de prueba controlados.
- **Cambio en runtime:** La estrategia puede cambiar sin reiniciar el servicio, respondiendo a picos de carga o cambios en el tipo de datos.

### Alternativa Descartada: Template Method

Template Method define el esqueleto de un algoritmo y permite que las subclases sobreescriban pasos específicos, pero lo hace mediante **herencia estática**: la subclase se determina en tiempo de compilación. Strategy usa **composición dinámica**: el algoritmo concreto se inyecta en runtime. Para un servicio que necesita cambiar de algoritmo según la carga actual, Strategy es la única opción correcta.

### Principios SOLID Aplicados

- **OCP:** Nuevo algoritmo = nueva clase; cero cambios en el contexto.
- **SRP:** Cada algoritmo tiene exactamente una razón para cambiar.
- **DIP:** `ProcessingContext` depende de `IProcessingStrategy` (interfaz), no de implementaciones concretas.

---

## 4. MS1-pos y MS2-online — Patrón Singleton

### Categoría GoF
Creacional.

### Descripción de los microservicios

El sistema cuenta con **dos microservicios de datos**, cada uno con su propio dominio y repositorio en memoria:

| Microservicio | Puerto | Dominio | Endpoint entrada | Endpoint consulta |
|---|---|---|---|---|
| `ms1-pos` | 8081 | Ventas en tienda física | `POST /api/pos/simulate-mq` | `GET /api/pos/data` |
| `ms2-online` | 8083 | Ventas canal online | `POST /api/online/venta` | `GET /api/online/ventas` |

Ambos microservicios tienen la **misma responsabilidad**: recibir datos, validarlos, limpiarlos y almacenarlos. No aplican filtros ni lógica de negocio — esa responsabilidad recae en el orq-service.

### El Problema sin el Patrón

Cada solicitud concurrente crea una nueva instancia del repositorio, generando múltiples listas independientes y pérdida de datos:

```java
// CÓDIGO FRÁGIL sin Singleton:
class VentaRepository {
  public void save(Venta v) {
    List<Venta> db = new ArrayList<>();  // Nueva lista en cada llamada
    db.add(v);
    // → Con 100 solicitudes concurrentes = 100 listas separadas
    // → GET /ventas devuelve 0 registros porque cada lista es local
    // → Inconsistencia total de estado
  }
}
```

### La Solución con Singleton (Holder Pattern)

Ambos microservicios implementan el mismo patrón — aquí el ejemplo de MS2:

```java
// PATRON SINGLETON — Holder Pattern: thread-safe sin synchronized
@Repository
public class OnlineVentaRepository {

    protected OnlineVentaRepository() {}  // Constructor protegido

    private static class DatabaseHolder {
        // La JVM garantiza que esta inicializacion es atomica
        static final List<OnlineVenta> INSTANCE = new CopyOnWriteArrayList<>();
    }

    public static List<OnlineVenta> getDatabase() {
        return DatabaseHolder.INSTANCE;   // Siempre la misma lista
    }

    public OnlineVenta save(OnlineVenta venta) {
        if (venta.getId() == null) {
            venta.setId((long) (getDatabase().size() + 1));
        }
        getDatabase().add(venta);
        return venta;
    }
}
```

MS1 implementa el mismo patrón en `PosTransactionRepository` con `CopyOnWriteArrayList<PosTransaction>`.

### Por qué el Holder Pattern es superior a otras implementaciones de Singleton

| Implementacion | Thread-safe | Lazy init | Overhead |
|---|---|---|---|
| Campo estatico simple | No (race condition) | No | Ninguno |
| `synchronized getInstance()` | Si | Si | Alto (lock en cada llamada) |
| Double-checked locking | Si (con `volatile`) | Si | Bajo (lock solo primera vez) |
| **Holder Pattern** (elegido) | **Si (por la JVM)** | **Si** | **Ninguno** |

`CopyOnWriteArrayList` se elige sobre `ArrayList` porque permite lecturas concurrentes sin bloqueo, apropiado para un GET que puede ejecutarse mientras el simulador escribe.

### Por qué mejora la Mantenibilidad y Seguridad

- **Una sola fuente de verdad:** Todos los threads del microservicio comparten la misma lista. Un POST de `simulador-pos.ps1` y un GET del orq-service leen exactamente los mismos datos.
- **Thread safety garantizada por la JVM:** No requiere locks explícitos; la especificación del lenguaje garantiza que la inicialización estática de clases es atómica.
- **Separación de dominios:** MS1 y MS2 tienen repositorios Singleton independientes. El orq los consulta en paralelo y consolida, evitando que un dominio afecte al otro.

### Alternativa Descartada: Spring IoC Bean Singleton

Spring Boot gestiona beans como Singleton por defecto mediante `@Scope("singleton")`. Esta sería la solución más idiomática en un proyecto Spring completo. Sin embargo, para **demostrar explícitamente el patrón de diseño GoF** en la evaluación, se implementa el patrón clásico en la capa de acceso a datos, desacoplando esta garantía del framework y haciendo el código portable a cualquier contexto Java.

---

## Tabla Resumen para Defensa Oral

| Componente | Patron | Categoria GoF | Problema del Cliente | Principios SOLID | Alternativa Descartada |
|---|---|---|---|---|---|
| `frontend-app` | Factory Method | Creacional | Instanciar clientes HTTP por entorno sin acoplamiento | OCP, DIP, SRP | Abstract Factory (YAGNI) |
| `bff-service` | Proxy | Estructural | Centralizar seguridad y auditoria sin contaminar el Controller | SRP, OCP, LSP, DIP | Decorator (no controla acceso) |
| `orq-service` | Strategy | Comportamiento | Intercambiar algoritmos de procesamiento en runtime; consolidar MS1+MS2 en paralelo | OCP, SRP, DIP | Template Method (herencia estatica) |
| `ms1-pos` | Singleton | Creacional | Unica lista thread-safe de ventas POS en memoria compartida entre todos los threads | SRP | Spring IoC (acoplamiento al framework) |
| `ms2-online` | Singleton | Creacional | Unica lista thread-safe de ventas online, dominio separado de MS1 | SRP | Spring IoC (acoplamiento al framework) |

---

## Preguntas Frecuentes en Defensa Oral

**¿Por qué Factory y no simplemente un objeto de configuración?**
Un objeto de configuración resuelve el problema de los valores, pero no el de la creación. Factory Method encapsula tanto la configuración como la instanciación, permite polimorfismo en runtime y está abierto a extensión (`register()`) sin modificar el código existente.

**¿Por qué Proxy y no Decorator en el BFF?**
La distinción clave es el propósito: Proxy controla **acceso** (quién puede llamar al servicio real y bajo qué condiciones), Decorator añade **funcionalidad visible al cliente**. Nuestro BFF necesita proteger el acceso al orq-service → Proxy. Si necesitáramos comprimir la respuesta para el frontend, sería Decorator.

**¿Por qué Strategy y no un simple `switch` en el orquestador?**
Un `switch` viola OCP: agregar un nuevo algoritmo requiere modificar el método. Strategy permite agregar nuevos algoritmos como clases independientes. Además, Strategy permite cambiar el algoritmo **en runtime** basado en condiciones dinámicas (carga actual, tipo de datos), algo imposible con un `switch` estático.

**¿Por qué Singleton con Holder Pattern y no `synchronized`?**
`synchronized getInstance()` adquiere un lock en **cada llamada**, incluso cuando la instancia ya existe (overhead innecesario bajo alta concurrencia). El Holder Pattern delega la thread-safety a la JVM (garantía de la especificación del lenguaje), con costo cero en el camino feliz.

**¿Cómo demuestran que el código funciona?**
Cada componente tiene tests unitarios con JUnit 5 (Java) y Jest (Node.js). La cobertura se mide con JaCoCo en los proyectos Maven y con `jest --coverage` en el frontend. Ambas configuraciones tienen umbrales mínimos del 70% que fallan el build si no se cumplen.

---

## Referencias

- Gamma, E., Helm, R., Johnson, R., Vlissides, J. (1994). *Design Patterns: Elements of Reusable Object-Oriented Software.* Addison-Wesley.
- Martin, R. C. (2003). *Agile Software Development: Principles, Patterns, and Practices.* Prentice Hall.
- Richardson, C. (2018). *Microservices Patterns.* Manning Publications.
- Bloch, J. (2018). *Effective Java, 3rd Edition.* Addison-Wesley. (Item 3: Singleton con Holder Pattern)
- OWASP. (2023). *Logging Cheat Sheet.* owasp.org/www-project-cheat-sheets
