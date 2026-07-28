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
| Archivos privados en Supabase Storage | Implementado |
| Documentación Swagger/OpenAPI | Implementado |
| Marketplace y detalle de oportunidades en Android | Implementado |
| Publicación de tareas desde Android | Implementado |
| Doce categorías de oportunidades | Implementado |
| Ubicación para tareas presenciales e híbridas | Implementado |
| Mapa con radio de búsqueda y marcadores interactivos | Implementado |
| Postulación desde Android | Implementado |
| Postulaciones, asignaciones y entregas en la API | Implementado |
| Navegación, carga y manejo visual de errores | Implementado |
| Autenticación y sesiones reales | Pendiente |
| Mensajería, pagos y notificaciones push | Pendiente |

La versión actual usa usuarios demo para validar el flujo. La publicación desde Android utiliza temporalmente `idCliente = 1` y la postulación utiliza `idEstudiante = 1` hasta integrar autenticación real.

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

## Requisitos Técnicos

Para ejecutar el proyecto se necesita:

- Git para descargar y versionar el repositorio.
- Android Studio con el SDK de Android configurado.
- JDK 21 para compilar el backend.
- Docker Desktop con Docker Compose para el entorno local.
- Conexión a Internet para consumir Render, Supabase y OpenFreeMap.
- Un emulador Android o un teléfono con depuración USB habilitada.

No se necesita instalar PostgreSQL directamente cuando se utiliza Docker Compose. Para probar solamente la aplicación móvil contra la API publicada tampoco es necesario iniciar el backend local.

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
- Datos demo para roles, universidad, carreras y doce categorías de oportunidades.

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

El esquema completo contiene instrucciones `DROP TABLE` para recrear un entorno desde cero. No debe ejecutarse nuevamente sobre la base remota con información importante. Los cambios en Supabase deben aplicarse de forma controlada desde SQL Editor y reflejarse después en este archivo.

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

### Enviar una Postulación

```json
{
  "idEstudiante": 1,
  "mensaje": "Tengo experiencia en este tipo de trabajo y disponibilidad esta semana.",
  "precioPropuesto": 25.00
}
```

La solicitud se envía mediante `POST /api/tasks/{idTarea}/applications`. La API rechaza una segunda postulación del mismo estudiante para la misma tarea y devuelve un mensaje que Android muestra en el formulario.

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

Flujo actual de exploración y postulación:

1. Android consulta las oportunidades y categorías publicadas.
2. Los filtros permiten explorar las doce categorías disponibles.
3. El mapa solicita la ubicación del dispositivo y descarta coordenadas inválidas como `(0, 0)`.
4. El usuario ajusta un radio de búsqueda entre `5 km` y `50 km`.
5. Al tocar un marcador se abre el detalle de la oportunidad.
6. Desde el detalle también se puede abrir el mapa centrado en la tarea.
7. El estudiante completa un mensaje y un precio propuesto.
8. Android envía la postulación al backend y muestra el resultado real.

## Uso del MVP

1. Abrir la aplicación y entrar con el acceso demo.
2. Explorar oportunidades desde Inicio o aplicar filtros por categoría.
3. Abrir una tarea para consultar presupuesto, modalidad, fechas y ubicación.
4. En tareas presenciales o híbridas, utilizar el mapa para revisar la ubicación.
5. Pulsar **Postularse**, completar la propuesta y enviarla.
6. Utilizar la sección **Publicar** para crear una nueva oportunidad.

Las funciones de identidad todavía utilizan IDs temporales. No deben interpretarse como autenticación o autorización definitiva.

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
.\gradlew.bat :app:testDebugUnitTest
```

Las pruebas del backend cubren la normalización de modalidades, la eliminación de coordenadas en tareas remotas y la obligación de ubicación para tareas presenciales. La compilación y las pruebas unitarias de Android se ejecutan antes de cerrar cada etapa funcional.

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
| `SUPABASE_URL` | URL del proyecto usada por Storage | `https://PROJECT_REF.supabase.co` |
| `SUPABASE_SECRET_KEY` | Clave secreta usada solo por el backend | Configurada en Render |
| `SUPABASE_STORAGE_BUCKET` | Bucket privado de adjuntos | `t4kash-attachments` |
| `T4KASH_API_BASE_URL` | URL consumida por Android | URL de Render |

Las contraseñas, cadenas de conexión y claves privadas no deben guardarse en Git. Render administra las variables del backend y Android solo recibe la URL pública de la API.

## Despliegue

Orden correcto para publicar cambios:

1. Verificar que el esquema actualizado ya esté aplicado en Supabase.
2. Subir el backend a GitHub.
3. Esperar que Render finalice el despliegue y muestre el servicio como `Live`.
4. Verificar `/api/health` y Swagger.
5. Compilar o ejecutar Android apuntando a Render.
6. Probar el flujo completo desde un dispositivo o emulador.

### Archivos Adjuntos

Los archivos de tareas y entregas se guardan en el bucket privado
`t4kash-attachments`. PostgreSQL conserva solamente el nombre, tipo, tamaño,
ruta y propietario del archivo.

- Tamaño máximo: 10 MB por archivo.
- Máximo desde Android: 3 archivos por publicación o entrega.
- Tipos aceptados: PDF, PNG, JPG, WebP, TXT, DOC, DOCX y ZIP.
- La clave secreta de Supabase se usa únicamente en el backend.
- Las descargas pasan por la API; Android no recibe acceso directo al bucket.

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

## Convenciones de Código

Para evitar mezclar estilos entre módulos, el proyecto seguirá estas reglas:

| Elemento | Convención | Ejemplo |
|---|---|---|
| Clases, interfaces y archivos de código | Inglés y `PascalCase` | `AttachmentService`, `JobDetailScreen` |
| Funciones, propiedades y variables internas | Inglés y `lowerCamelCase` | `loadAttachments`, `selectedFiles` |
| Constantes | Inglés y `UPPER_SNAKE_CASE` | `MAX_FILE_SIZE` |
| Paquetes y rutas técnicas | Inglés, minúsculas y nombres breves | `service`, `repository`, `attachments` |
| Tablas y columnas de PostgreSQL | Español y `snake_case` | `archivos_adjuntos`, `id_tarea` |
| Campos existentes de la API | Mantener el contrato actual | `idTarea`, `fechaLimite` |
| Textos visibles, documentación y comentarios | Español claro | `Cargando oportunidades...` |

Los comentarios deben explicar decisiones, límites o motivos que no sean evidentes en el
código. No deben repetir literalmente lo que hace una instrucción. Los nombres heredados
de la base de datos o de la API solo se cambiarán mediante una modificación coordinada
entre PostgreSQL, backend y Android.

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

## Etapas Pendientes

1. **Optimización y estandarización (en progreso)**
   - Ya se centralizaron los formatos de fechas, córdobas y tamaños de archivo.
   - El usuario demo se obtiene desde un único punto de la aplicación.
   - Mantener las convenciones de nombres en todo código interno nuevo.
   - Reducir recargas y solicitudes duplicadas al backend.
   - Dividir componentes o `ViewModel` que acumulen demasiadas responsabilidades.
   - Eliminar comentarios redundantes y mantener solo los que aporten contexto.
2. **Integración de identidad**
   - Conectar el módulo de autenticación y roles desarrollado por Dev 1.
   - Sustituir el usuario demo con ID `1` por el usuario de la sesión.
   - Aplicar permisos según el rol y la propiedad de cada recurso.
3. **Ubicación y mapa**
   - Permitir elegir o ajustar manualmente la ubicación de una tarea.
   - Completar el flujo de distancia, rango y navegación hacia la oportunidad.
4. **Comunicación**
   - Implementar conversaciones y mensajes.
   - Incorporar notificaciones push con Firebase Cloud Messaging.
5. **Finanzas y reputación**
   - Completar wallet, pagos y movimientos.
   - Agregar calificaciones y reputación al finalizar trabajos.
6. **Cierre técnico**
   - Ejecutar pruebas integrales de Android, backend y PostgreSQL.
   - Revisar validaciones, permisos, manejo de errores y estados de sesión.
   - Actualizar diagramas, documentación y guía de despliegue final.
