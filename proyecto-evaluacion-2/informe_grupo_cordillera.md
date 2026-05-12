# Grupo Cordillera — Plataforma de Monitoreo Inteligente para el Desempeño Organizacional

**INSTITUTO PROFESIONAL DUOC UC**  
Escuela de Informática y Telecomunicaciones

**Evaluación Parcial 1 — Diseño de Arquitectura de Microservicios**  
DSY1106 · Desarrollo Fullstack III

**Integrantes:** Francisca Barrera · Ignacio Sorko · Patricio Zapata  
**Docente:** Alejandro Sepúlveda  
**Fecha:** 09 de abril de 2026

---

## Índice

1. [Introducción](#1-introducción)
   - 1.1. Contexto de la organización
   - 1.2. Problemática actual
   - 1.3. Objetivo de la solución
2. [Selección de patrones de arquitectura](#2-selección-de-patrones-de-arquitectura)
   - 2.1. Patrones seleccionados
   - 2.2. Justificación de patrones según requerimientos del cliente
   - 2.3. Ejemplo de aplicación combinada
3. [Herramientas y estrategias de implementación](#3-herramientas-y-estrategias-de-implementación)
   - 3.1. Stack tecnológico seleccionado
   - 3.2. Justificación y aporte a la eficiencia técnica y operativa
4. [Arquitectura de microservicios propuesta](#4-arquitectura-de-microservicios-propuesta)
   - 4.1. Diagrama de Arquitectura
   - 4.2. Descripción de componentes
5. [Consideraciones de seguridad, privacidad y sostenibilidad](#5-consideraciones-de-seguridad-privacidad-y-sostenibilidad)
   - 5.1. Seguridad
   - 5.2. Privacidad
   - 5.3. Sostenibilidad
6. [Evaluación del diseño propuesto](#6-evaluación-del-diseño-propuesto)
   - 6.1. Cumplimiento de requerimientos funcionales
   - 6.2. Cumplimiento de especificaciones técnicas
7. [Conclusión](#7-conclusión)
8. [Referencias](#8-referencias)

---

## 1. Introducción

### 1.1. Contexto de la organización

Grupo Cordillera es una empresa del sector retail y comercialización de productos para el hogar y tecnología, con presencia en diversas ciudades del país a través de múltiples sucursales. Para gestionar sus procesos utiliza cinco sistemas independientes: sistema de punto de venta, plataforma de comercio electrónico, sistema de gestión de inventarios, herramientas de gestión financiera y sistema de atención al cliente.

### 1.2. Problemática actual

La información dispersa en cinco plataformas independientes impide obtener una visión integral del desempeño organizacional. La generación de reportes requiere procesos manuales: extracción desde distintos sistemas, consolidación en hojas de cálculo, validación manual de indicadores y preparación de reportes para reuniones ejecutivas. Este flujo genera retrasos, errores de consolidación, falta de acceso en tiempo real y alta dependencia de procesos manuales.

### 1.3. Objetivo de la solución

El objetivo principal es desarrollar una plataforma basada en microservicios que centralice la información de los cinco sistemas independientes del Grupo Cordillera, permitiendo el monitoreo del desempeño organizacional en tiempo real.

Para lograr esto, la solución se estructura en tres módulos funcionales:

- **Gestión de datos organizacionales:** Centralizar y disponer la información proveniente de las diversas áreas del negocio (ventas, inventario, finanzas, etc.).
- **Gestión de indicadores (KPI):** Automatizar la definición y el análisis de indicadores estratégicos, permitiendo una evaluación rápida del cumplimiento de metas mediante visualizaciones intuitivas.
- **Visualización de reportes:** Implementar un panel de control dinámico para la alta dirección que facilite la generación de reportes ejecutivos y la toma de decisiones basada en datos confiables.

---

## 2. Selección de patrones de arquitectura

### 2.1. Patrones seleccionados

La arquitectura propuesta implementa los siguientes patrones, seleccionados para resolver los desafíos de integración, disponibilidad y escalabilidad del Grupo Cordillera:

- **API Gateway Pattern:** Punto de entrada único que centraliza la autenticación mediante tokens JWT, la autorización basada en roles (RBAC) y el enrutamiento hacia el backend. Implementa Rate Limiting para proteger la infraestructura y logging centralizado para auditoría de accesos.

- **Backend for Frontend (BFF):** Capa de orquestación que consolida las respuestas de los distintos módulos en un único JSON optimizado para el dashboard. Implementa una estrategia de Calculation Caching en Redis, almacenando KPIs y reportes ya procesados para reducir la latencia y evitar la re-ejecución de lógica de negocio costosa.

- **Orchestrator Pattern:** Tres orquestadores especializados (ORQ-DATOS, ORQ-IND, ORQ-REP) que gestionan la lógica de negocio y las configuraciones de sus respectivos dominios. El ORQ-IND, en particular, actúa como el sistema de registro (System of Record) para las definiciones, fórmulas y umbrales de los indicadores estratégicos.

- **Circuit Breaker Pattern:** Implementado con Resilience4j para gestionar fallos en la comunicación con sistemas externos. Ante una falla, el circuito activa un Fallback síncrono que recupera el último estado conocido desde la Base de Datos del orquestador o Redis, garantizando la disponibilidad del servicio.
  > **Nota de Integridad:** Siguiendo el marco Ethically Aligned Design (IEEE), la respuesta bajo este estado incluye una bandera de **Stale Data** (dato desfasado) y un timestamp de origen para garantizar la transparencia en la toma de decisiones.

- **Change Data Capture (CDC):** Implementado mediante Debezium Server, este patrón permite capturar las inserciones y actualizaciones directamente desde los logs de las 5 bases de datos externas (Punto de Venta, Inventario, etc.).

- **Desacoplamiento mediante Colas (Ingesta):** Se integran 5 colas de mensajes (MQ1 a MQ5) que actúan como buffer entre los sistemas legados y la plataforma, garantizando que no se pierdan datos durante picos de tráfico.

- **External Authentication Service:** Se implementa un microservicio dedicado (MS AUTH) que centraliza la gestión de identidad y persistencia de usuarios en una base de datos independiente (BD USUARIO), permitiendo que el API Gateway delegue la validación de credenciales y la emisión de tokens JWT.

- **Message Queue Pattern (Auditoría):** Colas de mensajes (MQ-BDAUD-1, MQ-BDAUD-2, MQ-BDAUD-3) utilizadas para el desacoplamiento asíncrono de procesos de auditoría. Este patrón registra tanto los fallos detectados por el Circuit Breaker como las modificaciones manuales de umbrales realizadas por los analistas, asegurando un rastro de auditoría inalterable sin bloquear la experiencia del usuario.

- **Anti-Corruption Layer (ACL):** Cinco microservicios de integración (MS1-MS5) que actúan como traductores. Operan como consumidores asíncronos de los eventos capturados por el patrón CDC, procesando la información proveniente de las colas MQ1-MQ5 para mantener actualizada la capa de persistencia interna.

- **Factory Method Pattern:** Aplicado en ORQ-IND y ORQ-REP para la creación dinámica de distintos tipos de KPIs y formatos de reportes sin modificar el código existente, cumpliendo con el principio de abierto-cerrado.

- **Repository Pattern:** Abstrae el acceso a datos en cada microservicio mediante Spring Data JPA, desacoplando la lógica de negocio de la persistencia física en PostgreSQL.

- **Database per Service Pattern:** Cada microservicio, orquestador y componente de auditoría posee su propia base de datos PostgreSQL. Esto asegura que las configuraciones maestras de los KPIs estén aisladas y protegidas, permitiendo un escalado independiente y mayor seguridad.

- **Micro Frontend Pattern:** La capa WEB se divide en tres módulos independientes (GESTION-DATOS-WEBMF, GESTION-INDICADORES-WEBMF y VISUAL-REPORT-WEBMF) cargados dinámicamente mediante Module Federation. Esto permite actualizaciones y despliegues independientes para cada área funcional del sistema.

---

### 2.2. Justificación de patrones según requerimientos del cliente

| Patrón | Requerimiento que resuelve | Justificación |
|---|---|---|
| API Gateway | Acceso seguro y centralizado | Centraliza la validación de JWT y el control de acceso basado en roles (RBAC), evitando duplicar lógica de seguridad en cada microservicio. |
| External Auth Service (MS AUTH) | Gestión centralizada de identidad y seguridad | Separa la lógica de autenticación y el almacenamiento de usuarios (BD USUARIO) del resto del sistema. Permite una gestión de roles (RBAC) más limpia y escalable que no sobrecarga al API Gateway. |
| BFF + Redis | Visualización rápida consolidada | El BFF orquesta llamadas en paralelo a los servicios. Redis implementa Calculation Caching para entregar KPIs ya procesados, eliminando latencias de recálculo innecesarias. |
| Orchestrator Pattern | Centralización de lógica por dominio | Permite que cada módulo (Datos, KPIs, Reportes) gestione sus propias reglas y estados, actuando como el "dueño" de la lógica de negocio de cada funcionalidad. |
| ACL (MS1-MS5) | Integración con sistemas heterogéneos | Protege la integridad de la plataforma mediante una capa de traducción. Su enfoque como consumidor de los eventos de cambio provenientes de las colas MQ1-MQ5 permite sincronizar los datos en "tiempo real" sin saturar los microservicios orquestadores. |
| Change Data Capture (CDC) | Sincronización con los sistemas externos sin afectar el rendimiento legacy | Permite capturar cambios directamente desde los logs de las 5 bases de datos externas mediante Debezium Server. Esto evita realizar consultas pesadas (polling) que podrían degradar el rendimiento de los sistemas externos actuales. |
| Factory Method | Gestión de diversos KPIs y formatos | Permite que el ORQ-IND y ORQ-REP generen nuevos tipos de indicadores y reportes de forma dinámica sin modificar el código base, facilitando la escalabilidad. |
| Circuit Breaker + BFF | Disponibilidad y ética en los datos | Ante fallos, activa un fallback síncrono consultando el estado anterior en Redis. Se garantiza la transparencia informando la vigencia (timestamp) del dato. |
| Message Queue (Auditoría: MQ-BDAUD 1-3) | Centralización de logs y auditoría técnica | Permite que los orquestadores reporten fallos de integración y cambios administrativos de forma asíncrona. El Auditory Consumer procesa estos mensajes y los persiste en la BDF, creando un historial centralizado de incidencias para soporte técnico sin degradar la experiencia del ejecutivo. |
| Messaging (MQ1-MQ5) | Desacoplamiento de la ingesta y tolerancia a fallos | Actúa como un buffer de persistencia temporal entre los sistemas externos y los microservicios de integración. Si hay un pico de ventas, las colas gestionan la carga para que los MS1-MS5 procesen la información a su propio ritmo sin pérdida de datos. |
| Repository Pattern | Abstracción de la persistencia | Desacopla la lógica de negocio de la tecnología de base de datos específica. Utiliza Spring Data JPA para proporcionar una interfaz uniforme de acceso a datos. |
| Database per Service | Escalabilidad e independencia | Cada componente posee su propia base de datos. El ORQ-IND resguarda ahí las configuraciones y metas estratégicas de la organización. |
| Micro Frontend | Módulos independientes en el frontend | Permite que cada área funcional (Datos, KPIs, Reportes) sea desarrollada, versionada y desplegada de forma independiente en el contenedor principal. |

---

### 2.3. Ejemplo de aplicación combinada

Para ilustrar el funcionamiento de la arquitectura, se describe el flujo de una consulta de KPI de Ventas por parte de un ejecutivo:

1. **Acceso y Seguridad:** El ejecutivo accede al dashboard desde el microfrontend GESTION-INDICADORES-WEBMF. La petición HTTPS llega al API Gateway, el cual delega la validación de identidad al microservicio MS AUTH. Este verifica las credenciales en la BD USUARIO y confirma el token JWT contra Redis, validando que el usuario posee el rol de "ejecutivo" mediante RBAC.

2. **Optimización en BFF:** El Gateway enruta la solicitud al BFF. Este componente primero verifica en Redis si el KPI solicitado ya fue calculado recientemente (Calculation Caching). Ante un cache-miss, el BFF inicia llamadas en paralelo a los orquestadores necesarios utilizando CompletableFuture.

3. **Orquestación y Lógica:** El orquestador ORQ-IND recibe la instrucción y utiliza el Factory Method para instanciar la lógica del KPI de ventas. Consulta a los microservicios de integración MS1 (Punto de Venta) y MS3 (E-commerce), los cuales mantienen datos actualizados en tiempo real gracias a la ingesta continua de Debezium y las colas MQ1/MQ3.

4. **Resiliencia (Circuit Breaker):** Si el MS3 no responde, el nodo FALLA (Resilience4j) detecta la anomalía y activa un método de fallback síncrono. Este método recupera el último valor válido desde la capa de persistencia interna (Redis o base de datos del orquestador), añadiendo una bandera de Stale Data y un timestamp para informar al usuario sobre la vigencia del dato.

5. **Auditoría Asíncrona:** Mientras se entrega la respuesta al usuario, el orquestador envía un mensaje a la cola MQ-BDAUD-2. El Auditory Consumer procesa este mensaje y registra el fallo técnico en la BDF, permitiendo que el equipo de TI analice la caída del sistema externo sin haber bloqueado la experiencia del ejecutivo.

6. **Visualización Final:** El BFF consolida las respuestas de todos los orquestadores y devuelve un único JSON al microfrontend, el cual renderiza el indicador con su respectivo semáforo y la advertencia de transparencia correspondiente.

---

## 3. Herramientas y estrategias de implementación

### 3.1. Stack tecnológico seleccionado

| Tecnología | Rol en la arquitectura |
|---|---|
| Spring Boot (Java) | Framework base para el desarrollo de microservicios, orquestadores, el BFF y el MS AUTH, garantizando un ecosistema robusto y escalable. |
| Spring Cloud Gateway | Implementación del API Gateway. Encargado de la seguridad transversal (JWT), autorización (RBAC), rate limiting y enrutamiento. |
| Resilience4j | Implementación del patrón Circuit Breaker. Gestiona la tolerancia a fallos y activa los mecanismos de fallback síncronos hacia Redis. |
| Spring Data JPA | Implementación del Repository Pattern para la persistencia en PostgreSQL, facilitando el acceso a datos mediante abstracción de interfaces. |
| Debezium Server | Agente de Change Data Capture (CDC). Monitorea los logs de las 5 bases de datos externas para capturar cambios y publicarlos como eventos en RabbitMQ. |
| RabbitMQ | Message Broker para la comunicación asíncrona. Soporta tanto la ingesta de datos (MQ1-MQ5) como las colas de auditoría y logs (MQ-BDAUD 1-3). |
| GitLab | Plataforma integral para el control de versiones (Git) y gestión del flujo de trabajo mediante Merge Requests. |
| GitLab CI/CD | Motor de automatización nativo para el Pipeline de integración y despliegue continuo, gestionando pruebas unitarias y empaquetado. |
| React.js + Module Federation | Stack de frontend para el desarrollo de Micro Frontends independientes, empaquetados y desplegados mediante módulos NPM. |
| PostgreSQL | BD relacional dedicada para cada microservicio, orquestador, el registro central de auditoría (BDF) y el servicio de autenticación (BD USUARIO). |
| Redis | Caché distribuida de alta velocidad. Utilizada para Calculation Caching de KPIs, sesiones JWT y almacenamiento de estados de fallback. |
| Docker | Contenedorización de todos los componentes para asegurar la paridad de entornos y facilitar el escalamiento horizontal. |
| SonarQube / SAST | Herramientas de análisis estático integradas en el pipeline de GitLab para garantizar la calidad del código y detectar vulnerabilidades. |
| Maven | Herramienta de gestión de dependencias y estandarización de la estructura de proyectos y procesos de construcción. |

---

### 3.2. Justificación y aporte a la eficiencia técnica y operativa

La elección de las herramientas responde a la necesidad de construir una plataforma de clase empresarial que sea resiliente, fácil de mantener y que garantice la integridad de los datos estratégicos del Grupo Cordillera.

**1. Backend, Seguridad y Capa de Microservicios (Ecosistema Spring):**

- **Spring Boot:** Se seleccionó por su capacidad de auto-configuración y madurez, reduciendo el Time-to-Market. Sustenta no solo a los orquestadores sino también al MS AUTH, permitiendo una gestión de identidad centralizada y escalable.
- **Spring Cloud Gateway:** Centraliza el acceso de los tres microfrontends. Actúa como el primer muro de defensa delegando la autenticación al MS AUTH y aplicando Rate Limiting para proteger la lógica interna.
- **Resilience4j:** Fundamental para la alta disponibilidad. Permite reaccionar ante fallos en los microservicios de integración (MS1-MS5) activando fallbacks hacia la caché de Redis.
- **Spring Data JPA:** Implementa el Repository Pattern, permitiendo al equipo trabajar con objetos de Java en lugar de consultas SQL complejas, lo que facilita enormemente el mantenimiento del código.

**2. Gestión de Datos, Rendimiento e Ingesta:**

- **PostgreSQL:** Elegida por su robustez en la integridad referencial. Se aplica el patrón Database per Service, asignando bases de datos independientes para el MS AUTH (BD USUARIO), los orquestadores (BD DATOS, BD IND, BD REP) y el historial técnico (BDF).
- **Debezium Server (CDC):** Se integra para automatizar la captura de cambios en las 5 fuentes externas sin realizar consultas invasivas (polling). Garantiza que la plataforma trabaje con datos frescos en "casi tiempo real" sin degradar el rendimiento de los sistemas legados.
- **Redis (Calculation Caching):** Es la pieza clave para la rapidez del dashboard. Al almacenar KPIs ya procesados y sesiones de usuario, asegura respuestas en milisegundos incluso bajo alta demanda.

**3. Frontend y Escalabilidad:**

- **React.js + Module Federation:** Permite desarrollar y desplegar los módulos de Gestión de Datos, KPIs y Reportes de forma independiente. Esto reduce el riesgo de que un cambio en un área afecte la estabilidad global del sistema.

**4. Integración, Despliegue y Calidad (GitLab Ecosystem):**

- **GitLab & GitLab CI/CD:** La unificación del código y el pipeline en una sola plataforma elimina la complejidad operativa y permite una trazabilidad total del software.
- **SonarQube / SAST:** Garantiza que el código cumpla con estándares de seguridad y mantenga una cobertura de pruebas unitarias superior al 60%.
- **Maven:** Estandariza la estructura de los proyectos y gestiona las dependencias para asegurar la paridad entre los desarrolladores.
- **Docker:** Garantiza la paridad de entornos, asegurando que la aplicación funcione de forma idéntica en desarrollo, pruebas y producción.

**5. Comunicación Asíncrona y Eficiencia de Costos:**

- **RabbitMQ:** Su uso se justifica por su alta eficiencia y bajo consumo de recursos en comparación con otras soluciones de mensajería masiva. En esta arquitectura cumple un doble rol crítico:
  - **Ingesta de Datos:** Gestiona las colas MQ1-MQ5 para recibir eventos de Debezium, actuando como buffer ante picos de tráfico de los sistemas externos.
  - **Auditoría Técnica:** Canaliza los logs de error a través de las colas MQ-BDAUD 1-3 hacia la BDF de forma asíncrona, sin interferir en la experiencia del usuario.
  > **Nota:** La combinación de Debezium Server + RabbitMQ permite implementar una arquitectura reactiva de alto nivel con un costo operativo significativamente menor al de infraestructuras basadas en Kafka.

---

## 4. Arquitectura de microservicios propuesta

### 4.1. Diagrama de Arquitectura

> *(El diagrama original se encuentra en el documento PDF de referencia, página 11)*

---

### 4.2. Descripción de componentes

La solución se desglosa en componentes modulares que interactúan bajo una arquitectura de microservicios, asegurando alta disponibilidad y escalabilidad.

**A. Capa de Presentación (Micro Frontends):**

- **GESTION-DATOS-WEBMF:** Módulo encargado de la visualización y auditoría de registros consolidados, permitiendo revisar la integridad de los datos de las 5 fuentes externas.
- **GESTION-INDICADORES-WEBMF:** Interfaz para la configuración estratégica de fórmulas y umbrales de KPIs con visualización en tiempo real.
- **VISUAL-REPORT-WEBMF:** Panel de control especializado en la generación de reportes ejecutivos y comparativas históricas para la alta dirección.

**B. Capa de Acceso y Orquestación:**

- **API Gateway (Spring Cloud Gateway):** Punto único de entrada que gestiona el enrutamiento y el Rate Limiting. Delega la validación de identidad al MS AUTH y verifica tokens contra Redis.
- **MS AUTH:** Microservicio dedicado exclusivamente a la autenticación y gestión de identidad. Utiliza la BD USUARIO para la persistencia de credenciales y perfiles de acceso.
- **BFF (Backend for Frontend):** Agregador de servicios que implementa Calculation Caching en Redis para optimizar la entrega de datos al dashboard sin re-ejecutar lógica de negocio costosa.

**C. Capa de Lógica de Negocio (Orquestadores):**

- **ORQ-DATOS:** Especializado en la recolección y limpieza de datos provenientes de la capa de integración. Registra los estados de consolidación en su propia base de datos (BD DATOS).
- **ORQ-IND:** El "cerebro" estratégico que aplica el patrón Factory Method para calcular KPIs. Resguarda los umbrales maestros y metas organizacionales en la BD IND.
- **ORQ-REP:** Generador de reportes complejos que cruza múltiples dominios y gestiona la lógica de comparación temporal. Utiliza la BD REP para su persistencia local.

**D. Capa de Integración y Captura de Datos (CDC + ACL):**

- **Debezium Server:** Agente de Change Data Capture (CDC) que monitorea los logs de las 5 bases de datos externas para capturar inserciones y actualizaciones de forma no invasiva.
- **Colas de Ingesta (MQ1 - MQ5):** Actúan como buffer asíncrono, recibiendo los eventos desde Debezium y distribuyéndolos hacia la capa de microservicios.
- **Microservicios de Integración (MS1 - MS5):** Actúan como consumidores de las colas MQ1-MQ5, traduciendo los modelos legacy al lenguaje común de la plataforma mediante una capa Anti-Corrupción. Cada uno posee su propia base de datos local (BD1 - BD5) para persistir los estados de integración.

**E. Capa de Soporte y Persistencia:**

- **Bases de Datos Locales (PostgreSQL):** Se implementa el patrón Database per Service, asegurando el aislamiento total entre los dominios de usuario (BD USUARIO), orquestación (BD DATOS, BD IND, BD REP) e integración (BD1 - BD5).
- **Caché Distribuida (Redis):** Almacena sesiones, tokens de seguridad y la capa de KPIs pre-calculados para garantizar tiempos de respuesta inferiores a 200ms.
- **Message Broker (RabbitMQ):** Gestiona tanto la ingesta de datos como las colas de auditoría técnica (MQ-BDAUD-1, MQ-BDAUD-2 y MQ-BDAUD-3) que registran eventos de fallo y cambios administrativos.
- **Auditory Consumer & BDF:** Componente encargado de persistir los eventos de las colas de auditoría en la BDF (Base de Datos de Fallback), asegurando un historial inalterable de incidencias técnicas.

---

## 5. Consideraciones de seguridad, privacidad y sostenibilidad

La estrategia transversal del proyecto asegura que la plataforma no solo sea resistente a ataques, sino también ética en el manejo de la información y eficiente en el uso de recursos.

### 5.1. Seguridad

Se implementa un modelo de **Defensa en Profundidad** para proteger la infraestructura y la lógica de negocio:

- **Gestión de Identidad (JWT + RBAC):** La autenticación es centralizada mediante un microservicio dedicado (MS AUTH) con su propia base de datos (BD USUARIO). La validación es stateless mediante tokens JWT validados en el API Gateway. La autorización se basa en roles (Ejecutivo, Analista, Administrador) para asegurar el principio de menor privilegio.
- **Seguridad en el Código (DevSecOps):** Integración de SonarQube y herramientas de SAST en el pipeline de GitLab CI/CD. Se exige una cobertura mínima del 60% y la ausencia de vulnerabilidades críticas (OWASP Top 10) antes de cualquier despliegue.
- **Aislamiento de Infraestructura:** El uso de Docker permite que los microservicios operen en redes privadas virtuales, exponiendo únicamente el Gateway al tráfico exterior por HTTPS.

### 5.2. Privacidad

Siguiendo los principios de **Ethically Aligned Design (IEEE)**, la plataforma garantiza que los datos estratégicos sean tratados con transparencia:

- **Transparencia de Datos (Stale Data Flag):** En cumplimiento con el derecho a la información veraz, el sistema identifica explícitamente cuando un dato proviene de la caché (Redis) ante una falla de integración, informando al usuario la vigencia exacta (timestamp) de la información.
- **Trazabilidad e Inalterabilidad:** El uso de las colas de auditoría (MQ-BDAUD 1-3) asegura que cada fallo técnico o cambio manual en los umbrales de los KPIs sea registrado de forma asíncrona en la BDF. Esto garantiza que la dirección pueda auditar el origen y modificación de reglas de negocio, evitando manipulaciones de información sensible.
- **Cifrado de Información:** Los datos en tránsito viajan cifrados mediante TLS 1.2, y las credenciales de acceso a las 5 bases de datos externas se gestionan de forma segura mediante variables de entorno, nunca en el código fuente.

### 5.3. Sostenibilidad

La sostenibilidad del software se aborda desde la eficiencia técnica y la capacidad de evolución del sistema a largo plazo:

- **Eficiencia de Recursos (CDC + Caching):** El uso de Debezium Server y RabbitMQ permite una captura de datos en tiempo real con una huella computacional menor en comparación con infraestructuras más pesadas, optimizando el consumo de energía. Complementariamente, Redis reduce la carga al evitar cálculos constantes de KPIs.
- **Escalabilidad Horizontal:** Gracias a Docker, el sistema permite escalar de forma independiente los módulos bajo alta demanda (como el orquestador de KPIs o los servicios de integración durante picos de ingesta) sin desperdiciar recursos en componentes inactivos.
- **Sostenibilidad del Desarrollo (Mantenibilidad):** La arquitectura de Micro Frontends y el uso de GitLab CI/CD aseguran que el sistema sea fácil de mantener por equipos pequeños. La automatización de pruebas reduce el error humano y garantiza que la plataforma pueda evolucionar sin volverse obsoleta.

---

## 6. Evaluación del diseño propuesto

### 6.1. Cumplimiento de requerimientos funcionales

- **Gestión de Datos Organizacionales** *(Debezium + MQ1-MQ5 + MS1-MS5 + ORQ-DATOS):* La arquitectura garantiza la captura automática de cambios en las 5 fuentes externas mediante el patrón CDC, eliminando la necesidad de procesos manuales de extracción. El orquestador de datos permite visualizar estos registros ya consolidados y traducidos.

- **Gestión de Indicadores (KPI)** *(ORQ-IND + GESTION-INDICADORES-WEBMF):* Permite la definición dinámica de KPIs mediante el patrón Factory Method. El orquestador gestiona umbrales y metas en su propia base de datos (BD IND), permitiendo ajustes estratégicos sin afectar la operación.

- **Visualización de Reportes** *(ORQ-REP + BFF + VISUAL-REPORT-WEBMF):* Proporciona un panel de control para la alta dirección que cruza datos de múltiples dominios, generando reportes ejecutivos de alta fidelidad optimizados por la caché de Redis.

### 6.2. Cumplimiento de especificaciones técnicas

| Especificación Técnica | Mecanismo de Implementación | Resultado Esperado |
|---|---|---|
| Alta Disponibilidad | Circuit Breaker (Resilience4j) + Redis | Ante fallos de sistemas externos, el sistema activa un fallback automático hacia la caché, garantizando que el dashboard nunca deje de mostrar información. |
| Seguridad y Control de Acceso | API Gateway + MS AUTH + JWT + RBAC | Se centraliza la seguridad en un microservicio dedicado. Solo usuarios autenticados con roles específicos (ej. Ejecutivo) acceden a datos sensibles. |
| Sincronización de Datos | Debezium Server (CDC) + RabbitMQ (MQ1-5) | Captura de cambios en "casi tiempo real" desde las fuentes legacy sin degradar el rendimiento de las bases de datos externas de la empresa. |
| Integridad y Auditoría | RabbitMQ (MQ-BDAUD 1-3) + BDF | Cada cambio en un KPI o error crítico se registra de forma asíncrona en la BDF, garantizando un historial inalterable para auditorías técnicas y administrativas. |
| Rendimiento y Latencia | Redis (Calculation Caching) | Al pre-calcular los KPIs y almacenarlos en caché, se logran tiempos de respuesta inferiores a 200ms, evitando consultas pesadas a los sistemas legacy. |
| Transparencia Ética | Flag Stale Data (Diseño Ético) | El sistema informa explícitamente al usuario cuando los datos están desfasados mediante un timestamp, cumpliendo con estándares de transparencia en la toma de decisiones. |
| Ciclo de Vida (CI/CD) | GitLab CI/CD + Docker | Se garantiza la paridad de entornos y se automatiza la calidad del código, asegurando una cobertura de pruebas unitarias superior al 60%. |

---

## 7. Conclusión

La arquitectura de microservicios propuesta para el Grupo Cordillera transforma un escenario crítico de datos dispersos en sistemas legacy y reportes manuales propensos al error, en una plataforma consolidada, resiliente y de alto rendimiento para el monitoreo estratégico en tiempo real. La solución no solo centraliza la información, sino que garantiza su integridad y disponibilidad mediante un diseño desacoplado y moderno.

La integración de patrones como API Gateway, BFF, ACL y el flujo de CDC (Change Data Capture) mediante Debezium Server representa una respuesta directa a los desafíos de interoperabilidad del caso. El mecanismo de resiliencia, sustentado por el Circuit Breaker y el Calculation Caching en Redis, asegura que la alta dirección nunca pierda el acceso a la información; ante fallos externos, el sistema entrega proactivamente datos bajo una bandera de transparencia (Stale Data). Por otro lado, el uso estratégico de RabbitMQ para la ingesta asíncrona (MQ1-MQ5) y la auditoría técnica (MQ-BDAUD 1-3) garantiza un sistema altamente escalable y un rastro de auditoría inalterable en la BDF.

La seguridad se ve robustecida con la implementación de un servicio de identidad centralizado (MS AUTH) y su propia base de datos (BD USUARIO), permitiendo una gestión de acceso basada en roles (RBAC) mucho más limpia. En el frontend, la adopción de Micro Frontends mediante Module Federation elimina los cuellos de botella en el desarrollo, permitiendo que cada área de negocio evolucione de forma independiente. Asimismo, la unificación del ciclo de vida en GitLab CI/CD y el análisis de SonarQube garantizan una cobertura de pruebas superior al 60% y una entrega libre de vulnerabilidades críticas.

Finalmente, la alineación con marcos éticos como el IEEE Ethically Aligned Design y la apuesta por la sostenibilidad técnica —al elegir tecnologías eficientes en costos y recursos como Debezium Server y RabbitMQ frente a infraestructuras más pesadas— aseguran que el Grupo Cordillera cuente con una solución no solo técnicamente robusta, sino también responsable, transparente y viable a largo plazo en un entorno empresarial competitivo.

---

## 8. Referencias

- Atlassian. (2024). *GitFlow Workflow*. Recuperado de https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow
- Brown, S. (2022). *The C4 Model for Software Architecture*. Recuperado de https://c4model.com
- Debezium. (2024). *Debezium Documentation*. Recuperado de https://debezium.io/documentation/reference/stable/
- Docker Inc. (2024). *Docker Documentation*. Recuperado de https://docs.docker.com
- Fowler, M. (2002). *Patterns of Enterprise Application Architecture*. Addison-Wesley.
- Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley.
- GitLab. (2024). *GitLab Documentation and CI/CD Guide*. Recuperado de https://docs.gitlab.com/
- IEEE. (2017). *Ethically Aligned Design: A Vision for Prioritizing Human Well-being with Autonomous and Intelligent Systems*. IEEE.
- Newman, S. (2021). *Building Microservices* (2nd ed.). O'Reilly Media.
- PostgreSQL Global Development Group. (2024). *PostgreSQL Documentation*. Recuperado de https://www.postgresql.org/docs/
- RabbitMQ. (2024). *RabbitMQ Documentation*. Recuperado de https://www.rabbitmq.com/documentation.html
- React. (2024). *React Documentation and Module Federation Guide*. Recuperado de https://react.dev
- Redis Inc. (2024). *Redis Documentation*. Recuperado de https://redis.io/docs
- Resilience4j. (2024). *Resilience4j Documentation*. Recuperado de https://resilience4j.readme.io
- Richardson, C. (2018). *Microservices Patterns*. Manning Publications.
- SonarSource. (2024). *SonarQube Documentation*. Recuperado de https://docs.sonarsource.com/sonarqube/
- Spring. (2024). *Spring Boot Reference Documentation*. Recuperado de https://spring.io/projects/spring-boot
- Spring. (2024). *Spring Cloud Gateway*. Recuperado de https://spring.io/projects/spring-cloud-gateway
