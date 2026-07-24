# T4KASH

T4KASH es una aplicación móvil orientada al networking universitario y a la gestión de oportunidades flexibles para estudiantes. Conecta estudiantes con personas, empresas o instituciones que necesitan apoyo en microtrabajos, tutorías, proyectos, servicios, prácticas o voluntariados.

El proyecto se desarrolla como MVP para un hackathon. Su prioridad es demostrar un flujo funcional de publicación, exploración, postulación, asignación y entrega, respaldado por una arquitectura que pueda ejecutarse tanto localmente como en servicios gratuitos de nube.

## Problema

Muchos estudiantes necesitan generar ingresos o experiencia mientras estudian, pero sus horarios no siempre les permiten aceptar empleos tradicionales. Al mismo tiempo, quienes necesitan ayuda puntual suelen recurrir a canales informales que presentan problemas como:

- Perfiles difíciles de verificar.
- Falta de reputación e historial profesional.
- Poco seguimiento de tareas, entregas y pagos.
- Riesgo de estafas o incumplimientos.
- Dificultad para organizar postulaciones.

T4KASH centraliza estas interacciones en un flujo trazable y enfocado en oportunidades universitarias. No pretende funcionar como una red social ni incluir un feed de publicaciones.

## Flujo Principal

1. Un cliente publica una tarea u oportunidad.
2. La tarea queda disponible en el marketplace.
3. Si es presencial o híbrida, también aparece en el mapa.
4. Los estudiantes revisan y se postulan.
5. El cliente acepta o rechaza postulaciones.
6. Una postulación aceptada se convierte en trabajo asignado.
7. El estudiante registra una entrega.
8. El cliente revisa y aprueba el trabajo.

## Estado del MVP

| Componente | Estado |
|---|---|
| API Spring Boot desplegada en Render | Implementado |
| PostgreSQL administrado en Supabase | Implementado |
| Documentación Swagger/OpenAPI | Implementado |
| Marketplace y detalle de oportunidades en Android | Implementado |
| Publicación de tareas desde Android | Implementado |
| Ubicación para tareas presenciales e híbridas | Implementado |
| Mapa con MapLibre y OpenFreeMap | Implementado |
| Postulaciones, asignaciones y entregas en la API | Implementado |
| Navegación, carga y manejo visual de errores | Implementado |
| Autenticación y sesiones reales | Pendiente |
| Mensajería, pagos y notificaciones push | Pendiente |

La versión actual usa usuarios demo para validar el flujo. En particular, la publicación desde Android utiliza temporalmente `idCliente = 1` hasta integrar autenticación real.

## Tecnologías

| Capa | Tecnología |
|---|---|
| Aplicación móvil | Kotlin, Android Studio, Jetpack Compose |
| Navegación móvil | Navigation Compose |
| Consumo de API | Retrofit, Gson, OkHttp |
| Mapas | MapLibre Compose, OpenFreeMap |
| Backend | Java 21, Spring Boot, Spring Data JPA |
| API | REST, Swagger / OpenAPI |
| Base de datos | PostgreSQL 16 |
| Entorno local | Docker Compose |
| Hosting | Render, Supabase |
| Diseño | Figma |
| Control de versiones | GitHub, Conventional Commits |

Las versiones concretas de las dependencias Android están centralizadas en `mobile/gradle/libs.versions.toml`. Las dependencias del backend se administran desde `backend/pom.xml`.

## Arquitectura

```text
Aplicación Android
   | \
   |  \ HTTPS
   |   +---------------------> OpenFreeMap
   |                            mapas y teselas
   |
   | HTTPS / REST
   v
API Spring Boot en Render
   |
   | JDBC / JPA + SSL
   v
PostgreSQL en Supabase
```

- Android nunca se conecta directamente a PostgreSQL.
- Las credenciales de Supabase solo se configuran en Render.
- La aplicación consulta y publica tareas mediante la API.
- OpenFreeMap proporciona el estilo y las teselas del mapa sin requerir una clave privada.
- El backend normaliza y valida los datos antes de persistirlos.

## Entornos

| Entorno | Uso | Estado |
|---|---|---|
| Local | Desarrollo con Docker Compose y Android Studio | Configurado |
| Demo | Android, API en Render y PostgreSQL en Supabase | Activo |
| Producción | Versión endurecida posterior al hackathon | Pendiente |

API publicada:

```text
https://t4k4sh.onrender.com/api/
```

Swagger:

```text
https://t4k4sh.onrender.com/swagger-ui/index.html
```

Health check:

```text
https://t4k4sh.onrender.com/api/health
```

El plan gratuito de Render puede suspender el servicio después de un periodo de inactividad. La primera solicitud puede tardar mientras la instancia vuelve a iniciar.

## Estructura del Repositorio

```text
T4k4sh/
  backend/
    src/main/             Código de la API
    src/test/             Pruebas automatizadas
    Dockerfile            Imagen del backend
    docker-compose.yml    API y PostgreSQL local
    pom.xml               Dependencias Maven
  mobile/
    app/src/main/         Aplicación Android
    gradle/               Catálogo de dependencias
  database/
    schema-postgresql.sql Esquema principal PostgreSQL
    sqlserver-original.sql Referencia histórica del modelo original
  docs/
    diagramas/            Diagramas de base de datos y UML
    deployment.md         Guía de Render y Supabase
  render.yaml             Configuración del servicio de Render
  README.md               Documentación principal
```

## Base de Datos

El modelo original fue diagramado en SQL Server y posteriormente migrado a PostgreSQL. La fuente oficial del esquema actual es:

```text
database/schema-postgresql.sql
```

El esquema contiene 34 tablas e incluye:

- Llaves primarias y foráneas.
- Restricciones únicas y validaciones.
- Índices para búsquedas frecuentes.
- Usuarios, roles y perfiles universitarios.
- Tareas, habilidades, postulaciones y trabajos.
- Entregas, pagos, conversaciones y reportes.
- Sesiones, verificaciones y auditoría.
- Datos demo para roles, universidad, carreras y categorías.

`database/sqlserver-original.sql` se conserva únicamente como referencia histórica y no debe utilizarse para Supabase.

### Ubicación de Tareas

La tabla `tareas` contiene:

| Campo | Uso |
|---|---|
| `modalidad` | `REMOTA`, `PRESENCIAL` o `HIBRIDA` |
| `direccion_referencia` | Descripción opcional del lugar |
| `latitud` | Coordenada entre `-90` y `90` |
| `longitud` | Coordenada entre `-180` y `180` |

Reglas del sistema:

- Una tarea `REMOTA` guarda dirección y coordenadas como `NULL`.
- Una tarea `PRESENCIAL` o `HIBRIDA` requiere latitud y longitud.
- La base exige que ambas coordenadas estén presentes o que ambas sean nulas.
- La ubicación actual del usuario no se almacena permanentemente.

El esquema completo contiene instrucciones `DROP TABLE` para recrear un entorno desde cero. No debe ejecutarse nuevamente sobre la base remota con información importante. Los cambios posteriores deben aplicarse mediante migraciones SQL controladas.

## Endpoints Implementados

| Método | Ruta | Uso |
|---|---|---|
| `GET` | `/api/health` | Verificar disponibilidad |
| `GET` | `/api/categories` | Listar categorías activas |
| `GET` | `/api/tasks` | Listar oportunidades |
| `POST` | `/api/tasks` | Crear una oportunidad |
| `GET` | `/api/tasks/{idTarea}` | Obtener detalle |
| `GET` | `/api/tasks/{idTarea}/applications` | Listar postulaciones |
| `POST` | `/api/tasks/{idTarea}/applications` | Crear postulación |
| `POST` | `/api/applications/{idPostulacion}/accept` | Aceptar postulación |
| `POST` | `/api/applications/{idPostulacion}/reject` | Rechazar postulación |
| `GET` | `/api/jobs` | Listar trabajos asignados |
| `GET` | `/api/jobs/{idTrabajo}/deliveries` | Listar entregas |
| `POST` | `/api/jobs/{idTrabajo}/deliveries` | Registrar entrega |
| `POST` | `/api/deliveries/{idEntrega}/approve` | Aprobar entrega |

Los endpoints de autenticación todavía no están implementados. El cliente y el estudiante se representan mediante IDs demo durante esta fase.

### Crear una Tarea Presencial

```json
{
  "titulo": "Apoyo durante un evento universitario",
  "descripcion": "Necesito apoyo con el registro de asistentes y organización del salón.",
  "presupuesto": 25.00,
  "fechaLimitePostulacion": null,
  "fechaLimite": null,
  "idCategoria": 4,
  "idCliente": 1,
  "tipoOportunidad": "TAREA",
  "modalidad": "PRESENCIAL",
  "visibilidad": "PUBLICA",
  "direccionReferencia": "Entrada principal del campus",
  "latitud": 12.114990,
  "longitud": -86.236170
}
```

Para una tarea remota se utiliza `"modalidad": "REMOTA"` y se omiten o envían como `null` los tres campos de ubicación.

## Aplicación Android

La aplicación utiliza Retrofit para consumir la API. La URL base se obtiene de:

1. La propiedad Gradle `T4KASH_API_BASE_URL`.
2. La variable de entorno `T4KASH_API_BASE_URL`.
3. La URL de Render como valor predeterminado.

Flujo actual de publicación:

1. Android carga las categorías desde `/api/categories`.
2. El usuario completa título, descripción y presupuesto.
3. Selecciona modalidad y categoría.
4. Para una tarea presencial o híbrida, concede el permiso y captura su ubicación.
5. Android envía `POST /api/tasks`.
6. La tarea creada se incorpora al estado del marketplace.
7. La aplicación abre el mapa y dibuja el marcador mediante GeoJSON.

Las tareas remotas se muestran en el marketplace, pero no generan marcadores.

## Ejecución Local

### Backend y PostgreSQL

Desde la raíz del repositorio:

```powershell
cd backend
docker compose up -d --build
```

Servicios locales:

```text
API:     http://localhost:8080/api/
Swagger: http://localhost:8080/swagger-ui/index.html
```

Comandos útiles:

```powershell
docker compose logs -f api
docker compose down
```

La primera creación del volumen PostgreSQL carga automáticamente `database/schema-postgresql.sql`.

### Android

Compilar usando la API publicada:

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

Compilar para consumir el backend local desde el emulador:

```powershell
.\gradlew.bat :app:assembleDebug -PT4KASH_API_BASE_URL=http://10.0.2.2:8080/api/
```

El APK debug se genera en:

```text
mobile/app/build/outputs/apk/debug/app-debug.apk
```

## Pruebas

Backend:

```powershell
cd backend
.\mvnw.cmd test
```

Android:

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

Las pruebas del backend cubren la normalización de modalidades, la eliminación de coordenadas en tareas remotas y la obligación de ubicación para tareas presenciales.

## Variables de Entorno

| Variable | Descripción | Ejemplo local |
|---|---|---|
| `SERVER_PORT` | Puerto de la API | `8080` |
| `SPRING_DATASOURCE_URL` | Conexión JDBC PostgreSQL | `jdbc:postgresql://db:5432/t4kash` |
| `SPRING_DATASOURCE_USERNAME` | Usuario PostgreSQL | `t4kash` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña PostgreSQL | `t4kash` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Estrategia de Hibernate | `validate` |
| `SPRING_DATASOURCE_MAX_POOL_SIZE` | Máximo de conexiones | `5` |
| `SPRING_DATASOURCE_MIN_IDLE` | Conexiones mínimas en reposo | `1` |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes permitidos | `*` durante la demo |
| `T4KASH_API_BASE_URL` | URL consumida por Android | URL de Render |

Las contraseñas, cadenas de conexión y claves privadas no deben guardarse en Git. Render administra las variables del backend y Android solo recibe la URL pública de la API.

## Despliegue

La guía ampliada se encuentra en `docs/deployment.md`.

Orden correcto para publicar cambios:

1. Aplicar primero cualquier migración necesaria en Supabase.
2. Subir el backend a GitHub.
3. Esperar que Render finalice el despliegue y muestre el servicio como `Live`.
4. Verificar `/api/health` y Swagger.
5. Compilar o ejecutar Android apuntando a Render.
6. Probar el flujo completo desde un dispositivo o emulador.

## Diseño y Diagramas

Prototipo de referencia:

[Figma - T4KASH](https://www.figma.com/design/k5PeSZUFQJgIja3Mw2BpAw/Sin-t%C3%ADtulo?node-id=0-1&t=mX1bigfYctB3XCdZ-0)

Los diagramas están ubicados en `docs/diagramas/` e incluyen:

- Diagrama entidad-relación.
- Diagrama de clases.
- Diagrama de actividades.
- Diagrama de casos de uso.

Los diagramas deben reflejar las coordenadas de `tareas` y diferenciar el flujo remoto del presencial o híbrido.

## Organización del Equipo

| Módulo | Tablas principales | Responsable |
|---|---|---|
| Identidad y Perfiles | usuarios, roles, sesiones, carreras, habilidades, verificaciones | Dev 1 |
| Marketplace Core | tareas, categorías, postulaciones, trabajos, entregas | Dev 2 |
| Social y Comunicación | mensajes, conversaciones, notificaciones, calificaciones, recomendaciones | Dev 1 |
| Finanzas y Sistema | pagos, transacciones, reportes, auditoría, archivos | Dev 2 |

## Control de Versiones

El proyecto utiliza ramas organizadas, Pull Requests y Conventional Commits.

Ejemplos:

```text
feat: conectar publicación de tareas con ubicación y mapa
fix: corregir validación de coordenadas
docs: actualizar documentación del despliegue
test: agregar pruebas del flujo marketplace
chore: ajustar configuración de render
```

## Próximos Pasos

1. Implementar autenticación real y eliminar los IDs demo.
2. Permitir seleccionar manualmente una ubicación en el mapa.
3. Abrir el detalle de una tarea al tocar su marcador.
4. Integrar almacenamiento de archivos con Supabase Storage.
5. Incorporar notificaciones push.
6. Completar pagos, reputación y mensajería.
