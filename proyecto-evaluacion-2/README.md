# Proyecto Evaluación 2 — Arquitectura de Software

## Descripción General

Sistema distribuido en cuatro capas que implementa patrones de diseño GoF y sigue la estrategia de branching **GitHub Flow**.

## Arquitectura

```
┌─────────────────────────────────────────────────────┐
│   frontend-app  (Node.js)    — Patrón FACTORY       │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP
┌──────────────────────▼──────────────────────────────┐
│   bff-service   (Spring Boot) — Patrón PROXY        │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP (red interna)
┌──────────────────────▼──────────────────────────────┐
│   orq-service   (Spring Boot) — Patrón STRATEGY     │
└──────────────────────┬──────────────────────────────┘
                       │ JDBC
┌──────────────────────▼──────────────────────────────┐
│   data-ms       (Spring Boot) — Patrón SINGLETON    │
└─────────────────────────────────────────────────────┘
```

## Componentes

| Componente     | Tecnología      | Patrón    | Puerto | Rama                   |
|----------------|-----------------|-----------|--------|------------------------|
| `frontend-app` | Node.js         | Factory   | 3000   | `feature/frontend-bff` |
| `bff-service`  | Java/Spring Boot| Proxy     | 8080   | `feature/frontend-bff` |
| `orq-service`  | Java/Spring Boot| Strategy  | 8081   | `feature/orq-service`  |
| `data-ms`      | Java/Spring Boot| Singleton | 8082   | `feature/data-ms`      |

## Cómo ejecutar los tests

```bash
# Frontend (desde frontend-app/)
npm install && npm test

# Servicios Java (desde cada carpeta Maven)
mvn test                          # Ejecuta tests
mvn test jacoco:report            # Genera reporte HTML en target/site/jacoco/
```

## Documentación

- [BRANCHING.md](BRANCHING.md) — Estrategia GitHub Flow y resolución de conflictos
- [ANALISIS.md](ANALISIS.md)   — Justificación técnica de patrones (para defensa oral)

## Requisitos

- Java 17+ · Maven 3.8+ · Node.js 18+ · NPM 9+
