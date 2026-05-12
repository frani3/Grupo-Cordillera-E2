# Resumen Técnico — `orq-service`
### Proyecto Evaluación 2 · Arquitectura de Software · Grupo Cordillera

---

## 1. Reubicación del Servicio

El microservicio `orq-service` fue desarrollado inicialmente en la raíz del repositorio
(`Grupo-Cordillera-E2/orq-service/`) y posteriormente reubicado mediante `git mv` a su
posición final en `proyecto-evaluacion-2/orq-service/`.

Esta estructura es coherente con el diagrama de capas del archivo `ANALISIS.md`, donde
los cuatro microservicios de la evaluación (`frontend-app`, `bff-service`, `orq-service`
y `data-ms`) conviven bajo un mismo directorio raíz de proyecto. El uso de `git mv`
(en lugar de una copia manual) preserva el historial de commits de cada archivo,
permitiendo reconstruir la evolución completa del código con `git log --follow`.

---

## 2. Patrón Strategy — Implementación

### Categoría GoF
**Comportamental.** Encapsula algoritmos intercambiables en clases independientes y
permite sustituirlos en tiempo de ejecución sin modificar el contexto que los invoca.

### Problema que resuelve
Sin el patrón, la lógica de decisión entre "datos frescos" y "datos de caché" quedaría
como un bloque `if/else` dentro del servicio, violando el principio Open/Closed: agregar
un tercer comportamiento obligaría a modificar código existente y testeado.

### Estructura implementada

```
«interface»  KpiStrategy
  └── obtenerDatos(kpiId: String): KpiResponse

         ├── RealTimeKpiStrategy   @Component @Qualifier("realTime")
         │     Simula (y en producción reemplazará) la llamada al data-ms.
         │     Retorna staleData = false → dato fresco y confiable.
         │
         └── FallbackKpiStrategy   @Component @Qualifier("fallback")
               Simula la recuperación de caché Redis ante fallo del sistema.
               Retorna staleData = true → cumple la HU-08.

KpiOrchestratorContext   @Service
  Contexto del patrón. Recibe ambas estrategias por constructor con
  @Qualifier para evitar ambigüedad en la inyección de Spring.

  orquestarKpi(kpiId, isServiceUp):
    isServiceUp == true  → delega a RealTimeKpiStrategy
    isServiceUp == false → delega a FallbackKpiStrategy
```

### Cumplimiento de la HU-08
La historia de usuario HU-08 exige que, cuando el servicio de datos no esté disponible,
el sistema recupere el último valor conocido desde Redis y lo señalice explícitamente
como dato potencialmente desactualizado.

El campo `staleData: boolean` del modelo `KpiResponse` es el mecanismo de señalización.
Su valor es un invariante de cada estrategia, no una responsabilidad del llamador:

| Estrategia           | `isServiceUp` | `staleData` | Significado                        |
|----------------------|:---:|:---:|----------------------------------------------|
| `RealTimeKpiStrategy`  | `true`  | **`false`** | Dato fresco, confiable               |
| `FallbackKpiStrategy`  | `false` | **`true`**  | Dato de caché Redis, puede desactualizado |

Esto respeta el principio **Fail-Safe Defaults**: la condición de degradación es explícita
por construcción, no dependiente de que el programador recuerde setear el flag.

### Principios SOLID aplicados
- **OCP:** Agregar `CacheL2KpiStrategy` requiere solo una clase nueva; cero cambios en el contexto.
- **SRP:** Cada estrategia tiene una única razón para cambiar.
- **DIP:** `KpiOrchestratorContext` depende de `KpiStrategy` (abstracción), nunca de las implementaciones concretas.

---

## 3. Decisión Crítica — Eliminación de Lombok y Migración a Java Puro

### El error encontrado

Durante la fase de compilación Maven el build falló con el siguiente error:

```
[ERROR] Fatal error compiling:
java.lang.ExceptionInInitializerError:
  com.sun.tools.javac.code.TypeTag :: UNKNOWN

Caused by: java.lang.NoSuchFieldException:
  com.sun.tools.javac.code.TypeTag :: UNKNOWN
    at lombok.javac.Javac.<clinit>(Javac.java:187)
```

### Diagnóstico raíz

| Factor | Detalle |
|---|---|
| JDK instalado | `24+36-3646` — build **final** de Java 24 (marzo 2025) |
| Lombok utilizado | `1.18.36` — última versión estable (enero 2025) |
| Tipo de error | `NoSuchFieldException` — el campo no existe en el bytecode |

Lombok 1.18.36 fue publicado contra el JDK 24 en fase Early Access. El build final
`+36-3646` modificó el enum interno `com.sun.tools.javac.code.TypeTag`, eliminando
el valor `UNKNOWN` que Lombok busca por reflexión en su inicializador estático
(`Javac.java:187`).

El error es `NoSuchFieldException` (campo inexistente), **no** `InaccessibleObjectException`
(acceso denegado). Esto descarta cualquier solución basada en `--add-opens`, `MAVEN_OPTS`
o configuración de `annotationProcessorPaths`: el campo literalmente no está presente
en el JDK instalado.

### Solución adoptada: Builder Manual idiomático

`KpiResponse` fue reescrita como Java puro con una clase interna `Builder` estática.
La API pública es idéntica a la que generaba Lombok:

```java
// Mismo uso que antes — los llamadores no necesitaron cambios
KpiResponse.builder()
    .kpiId(kpiId)
    .nombre("Ventas Mensuales")
    .valor(98750.50)
    .staleData(false)
    .build();
```

La clase implementa constructor vacío, constructor completo, getters, setters,
`equals()` y `hashCode()` basados en `java.util.Objects`.

### Beneficios de la decisión

| Dimensión           | Con Lombok                          | Con Builder Manual               |
|---------------------|-------------------------------------|----------------------------------|
| Compatibilidad JDK  | Requiere EA específico              | Cualquier JDK 17+                |
| Depuración          | Métodos invisibles en el fuente     | Visibles y trazables en el IDE   |
| Portabilidad        | Acoplado al procesador de anotaciones | Java estándar sin dependencias |
| Riesgo CI/CD        | Rompe si el JDK del pipeline cambia | Estable en cualquier entorno     |

La eliminación de Lombok también redujo el `pom.xml` a sus dependencias estrictamente
necesarias: `spring-boot-starter-web`, `spring-boot-starter-test` y JaCoCo.

---

## 4. API REST — `KpiController` y Puerto 8082

Se creó `KpiController` en el paquete `controller`, exponiendo el endpoint que permite
al BFF (`bff-service`) consumir el orquestador vía HTTP.

### Endpoint

```
GET http://orq-service:8082/api/kpis/{kpiId}
```

### Respuesta (200 OK)

```json
{
  "kpiId":     "ventas-q4",
  "nombre":    "Ventas Mensuales",
  "valor":     98750.5,
  "staleData": false
}
```

### Integración futura — Circuit Breaker

El Controller contiene un `TODO` explícito en el punto exacto donde se integrará
Resilience4j cuando el `data-ms` del compañero esté disponible:

```java
// TODO: reemplazar por cb.getState() == CircuitBreaker.State.CLOSED
// cuando se integre Resilience4j con el data-ms del compañero B.
boolean isServiceUp = true;
```

Cuando esa línea se reemplace, el Circuit Breaker calculará automáticamente
`isServiceUp` según el historial de fallos del `data-ms`, activando la
`FallbackKpiStrategy` sin que ninguna otra clase del sistema deba cambiar.

### Puerto 8082
Configurado en `src/main/resources/application.yml`:

```yaml
server:
  port: 8082
spring:
  application:
    name: orq-service
```

El puerto 8082 está reservado para el orquestador dentro del esquema de puertos
del sistema: 8080 (BFF), 8082 (orq-service), 8083+ (data-ms y servicios legacy).

---

## 5. Pruebas Unitarias y Cobertura JaCoCo

### Resultado de la suite

```
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS — Total time: ~7 s
```

### Detalle por clase de test

| Clase de test | Tests | Framework | Qué valida |
|---|:---:|---|---|
| `KpiStrategyTest` | 4 | JUnit 5 puro | `staleData=false` en RealTime; `staleData=true` en Fallback (HU-08); propagación del `kpiId` |
| `KpiOrchestratorContextTest` | 3 | JUnit 5 + Mockito | Selección exclusiva de estrategia según `isServiceUp`; verificación con `never()` |
| `OrqServiceApplicationTests` | 1 | Spring Boot Test | Carga correcta del contexto con todos los beans configurados |

`KpiOrchestratorContextTest` usa inyección por constructor para instanciar el contexto
directamente con mocks (`new KpiOrchestratorContext(mockA, mockB)`), sin levantar
contexto Spring. Esto valida que el diseño no está acoplado al framework.

### Cobertura JaCoCo — umbral 70% (exigido en `ANALISIS.md`)

| Clase | Líneas cubiertas | Líneas perdidas | Cobertura |
|---|:---:|:---:|:---:|
| `RealTimeKpiStrategy` | 7 | 0 | **100%** |
| `FallbackKpiStrategy` | 7 | 0 | **100%** |
| `KpiOrchestratorContext` | 6 | 0 | **100%** |
| `KpiResponse` + `Builder` | 18 | 11 | 62% |
| `OrqServiceApplication` | 1 | 2 | 33% |
| **TOTAL DEL BUNDLE** | **39** | **13** | **75%** |

> **75% de cobertura de líneas — supera el umbral del 70% definido en `ANALISIS.md`.**
>
> Las líneas no cubiertas son setters de `KpiResponse` no ejercitados directamente
> y el método `main()`, que por convención de la industria se cubre en tests de
> integración (fuera del scope de la evaluación).

---

## 6. Estado del Repositorio Git

**Rama activa:** `feature/orq-service`

### Historial de commits del sprint

| Hash | Tipo | Descripción |
|---|---|---|
| `efb5e84` | `refactor` | Reubicar orq-service dentro de proyecto-evaluacion-2 |
| `64592c0` | `feat` | Exponer endpoint REST GET /api/kpis/{kpiId} |
| `3e22e71` | `test` | Agregar JaCoCo y tests unitarios del patrón strategy |
| `54ae739` | `feat` | Implementar patrón strategy para orquestación de KPIs |

Los mensajes siguen el estándar **Conventional Commits** (`tipo(scope): descripción`)
definido en `BRANCHING.md`. Cada commit representa un hito verificable e independiente.

---

*Implementado en la rama `feature/orq-service` · Mayo 2026 · Grupo Cordillera*
