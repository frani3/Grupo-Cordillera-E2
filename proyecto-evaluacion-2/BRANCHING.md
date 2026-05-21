# Estrategia de Branching — GitHub Flow
## Proyecto Evaluación 2 — Arquitectura de Software

---

## ¿Por qué GitHub Flow?

GitHub Flow es una estrategia de branching minimalista centrada en una única rama estable (`main`) y ramas de corta vida para cada funcionalidad. Es la elección correcta para este equipo porque:

| Criterio | GitHub Flow | GitFlow |
|----------|------------|---------|
| Tamaño del equipo | 3 personas → ideal | Diseñado para equipos grandes |
| Complejidad | Baja (1 rama base) | Alta (main, develop, release, hotfix) |
| Velocidad de iteración | Alta | Media (requiere más merges) |
| Estado de `main` | Siempre deployable | Solo `main` es producción |
| Curva de aprendizaje | Mínima | Requiere entrenamiento |

---

## Estructura de Ramas

```
main  (protegida — siempre estable)
 │
 └── rama-merge  ← rama de integracion activa
         frontend-app/  +  bff-service/
         orq-service/
         ms1-pos/
         ms2-online/
         docker-compose.yml
```

### Descripcion de cada rama

| Rama | Estado | Componentes | Patrones |
|------|--------|-------------|--------|
| `main` | Estable | Documentacion base | — |
| `rama-merge` | Activa (desarrollo EP2) | Todos los microservicios | Factory + Proxy + Strategy + Singleton x2 |

---

## Flujo de Trabajo Diario

### 1. Sincronizar con `main` antes de empezar

```bash
git checkout main
git pull origin main
git checkout feature/<tu-rama>
```

### 2. Desarrollar con commits atómicos

```bash
# Agregar solo los archivos del cambio actual (nunca git add .)
git add src/services/ApiServiceFactory.js
git commit -m "feat(frontend-app): agregar registro de AuthService en factory"
```

### 3. Mantener la rama actualizada (evitar conflictos grandes)

```bash
# Incorporar cambios nuevos de main sin crear un merge commit
git fetch origin
git rebase origin/main
```

### 4. Abrir Pull Request cuando la feature esté lista

```
→ Ir a GitHub → Pull Requests → New Pull Request
→ Base: main  |  Compare: feature/<tu-rama>
→ Solicitar revisión de al menos 1 compañero
→ Esperar aprobación antes de mergear
```

---

## Convención de Mensajes de Commit

Seguimos **Conventional Commits** para mantener un historial legible:

```
<tipo>(<scope>): <descripción corta en presente>

Tipos:
  feat     → nueva funcionalidad
  fix      → corrección de bug
  test     → agregar o modificar tests
  refactor → cambio sin impacto en comportamiento
  docs     → solo documentación
  chore    → tareas de build, dependencias, configuración
```

**Ejemplos correctos:**
```bash
feat(bff-service): implementar ServiceProxy con validación Bearer
fix(frontend-app): corregir timeout en ApiService para entorno test
test(data-ms): agregar test de concurrencia para patrón Singleton
docs(orq-service): documentar las tres estrategias en ANALISIS.md
```

**Ejemplos incorrectos:**
```bash
"arreglé cosas"          ← sin tipo ni scope
"WIP"                    ← no describe el cambio
"fix bug"                ← demasiado vago
```

---

## Resolución de Conflictos y Merges

### Estrategia: Rebase antes de abrir el PR

Antes de abrir un Pull Request, cada integrante debe incorporar los cambios de `main` mediante **rebase** (no merge). Esto produce un historial lineal y limpio.

```bash
# Paso a paso
git fetch origin                    # Descargar cambios remotos
git checkout feature/<tu-rama>      # Asegurarse de estar en la feature branch
git rebase origin/main              # Reubicar commits propios encima de main
```

### Qué hacer cuando hay conflictos durante el rebase

```bash
# Git mostrará los archivos en conflicto:
# CONFLICT (content): Merge conflict in src/services/ApiServiceFactory.js

# 1. Abrir el archivo — buscar los marcadores:
<<<<<<< HEAD               ← versión de main
const url = 'http://bff:8080';
=======
const url = 'http://localhost:8080'; ← versión de tu rama
>>>>>>> feat(frontend-app): cambiar URL de desarrollo

# 2. Resolver manualmente (elegir una versión o combinar ambas)
# 3. Marcar como resuelto
git add src/services/ApiServiceFactory.js

# 4. Continuar el rebase
git rebase --continue

# Si decides abortar y empezar desde cero:
git rebase --abort
```

### Protocolo de decisión ante conflictos

| Situación | Quién decide | Acción |
|-----------|-------------|--------|
| Conflicto en mi propio archivo | Yo | Mantener mi versión (`--ours`) |
| Conflicto en archivo de otro integrante | Ambos | Discutir en el PR, resolver juntos |
| Conflicto en `pom.xml` / `package.json` | Ambos | Fusionar dependencias manualmente |
| Conflicto en `ANALISIS.md` | Todo el equipo | Reunión rápida → mantener ambas versiones |

### Comandos útiles para resolver conflictos

```bash
# Ver todos los archivos con conflictos
git status | grep "both modified"

# Usar la versión de main (descartar mis cambios en ese archivo)
git checkout --theirs <archivo>

# Usar mi versión (descartar los cambios de main en ese archivo)
git checkout --ours <archivo>

# Ver el log de qué commits introdujeron el conflicto
git log --oneline --graph --all origin/main..HEAD

# Comparar mi rama con main antes de hacer rebase
git diff origin/main...HEAD
```

---

## Política de Merge en Pull Requests

Usamos **Squash and Merge** en todos los PRs:

```
Historia en feature/frontend-bff:
  a1b2c3  WIP: inicio factory
  d4e5f6  fix: typo en nombre de clase
  g7h8i9  WIP: probando timeout
  j0k1l2  feat: terminar implementacion Factory

Después del Squash en main:
  m3n4o5  feat(frontend-app): implementar ApiServiceFactory con patrón Factory (#1)
```

**¿Por qué Squash?** `main` queda con commits atómicos y significativos, sin ruido de commits intermedios de trabajo en progreso.

**Excepción:** Si los commits de la feature ya son perfectamente atómicos, se puede usar Merge Commit para preservar la historia detallada.

---

## Checklist antes de abrir un Pull Request

```
[ ] Hice rebase sobre origin/main (git rebase origin/main)
[ ] Los tests pasan localmente
    - Backend:  mvn test
    - Frontend: npm test
[ ] La cobertura de tests no bajó del umbral mínimo (70%)
[ ] Revisé mi propio código (self-review)
[ ] El PR tiene título descriptivo en formato Conventional Commits
[ ] El PR tiene descripción explicando el qué y el por qué
[ ] No hay archivos innecesarios en el commit:
    - Sin node_modules/
    - Sin target/
    - Sin archivos .env
[ ] Solicité revisión de al menos 1 compañero
```

---

## Diagrama del Flujo Completo

```
main ──────────────────────────────────────────────► (stable)
  │                                        ▲
  │                              Pull Request (pendiente)
  │                                        │
  └─► rama-merge ──────────────────────────┘
        feat: docker-compose + env vars
        feat: ms1-pos movido a su carpeta
        feat: ms2-online ventas online (Singleton)
        feat: orq consolida MS1+MS2 en paralelo
        chore: limpieza de archivos no usados
        refactor: filtrado en orq, MS solo almacenan
```

## Estado actual del proyecto (EP2)

| Componente | Carpeta | Puerto | Estado |
|---|---|---|---|
| Ventas POS | `ms1-pos/` | 8081 | Completo |
| Ventas Online | `ms2-online/` | 8083 | Completo |
| Orquestador | `orq-service/` | 8082 | Completo |
| BFF | `bff-service/` | 8080 | Completo |
| Frontend | `frontend-app/` | 3000 | Completo |
| Infraestructura | `docker-compose.yml` | — | Completo |
| Simulador POS | `simulador-pos.ps1` | — | Completo |
| Simulador Online | `simulador-online.ps1` | — | Completo |

---

## Referencias

- GitHub. (2024). *GitHub Flow.* docs.github.com/get-started/using-github/github-flow
- Conventional Commits. (2024). *Specification v1.0.0.* conventionalcommits.org
