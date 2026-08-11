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
7. Cliente y estudiante coordinan el trabajo mediante la conversación interna.
8. El estudiante registra una entrega.
9. El cliente elige pago protegido; solo las tareas presenciales permiten efectivo.
10. Pagadito confirma el pago y habilita el inicio del trabajo.
11. El estudiante registra una entrega.
12. El cliente aprueba la entrega y el monto pasa al balance disponible.

## Estado del MVP

| Componente | Estado |
|---|---|
| API Spring Boot desplegada en Render | Implementado |
| PostgreSQL administrado en Supabase | Implementado |
| Archivos privados en Supabase Storage | Implementado |
| Documentación Swagger/OpenAPI | Implementado |
| Marketplace y detalle de oportunidades en Android | Implementado |
| Publicación de tareas desde Android | Implementado |
| Veinticuatro categorías de oportunidades | Implementado |
| Ubicación para tareas presenciales e híbridas | Implementado |
| Mapa con radio de búsqueda y marcadores interactivos | Implementado |
| Tareas rápidas con radar, vigencia automática de 24 horas, asignación inmediata y efectivo confirmado por ambas partes | Implementado |
| Postulación desde Android | Implementado |
| Postulaciones, asignaciones y entregas en la API | Implementado |
| Navegación, carga y manejo visual de errores | Implementado |
| Registro institucional, verificación por correo y sesiones persistentes | Implementado |
| Autenticación en dos pasos (segundo código al iniciar sesión) | Implementado |
| Recuperación de contraseña por código | Implementado |
| Verificación de perfil estudiantil con adjunto y revisión administrativa | Implementado |
| Identidad pública por arroba y cambio de nombre de usuario | Implementado |
| Conversaciones, mensajes y notificaciones internas | Implementado |
| Configuración local con cuatro temas de fondo para el chat | Implementado |
| Formularios adaptados al teclado y manejo visual de errores | Implementado |
| Modelo financiero para wallet, Pagadito y efectivo | Implementado |
| Integración con Pagadito Sandbox y wallet transaccional | Implementado; requiere credenciales Sandbox |
| Puntos T4KASH y catálogo de beneficios | Diseñado en PostgreSQL |
| Calificaciones y reputación | Pendiente |
| Notificaciones push con Firebase Cloud Messaging | Pendiente |

El registro detecta automáticamente la universidad a partir del dominio del correo,
muestra carreras únicamente cuando existe una coincidencia institucional activa y
activa la cuenta después de confirmar un código enviado por correo.
Cada cuenta recibe un nombre de usuario público único generado desde el nombre y apellido.
El usuario puede cambiarlo desde su perfil y debe esperar 30 días antes de escoger otro.
Las oportunidades, postulaciones, trabajos y conversaciones muestran el nombre y la
arroba pública; el perfil público no expone correo, carnet, estado interno ni otros datos
privados de la cuenta.
Android conserva la sesión iniciada y utiliza internamente el ID autenticado para
publicaciones, postulaciones, trabajos y archivos. Las contraseñas se almacenan con BCrypt
y la base conserva únicamente el hash de cada token de sesión.

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
| Pagos de prueba | Pagadito Sandbox, WSPG y webhooks firmados |
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
   |\
   | +-- JDBC / JPA + SSL --> PostgreSQL en Supabase
   | +-- HTTPS -------------> Supabase Storage
   | +-- HTTPS -------------> Brevo Email API
   | +-- SOAP / HTTPS -------> Pagadito Sandbox
   | <-- HTTPS firmado ------- Webhooks de Pagadito
```

- Android nunca se conecta directamente a PostgreSQL.
- Las credenciales de Supabase solo se configuran en Render.
- La aplicación consulta y publica tareas mediante la API.
- OpenFreeMap proporciona el estilo y las teselas del mapa sin requerir una clave privada.
- Supabase Storage conserva los archivos privados y solo el backend utiliza su clave secreta.
- Brevo envía por HTTPS los códigos de activación, segundo paso y recuperación.
- Pagadito Sandbox procesa pagos ficticios y notifica cambios de estado al backend.
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

El esquema contiene 47 tablas e incluye:

- Llaves primarias y foráneas.
- Restricciones únicas y validaciones.
- Índices para búsquedas frecuentes.
- Usuarios, roles y perfiles universitarios.
- Nombres de usuario públicos y múltiples dominios por universidad.
- Tareas, habilidades, postulaciones y trabajos.
- Entregas, pagos, conversaciones y reportes.
- Sesiones, verificaciones y auditoría.
- Catalogos iniciales con roles, universidades de Managua y Leon, carreras, categorias de oportunidades y habilidades.

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
| `POST` | `/api/auth/register` | Crear una cuenta pendiente y enviar el código |
| `POST` | `/api/auth/verify-email` | Verificar el código y activar la cuenta |
| `POST` | `/api/auth/resend-verification` | Enviar un código nuevo de activación |
| `POST` | `/api/auth/login` | Validar correo y contraseña, y enviar el código del segundo paso |
| `POST` | `/api/auth/login/verify` | Confirmar el código del segundo paso y crear la sesión |
| `POST` | `/api/auth/login/resend` | Reenviar el código del segundo paso |
| `POST` | `/api/auth/password/forgot` | Solicitar código de recuperación de contraseña |
| `POST` | `/api/auth/password/reset` | Confirmar código y establecer una contraseña nueva |
| `GET` | `/api/auth/me` | Consultar el usuario autenticado |
| `POST` | `/api/auth/logout` | Cerrar la sesión actual |
| `GET` | `/api/profiles/{username}` | Consultar un perfil público por arroba |
| `PUT` | `/api/profiles/me/username` | Cambiar la arroba del usuario autenticado |
| `GET` | `/api/identity/universities` | Listar universidades activas |
| `GET` | `/api/identity/universities/{id}/careers` | Listar carreras de una universidad |
| `GET` | `/api/student-verifications/me` | Consultar mi validación estudiantil |
| `POST` | `/api/student-verifications/me/attachments` | Enviar carnet o constancia universitaria |
| `GET` | `/api/student-verifications/pending` | Listar validaciones pendientes (admin) |
| `POST` | `/api/student-verifications/{userId}/approve` | Aprobar perfil estudiantil (admin) |
| `POST` | `/api/student-verifications/{userId}/reject` | Rechazar perfil estudiantil (admin) |
| `GET` | `/api/categories` | Listar categorías activas |
| `GET` | `/api/tasks` | Listar oportunidades |
| `POST` | `/api/tasks` | Crear una oportunidad |
| `PUT` | `/api/tasks/{idTarea}` | Editar una oportunidad activa |
| `DELETE` | `/api/tasks/{idTarea}` | Cancelar una oportunidad activa (propietario) |
| `GET` | `/api/tasks/{idTarea}` | Obtener detalle |
| `GET` | `/api/quick-tasks/nearby` | Buscar tareas rápidas cercanas por ubicación y radio |
| `POST` | `/api/quick-tasks/{idTarea}/claim` | Tomar una tarea rápida disponible |
| `GET` | `/api/tasks/{idTarea}/applications` | Listar postulaciones |
| `POST` | `/api/tasks/{idTarea}/applications` | Crear postulación |
| `GET` | `/api/applications/me` | Listar mis postulaciones |
| `POST` | `/api/tasks/{idTarea}/reports` | Reportar una publicación |
| `GET` | `/api/reports/me` | Consultar reportes enviados |
| `POST` | `/api/applications/{idPostulacion}/accept` | Aceptar postulación |
| `POST` | `/api/applications/{idPostulacion}/reject` | Rechazar postulación |
| `GET` | `/api/jobs` | Listar trabajos asignados |
| `GET` | `/api/jobs/{idTrabajo}/deliveries` | Listar entregas |
| `POST` | `/api/jobs/{idTrabajo}/deliveries` | Registrar entrega |
| `POST` | `/api/deliveries/{idEntrega}/approve` | Aprobar entrega |
| `GET` | `/api/wallet` | Consultar balance, pagos y movimientos |
| `GET` | `/api/jobs/{idTrabajo}/payment` | Consultar el pago de un trabajo |
| `POST` | `/api/jobs/{idTrabajo}/payment/checkout` | Crear checkout de Pagadito Sandbox |
| `POST` | `/api/jobs/{idTrabajo}/payment/cash/confirm-receipt` | Confirmar que el estudiante recibió el efectivo |
| `POST` | `/api/payments/{idPago}/refresh` | Consultar nuevamente el estado en Pagadito |
| `POST` | `/api/payments/pagadito/webhook` | Recibir eventos firmados de Pagadito |
| `GET` | `/api/payments/pagadito/return` | Verificar el retorno del checkout |
| `GET` | `/api/tasks/{taskId}/attachments` | Listar archivos adjuntos de una tarea |
| `POST` | `/api/tasks/{taskId}/attachments` | Adjuntar archivo a una tarea |
| `GET` | `/api/deliveries/{deliveryId}/attachments` | Listar archivos adjuntos de una entrega |
| `POST` | `/api/deliveries/{deliveryId}/attachments` | Adjuntar archivo a una entrega |
| `GET` | `/api/jobs/{jobId}/attachments` | Listar archivos de las entregas de un trabajo |
| `GET` | `/api/attachments/{attachmentId}/download` | Descargar un archivo adjunto privado |
| `GET` | `/api/conversations` | Listar conversaciones del usuario |
| `GET` | `/api/conversations/{id}/messages` | Consultar mensajes |
| `POST` | `/api/conversations/{id}/messages` | Enviar un mensaje |
| `POST` | `/api/conversations/{id}/read` | Marcar mensajes como leídos |
| `GET` | `/api/notifications` | Listar notificaciones |
| `POST` | `/api/notifications/{id}/read` | Marcar una notificación como leída |
| `POST` | `/api/notifications/read-all` | Marcar todas como leídas |
| `GET` | `/api/admin/summary` | Consultar resumen administrativo |
| `GET` | `/api/admin/tasks` | Listar publicaciones para moderación |
| `DELETE` | `/api/admin/tasks/{idTarea}` | Retirar una publicación (admin) |
| `GET` | `/api/admin/reports` | Listar reportes de moderación |
| `POST` | `/api/admin/reports/{idReporte}/review` | Resolver o descartar un reporte |

Los endpoints privados de identidad, marketplace y archivos requieren el encabezado
`Authorization: Bearer <token>`. El token completo se entrega únicamente al cliente;
PostgreSQL almacena su hash SHA-256. El backend obtiene el usuario desde esta sesión y
no acepta IDs de cliente, estudiante o propietario enviados por Android.

En Swagger, el token se configura desde **Authorize**. Las operaciones protegidas
muestran un candado y responden `401` cuando la sesión no es válida o `403` cuando
el usuario no tiene el rol, la propiedad o la participación necesaria.

### Inicio de Sesión en Dos Pasos

`POST /api/auth/login` valida correo y contraseña, y envía un código temporal por
correo mediante Brevo; no crea la sesión todavía. La sesión y el token Bearer se
generan al confirmar ese código con `POST /api/auth/login/verify`.

`POST /api/auth/login`:

```json
{
  "correo": "estudiante@uamv.edu.ni",
  "password": "••••••••"
}
```

`POST /api/auth/login/verify`:

```json
{
  "correo": "estudiante@uamv.edu.ni",
  "codigo": "123456"
}
```

El código vence a los 10 minutos y `POST /api/auth/login/resend` permite pedir uno
nuevo respetando un mínimo de 60 segundos entre reenvíos. Después de 5 intentos
fallidos en una ventana de 15 minutos, la cuenta queda bloqueada temporalmente.

### Recuperar Contraseña

`POST /api/auth/password/forgot` envía un código de recuperación al correo
registrado. `POST /api/auth/password/reset` confirma ese código junto con la nueva
contraseña:

```json
{
  "correo": "estudiante@uamv.edu.ni",
  "codigo": "123456",
  "nuevaPassword": "unaContrasenaNueva123"
}
```

### Verificación de Perfil Estudiantil

Un estudiante consulta el estado de su validación con `GET /api/student-verifications/me`
y envía su carnet o constancia con `POST /api/student-verifications/me/attachments`
(`multipart/form-data`, campo `file`). Un administrador revisa las solicitudes pendientes
desde `GET /api/student-verifications/pending` y las aprueba o rechaza con
`POST /api/student-verifications/{userId}/approve` o `.../reject`, indicando una
observación opcional.

### Crear una Tarea Presencial

```json
{
  "titulo": "Apoyo durante un evento universitario",
  "descripcion": "Necesito apoyo con el registro de asistentes y organización del salón.",
  "presupuesto": 25.00,
  "fechaLimitePostulacion": null,
  "fechaLimite": null,
  "idCategoria": 4,
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
  "mensaje": "Tengo experiencia en este tipo de trabajo y disponibilidad esta semana.",
  "precioPropuesto": 25.00
}
```

### Aceptar y Pagar un Trabajo

Al aceptar una postulación, el cliente envía el método acordado:

```json
{
  "metodoPago": "PAGADITO"
}
```

`EFECTIVO` solo se acepta cuando la tarea es presencial. Para Pagadito, el trabajo queda
en `PENDIENTE_PAGO`; el cliente abre el checkout desde Wallet y el backend cambia el
trabajo a `EN_PROCESO` únicamente después de validar la confirmación del proveedor.

En el comercio Pagadito Sandbox deben configurarse estas direcciones públicas:

```text
Retorno: https://t4k4sh.onrender.com/api/payments/pagadito/return
Webhook: https://t4k4sh.onrender.com/api/payments/pagadito/webhook
Evento:  TRANSACTION.STATUS.CHANGE
```

La solicitud se envía mediante `POST /api/tasks/{idTarea}/applications`. Un estudiante
puede volver a postularse después de un rechazo hasta completar tres intentos, pero no
puede mantener dos postulaciones pendientes sobre la misma tarea.

### Reportes y Moderación

Desde el detalle de una oportunidad, un usuario puede seleccionar un motivo y enviar un
reporte. La API impide reportar publicaciones propias y duplicar un reporte pendiente.

El administrador revisa los reportes desde su panel y puede:

- Marcar el reporte como `RESUELTO`.
- Marcarlo como `DESCARTADO`.
- Retirar la publicación cuando exista una infracción.

Las revisiones y los retiros administrativos se registran en `auditoria_sistema` con el
administrador responsable, la dirección IP, el dispositivo y los estados antes y después
de la operación.

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
2. Los filtros permiten explorar las veinticuatro categorías disponibles.
3. El mapa solicita la ubicación del dispositivo y descarta coordenadas inválidas como `(0, 0)`.
4. El usuario ajusta un radio de búsqueda entre `5 km` y `50 km`.
5. Al tocar un marcador se abre el detalle de la oportunidad.
6. Desde el detalle también se puede abrir el mapa centrado en la tarea.
7. El estudiante completa un mensaje y un precio propuesto.
8. Android envía la postulación al backend y muestra el resultado real.

Flujo actual de comunicación:

1. La API crea una conversación cuando una postulación es aceptada.
2. El cliente y el estudiante asignado consultan sus conversaciones y mensajes.
3. El chat abierto se actualiza periódicamente sin bloquear la interfaz.
4. Los mensajes se separan por fecha y conservan el estado de lectura.
5. Las postulaciones, asignaciones, entregas y mensajes generan notificaciones internas.

La mensajería pertenece al trabajo asignado: otros usuarios no pueden consultar ni
enviar mensajes dentro de esa conversación. El MVP utiliza actualización periódica;
Firebase Cloud Messaging queda reservado para notificaciones push posteriores.

## Uso del MVP

1. Abrir la aplicación, registrar una cuenta o iniciar sesión con una cuenta verificada.
2. Explorar oportunidades desde Inicio o aplicar filtros por categoría.
3. Abrir una tarea para consultar presupuesto, modalidad, fechas y ubicación.
4. En tareas presenciales o híbridas, utilizar el mapa para revisar la ubicación.
5. Pulsar **Postularse**, completar la propuesta y enviarla.
6. Utilizar la sección **Publicar** para crear una nueva oportunidad.
7. Consultar postulaciones, publicaciones y trabajos desde **Perfil**.
8. Abrir el chat de un trabajo asignado y enviar mensajes al otro participante.
9. Revisar las notificaciones internas y marcar elementos como leídos.
10. Abrir **Tareas rápidas** desde Inicio, ajustar un radio de 250 metros a 5 kilómetros
    y tomar una oportunidad urgente disponible. La asignación es inmediata, el monto no
    puede superar C$1,000 y el pago se realiza en efectivo sin comisión. La publicación
    vence automáticamente en 24 horas y, al tomarla, comienza un plazo de 3 horas.
11. Después de la entrega, el cliente declara el pago y el estudiante confirma que recibió
    el efectivo. Solo entonces el trabajo queda finalizado.

Las operaciones privadas utilizan el usuario autenticado de la sesión. Android no decide
el propietario de una tarea, postulación, entrega o archivo.

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
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

| Capa | Cantidad actual | Cobertura principal |
|---|---:|---|
| Backend | 52 pruebas | Identidad y perfiles públicos, catálogos institucionales, nombres de usuario, sesiones, intentos de acceso, correo, marketplace, adjuntos, reportes, conversaciones y arranque de Spring Boot |
| Android | 20 pruebas unitarias | Dominios de correo, formatos, fechas, moneda, distancias, ubicación y políticas de carga/actualización |

Además de las pruebas unitarias, `lintDebug` revisa problemas estáticos y
`assembleDebug` confirma que el APK puede generarse. Antes de una entrega se deben
ejecutar los cuatro comandos sobre una copia limpia del repositorio.

## Problemas Comunes

| Situación | Causa habitual | Solución |
|---|---|---|
| `mvnw.cmd` o `gradlew.bat` fallan con `JAVA_HOME is not set` | No hay un JDK configurado en el `PATH` del entorno | Instalar JDK 21 o usar el JBR incluido con Android Studio y exportar `JAVA_HOME` antes de ejecutar el comando |
| `docker compose up -d --build` no responde o falla al iniciar | Docker Desktop no está abierto o el puerto 5432/8080 ya está en uso | Iniciar Docker Desktop y liberar el puerto, o cambiar `SERVER_PORT` |
| Android no conecta con el backend local desde el emulador | Se usó `localhost` en vez de la dirección del host del emulador | Compilar con `-PT4KASH_API_BASE_URL=http://10.0.2.2:8080/api/` |
| La primera solicitud a la API en Render tarda o falla | El plan gratuito suspende el servicio por inactividad | Reintentar después de 30-90 segundos mientras el contenedor reinicia |
| El código de verificación o de dos pasos no llega | El correo cae en Spam/Promociones, o se reenvía antes de la espera mínima | Revisar esas carpetas y esperar 60 segundos entre reenvíos |
| `401` al llamar un endpoint privado desde Swagger | El token no se configuró desde **Authorize** o ya expiró | Repetir el login completo (dos pasos) y volver a autorizar el token |

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
| `APP_AUTH_EVALUATOR_EMAILS` | Correos no institucionales autorizados para evaluación | `evaluador@gmail.com` |
| `APP_AUTH_ADMIN_EMAILS` | Correos que reciben el rol de administrador al iniciar sesión | `admin@ejemplo.com` |
| `APP_MAIL_ENABLED` | Activa el envío de códigos | `true` |
| `APP_MAIL_PROVIDER` | Transporte de correo (`brevo` en Render Free, `smtp` en local) | `brevo` |
| `APP_MAIL_FROM` | Remitente visible de verificación | Remitente verificado en Brevo |
| `APP_MAIL_FROM_NAME` | Nombre visible del remitente | `T4KASH` |
| `BREVO_API_KEY` | Clave privada para enviar mediante HTTPS | Configurada en Render |
| `APP_PUBLIC_BASE_URL` | URL pública del backend para retornos | `https://t4k4sh.onrender.com` |
| `APP_PAYMENTS_PLATFORM_FEE_PERCENT` | Tarifa transparente de T4KASH | `1.00` |
| `APP_PAGADITO_ENABLED` | Activa la comunicación con Pagadito | `true` en la demo configurada |
| `APP_PAGADITO_ENVIRONMENT` | Entorno financiero | `SANDBOX` |
| `APP_PAGADITO_ENDPOINT` | Endpoint WSPG | URL oficial de Sandbox |
| `PAGADITO_UID` | Identificador privado del comercio Sandbox | Configurado en Render |
| `PAGADITO_WSK` | Clave privada WSPG y de firma | Configurada en Render |
| `PAGADITO_PROCESSOR_FEE_PERCENT` | Porcentaje informado por Pagadito | `0` mientras Sandbox no cobre |
| `PAGADITO_PROCESSOR_FIXED_FEE` | Cargo fijo informado por Pagadito | `0` |
| `PAGADITO_PROCESSOR_TAX_PERCENT` | Impuesto aplicable al cargo del procesador | `0` |
| `SMTP_HOST` | Servidor de correo | Servidor del proveedor |
| `SMTP_PORT` | Puerto SMTP | `587` |
| `SMTP_USERNAME` | Usuario SMTP | Configurado en Render |
| `SMTP_PASSWORD` | Contraseña o clave SMTP | Configurada en Render |
| `T4KASH_API_BASE_URL` | URL consumida por Android | URL de Render |

Las contraseñas, cadenas de conexión y claves privadas no deben guardarse en Git. Render administra las variables del backend y Android solo recibe la URL pública de la API.
Los servicios gratuitos de Render bloquean los puertos SMTP, por lo que el despliegue usa la API HTTPS de Brevo. La configuración SMTP se conserva para desarrollo local o proveedores que permitan esos puertos.

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

- Tamaño máximo actual desde la aplicación: 10 MB por archivo.
- El esquema admite hasta 100 MB para la futura integración con Cloudflare R2.
- Máximo desde Android: 3 archivos por publicación o entrega.
- Tipos aceptados: PDF, PNG, JPG, WebP, TXT, DOC, DOCX y ZIP.
- La clave secreta de Supabase se usa únicamente en el backend.
- Las descargas pasan por la API; Android no recibe acceso directo al bucket.

### Modelo Financiero

El MVP integra el ciclo financiero con Pagadito Sandbox. Los mensajes entre servicios,
el checkout, la consulta de estado y los webhooks son reales, mientras que las operaciones
y los fondos del entorno Sandbox son ficticios.

Reglas acordadas:

- Las tareas remotas e híbridas requieren pago protegido con Pagadito.
- Las tareas presenciales permiten elegir Pagadito o efectivo.
- Las tareas rápidas son presenciales, aceptan hasta C$1,000 y usan únicamente efectivo.
- El monto acordado indica la ganancia exacta del estudiante.
- El cliente ve por separado el monto del trabajo, la tarifa de T4KASH del 1 %, el costo
  del procesador y el total antes de confirmar.
- Un pago en efectivo no genera comisión y queda registrado como operación externa sin
  protección financiera ni reembolso desde T4KASH.
- Aceptar una postulación reserva el trabajo; el estudiante no debe comenzar hasta que
  el pago protegido haya sido confirmado.

Las tablas financieras separan la orden de pago, sus movimientos, los eventos webhook,
los desembolsos, los reembolsos y las disputas. Las claves de idempotencia impiden
registrar dos veces una notificación o solicitud repetida. La billetera se calcula a
partir de estos movimientos auditables y no almacena un saldo aislado susceptible de
desincronizarse.

Estados principales implementados:

```text
PENDIENTE_PAGO -> PAGO_REGISTRADO o PAGO_EN_VERIFICACION
PENDIENTE_PAGO -> FONDOS_RETENIDOS
FONDOS_RETENIDOS -> PAGO_LIBERADO
FONDOS_RETENIDOS -> EN_DISPUTA -> REEMBOLSADO o PAGO_LIBERADO
PAGO_EXTERNO_PENDIENTE -> PAGO_EXTERNO_CONFIRMADO
```

### Puntos T4KASH

Los puntos son recompensas internas y no representan dinero electrónico. No pueden
comprarse, retirarse, convertirse a córdobas ni transferirse entre usuarios. Se obtienen
por acciones verificadas, como completar un trabajo o mantener una buena participación,
y se utilizan exclusivamente para beneficios dentro de T4KASH.

El esquema separa:

- `catalogo_beneficios_puntos`: define los beneficios disponibles y su costo.
- `movimientos_puntos`: registra entradas, salidas, ajustes y expiraciones sin guardar un
  saldo aislado.
- `canjes_puntos`: conserva el beneficio solicitado, su costo histórico y su vigencia.

El catálogo inicial permite destacar una tarea, una publicación o un perfil. Cada evento
usa una clave de idempotencia para impedir que una recompensa o un canje se aplique dos
veces. El saldo visible se obtiene sumando movimientos aplicados de entrada y restando
los de salida.

El esquema PostgreSQL completo contiene 47 tablas organizadas en identidad, marketplace,
finanzas, puntos, comunicación, moderación y auditoría.

Las 47 tablas tienen Row Level Security habilitado sin políticas para las claves públicas
de Supabase. Android accede exclusivamente mediante la API Spring Boot; el backend usa su
conexión PostgreSQL de servidor y las claves privadas nunca se incluyen en la aplicación.

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

## Próximas Etapas

El flujo principal del MVP ya está implementado. El trabajo restante se separa entre el
cierre obligatorio del hackathon y mejoras que pueden desarrollarse después.

### Cierre del Hackathon

1. **Pruebas integrales**
   - Probar registro, segundo paso, recuperación y cierre de sesión desde un teléfono real.
   - Ejecutar el ciclo completo con dos cuentas: publicar, postular, aceptar, conversar,
     entregar y aprobar.
   - Probar verificación estudiantil, reportes y moderación con una cuenta administradora.
   - Ejecutar las 55 pruebas del backend, las pruebas de Android, `lintDebug` y `assembleDebug`.
2. **Integración final**
   - Resolver diferencias entre ramas y completar los Pull Requests pendientes.
   - Integrar la versión validada en `main` y comprobar el despliegue automático de Render.
   - Generar y conservar el APK final utilizado durante la demostración.
3. **Documentación y evidencias**
   - Actualizar los diagramas para reflejar identidad, moderación y comunicación.
   - Revisar README, guía del evaluador y documentación de despliegue.
   - Sustituir prototipos antiguos por capturas actuales de T4KASH.
   - Preparar el video explicativo, accesos del evaluador y enlaces finales del tablero.

### Mejoras Posteriores al MVP

1. **Finanzas y reputación**
   - Completar reembolsos, desembolsos y resolución de disputas sobre el flujo implementado.
   - Validar con Pagadito el modelo de marketplace antes de utilizar producción.
   - Implementar las reglas de obtención y canje de puntos T4KASH.
   - Agregar calificaciones y recomendaciones al finalizar trabajos.
2. **Perfil profesional y networking**
   - Completar habilidades, portafolio y conexiones entre usuarios.
   - Mostrar experiencia y reputación verificable en los perfiles.
3. **Notificaciones push**
   - Integrar Firebase Cloud Messaging para avisos fuera de la aplicación.
   - Mantener las notificaciones internas como historial persistente.
4. **Endurecimiento para producción**
   - Restringir CORS, rotar secretos y revisar dependencias vulnerables.
   - Incorporar monitoreo, copias de seguridad y pruebas end-to-end automatizadas.
   - Evaluar servicios con disponibilidad garantizada para sustituir los planes gratuitos.
