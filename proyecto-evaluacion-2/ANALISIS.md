# Análisis Técnico — Justificación de Patrones de Diseño
## Proyecto Evaluación 2 — Arquitectura de Software

---

## Contexto del Sistema

El sistema implementa una arquitectura de microservicios en cuatro capas. Cada capa tiene una responsabilidad delimitada y adopta un patrón de diseño GoF que resuelve un problema arquitectónico concreto, no como elección arbitraria, sino como respuesta a requisitos reales del cliente:

> *"El sistema debe soportar múltiples entornos, garantizar la seguridad entre capas, ser mantenible por un equipo distribuido y escalar sin reescribir código existente."*

```
┌─────────────────────────────────────────────────────┐
│   frontend-app  (Node.js)  — Patrón FACTORY METHOD  │
└────────────────────────┬────────────────────────────┘
                         │ HTTP / JSON
┌────────────────────────▼────────────────────────────┐
│   bff-service   (Spring Boot) — Patrón PROXY        │
└────────────────────────┬────────────────────────────┘
                         │ HTTP / JSON (red interna)
┌────────────────────────▼────────────────────────────┐
│   orq-service   (Spring Boot) — Patrón STRATEGY     │
└────────────────────────┬────────────────────────────┘
                         │ JDBC
┌────────────────────────▼────────────────────────────┐
│   data-ms       (Spring Boot) — Patrón SINGLETON    │
└─────────────────────────────────────────────────────┘
```

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

## 4. Data MS — Patrón Singleton

### Categoría GoF
Creacional.

### El Problema sin el Patrón

El microservicio de datos necesita un pool de conexiones a la base de datos. Sin Singleton:

```java
// CÓDIGO FRÁGIL sin Singleton:
class UserRepository {
  public String findById(int id) {
    // Cada llamada crea una nueva instancia del gestor
    DatabaseConnectionManager mgr = new DatabaseConnectionManager();
    // → Con 100 solicitudes concurrentes = 100 instancias del pool
    // → Agotamiento de conexiones disponibles en la base de datos
    // → Inconsistencias de estado entre instancias
    // → Overhead de memoria proporcional a la carga
  }
}
```

### La Solución con Singleton (Holder Pattern)

```java
// PATRÓN SINGLETON: Justificación técnica para evaluación parcial 2
//
// Initialization-on-Demand Holder: thread-safe sin synchronized en el camino feliz.
// La JVM garantiza que la inicialización estática de clases es atómica.
public class DatabaseConnectionManager {
    private DatabaseConnectionManager() {}           // Constructor privado

    private static class Holder {                    // Carga diferida
        static final DatabaseConnectionManager INSTANCE = new DatabaseConnectionManager();
    }

    public static DatabaseConnectionManager getInstance() {
        return Holder.INSTANCE;                      // Thread-safe, sin locks
    }
}
```

### Por qué el Holder Pattern es superior a otras implementaciones de Singleton

| Implementación | Thread-safe | Lazy init | Overhead |
|---------------|-------------|-----------|----------|
| Campo estático simple | No (race condition) | No | Ninguno |
| `synchronized getInstance()` | Sí | Sí | Alto (lock en cada llamada) |
| Double-checked locking | Sí (con `volatile`) | Sí | Bajo (lock solo primera vez) |
| **Holder Pattern** (elegido) | **Sí (por la JVM)** | **Sí** | **Ninguno** |

### Por qué mejora la Mantenibilidad y Seguridad

- **Control de recursos:** Un único pool de conexiones es más eficiente que múltiples instancias compitiendo. Las conexiones de base de datos son recursos limitados y costosos.
- **Consistencia:** La configuración (URL, credenciales, pool size) es compartida y coherente entre todos los repositorios del microservicio.
- **Thread safety garantizada por la JVM:** No requiere locks explícitos ni anotaciones especiales; la especificación del lenguaje garantiza que la inicialización estática de clases es atómica.

### Alternativa Descartada: Spring IoC Bean Singleton

Spring Boot gestiona beans como Singleton por defecto mediante `@Scope("singleton")`. Esta sería la solución más idiomática en un proyecto Spring completo. Sin embargo, para **demostrar explícitamente el patrón de diseño GoF** en la evaluación, se implementa el patrón clásico en la capa de acceso a datos, desacoplando esta garantía del framework y haciendo el código portable a cualquier contexto Java.

---

## Tabla Resumen para Defensa Oral

| Componente | Patrón | Categoría GoF | Problema del Cliente | Principios SOLID | Alternativa Descartada |
|---|---|---|---|---|---|
| `frontend-app` | Factory Method | Creacional | Instanciar clientes HTTP por entorno sin acoplamiento | OCP, DIP, SRP | Abstract Factory (YAGNI) |
| `bff-service` | Proxy | Estructural | Centralizar seguridad y auditoría sin contaminar el Controller | SRP, OCP, LSP, DIP | Decorator (no controla acceso) |
| `orq-service` | Strategy | Comportamiento | Intercambiar algoritmos de procesamiento en runtime | OCP, SRP, DIP | Template Method (herencia estática) |
| `data-ms` | Singleton | Creacional | Única instancia thread-safe del pool de conexiones | SRP | Spring IoC (acoplamiento al framework) |

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
