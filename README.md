# T4KASH

T4KASH es una aplicacion movil orientada al networking universitario y a la gestion de oportunidades flexibles para estudiantes. Su objetivo es conectar estudiantes con personas, empresas o instituciones que necesitan apoyo en microtrabajos, tutorias, colaboraciones, proyectos, servicios, practicas o voluntariados.

La plataforma busca ofrecer un entorno mas organizado y confiable que canales informales como grupos de WhatsApp o publicaciones sueltas en redes sociales. Para lograrlo, el sistema se apoya en perfiles profesionales, habilidades, postulaciones, entregas, reputacion, reportes y seguimiento de pagos.

## Problema

Muchos estudiantes universitarios necesitan generar ingresos mientras estudian, pero sus horarios no siempre les permiten aceptar empleos tradicionales. Al mismo tiempo, muchas personas necesitan ayuda puntual y no siempre encuentran estudiantes confiables o con habilidades verificables.

Los canales informales actuales presentan problemas como:

- Perfiles falsos o dificiles de verificar.
- Falta de reputacion entre usuarios.
- Poco seguimiento de tareas, entregas y pagos.
- Riesgo de estafas o incumplimientos.
- Dificultad para organizar postulaciones.
- Ausencia de historial profesional del estudiante.

## Propuesta

T4KASH centraliza el proceso completo de una oportunidad universitaria:

1. Un usuario publica una tarea u oportunidad.
2. Los estudiantes revisan oportunidades disponibles.
3. Un estudiante envia una postulacion.
4. El publicador acepta o rechaza postulaciones.
5. Una postulacion aceptada se convierte en trabajo asignado.
6. El estudiante envia entregas.
7. El publicador revisa la entrega.
8. El sistema registra estados, pagos, reportes y reputacion.

## Alcance del MVP

Para el hackathon se prioriza un MVP funcional y defendible. El objetivo no es implementar todo el ecosistema desde el primer avance, sino demostrar el flujo principal con una arquitectura lista para crecer.

El MVP incluye:

- Registro e inicio de sesion.
- Perfil basico de usuario/estudiante.
- Consulta de oportunidades.
- Creacion de oportunidades.
- Postulaciones.
- Aceptacion o rechazo de postulaciones.
- Trabajos asignados.
- Entregas.
- Estado basico de pago.

Quedan como fases posteriores:

- Mensajeria completa.
- Verificacion institucional automatica.
- Notificaciones avanzadas.
- Sistema completo de reportes.
- Auditoria administrativa.
- Recomendaciones y calificaciones avanzadas.
- Panel web administrativo.

## Tecnologias

| Capa | Tecnologia |
|---|---|
| Aplicacion movil | Kotlin, Android Studio, Jetpack Compose |
| Backend | Java, Spring Boot, Spring Data JPA |
| API | REST, Swagger / OpenAPI |
| Base de datos | PostgreSQL |
| Entorno local | Docker Compose |
| Diseno | Figma |
| Control de versiones | GitHub, Conventional Commits |

## Arquitectura

```text
Aplicacion Android
        |
        | HTTPS / REST
        v
API Backend Spring Boot
        |
        | JDBC / JPA
        v
Base de datos PostgreSQL
```

La aplicacion movil consume una API REST desarrollada con Spring Boot. El backend contiene la logica de negocio y se conecta a PostgreSQL mediante Spring Data JPA.

Durante el desarrollo se usara Docker Compose para levantar la API y la base de datos en local. Despues de validar que el flujo funciona correctamente, el backend y la base de datos se prepararan para desplegarse en un hosting gratuito compatible con Java/Spring Boot y PostgreSQL.

## Entornos

| Entorno | Uso | Estado |
|---|---|---|
| Local | Desarrollo, pruebas con Docker Compose y Android Studio | Planificado |
| Testing/Demo | Validacion previa a entrega del hackathon | Pendiente |
| Produccion | Hosting gratuito con API publica y base de datos desplegada | Pendiente |

Las URLs locales solo se usan para desarrollo. En produccion, la aplicacion movil debe apuntar a la URL real del backend desplegado.

## Estructura del repositorio

```text
T4k4sh/
  backend/                 API REST con Spring Boot
  mobile/                  Aplicacion Android en Kotlin
  database/                Scripts SQL del proyecto
    schema-postgresql.sql  Esquema principal en PostgreSQL
    sqlserver-original.sql Script original usado como referencia
  docs/
    diagramas/             Diagramas de base de datos y UML
  README.md                Documentacion principal
```

## Base de datos

El modelo original fue creado en SQL Server para facilitar la diagramacion. Para el desarrollo real se migro a PostgreSQL.

Archivo principal:

```text
database/schema-postgresql.sql
```

El esquema PostgreSQL incluye:

- 34 tablas.
- Llaves primarias.
- Llaves foraneas.
- Restricciones unicas.
- Validaciones basicas.
- Datos iniciales para roles, universidad, carreras, categorias y habilidades.

Tablas principales del MVP:

- `usuarios`
- `roles`
- `usuarios_estudiantes`
- `categorias_tarea`
- `tareas`
- `postulaciones`
- `trabajos_asignados`
- `entregas`
- `pagos`

## Modulos y responsables

| Modulo | Tablas principales | Responsable |
|---|---|---|
| Identidad y Perfiles | usuarios, roles, sesiones_usuario, carreras, habilidades, verificaciones_usuario | Dev 1 |
| Marketplace Core | tareas, categorias_tarea, postulaciones, trabajos_asignados, entregas | Dev 2 |
| Social y Comunicacion | mensajes, conversaciones, notificaciones, calificaciones, recomendaciones | Dev 1 |
| Finanzas y Sistema | pagos, transacciones_pago, reportes, auditoria_sistema, archivos_adjuntos | Dev 2 |

## Diseno de interfaz

Prototipo de referencia:

[Figma - T4KASH](https://www.figma.com/design/k5PeSZUFQJgIja3Mw2BpAw/Sin-t%C3%ADtulo?node-id=0-1&t=mX1bigfYctB3XCdZ-0)

Las pantallas nuevas deben respetar el estilo definido en Figma: colores, jerarquia visual, componentes, espaciado y comportamiento general.

## Diagramas

Los diagramas del proyecto se encuentran en:

```text
docs/diagramas/
```

Incluyen:

- Diagrama entidad-relacion.
- Diagrama de clases.
- Diagrama de actividades.
- Diagrama de casos de uso.

Estos diagramas documentan el alcance completo del sistema, aunque el MVP se enfoque en el flujo principal de oportunidades, postulaciones y entregas.

## Variables de entorno previstas

Estas variables se usaran cuando se cree el backend:

| Variable | Descripcion | Ejemplo local |
|---|---|---|
| `SERVER_PORT` | Puerto donde corre la API | `8080` |
| `SPRING_DATASOURCE_URL` | Conexion JDBC a PostgreSQL | `jdbc:postgresql://db:5432/t4kash` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos | `t4kash` |
| `SPRING_DATASOURCE_PASSWORD` | Password de la base de datos | `t4kash` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Estrategia de JPA | `validate` |
| `APP_CORS_ALLOWED_ORIGINS` | Origenes permitidos para consumir la API | URL del frontend/app |

En produccion estas variables no deben guardarse dentro del repositorio. Deben configurarse desde el panel del hosting.

## Ejecucion local prevista

El entorno local del backend se levanta con Docker Compose:

```powershell
cd backend
docker compose up -d --build
```

Docker Compose inicia PostgreSQL, carga el esquema ubicado en `database/schema-postgresql.sql` y luego inicia la API Spring Boot.

La documentacion local de la API queda disponible mediante Swagger/OpenAPI cuando el backend esta corriendo. La URL final de produccion dependera del hosting elegido.

Para Android Emulator, la app no usa `localhost` directamente. En desarrollo se utiliza la direccion especial del emulador para comunicarse con la maquina anfitriona.

## Despliegue previsto

El flujo recomendado sera:

1. Crear backend Spring Boot.
2. Conectar backend con PostgreSQL local usando Docker Compose.
3. Probar endpoints con Swagger o Postman.
4. Conectar la app Android al backend local.
5. Validar el flujo completo del MVP.
6. Preparar variables de entorno para hosting.
7. Desplegar backend en un hosting gratuito.
8. Desplegar o conectar base de datos PostgreSQL en un servicio compatible.
9. Cambiar la URL base de la app Android hacia la API desplegada.

## Contrato inicial de endpoints

Estos endpoints representan el contrato inicial del MVP. La primera version del backend implementa el flujo Marketplace Core sin autenticacion real; por ahora los requests usan IDs de usuarios demo para poder validar el flujo completo.

| Metodo | Ruta | Uso |
|---|---|---|
| `POST` | `/api/auth/register` | Registrar usuario |
| `POST` | `/api/auth/login` | Iniciar sesion |
| `GET` | `/api/tasks` | Listar oportunidades |
| `POST` | `/api/tasks` | Crear oportunidad |
| `GET` | `/api/tasks/{id}` | Ver detalle de oportunidad |
| `POST` | `/api/tasks/{id}/applications` | Postularse a una oportunidad |
| `GET` | `/api/tasks/{id}/applications` | Ver postulaciones de una oportunidad |
| `POST` | `/api/applications/{id}/accept` | Aceptar postulacion |
| `POST` | `/api/applications/{id}/reject` | Rechazar postulacion |
| `GET` | `/api/jobs` | Listar trabajos asignados |
| `POST` | `/api/jobs/{id}/deliveries` | Enviar entrega |
| `POST` | `/api/deliveries/{id}/approve` | Aprobar entrega |

Ejemplo de creacion de oportunidad:

```json
{
  "titulo": "Tutoria de programacion",
  "descripcion": "Necesito apoyo con ejercicios de Java y bases de datos.",
  "presupuesto": 25.00,
  "idCategoria": 1,
  "tipoOportunidad": "TUTORIA",
  "modalidad": "REMOTA"
}
```

Ejemplo de postulacion:

```json
{
  "mensaje": "Tengo experiencia explicando Java y puedo ayudarte por la tarde.",
  "precioPropuesto": 20.00
}
```

## Scripts previstos

Comandos principales del backend:

```powershell
cd backend
docker compose up -d --build
docker compose logs -f api
docker compose down
```

Para Android:

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

## Control de versiones

El proyecto usara ramas y Conventional Commits.

Ejemplos:

```text
docs: actualizar readme principal
feat: agregar esquema postgresql
feat: crear proyecto android inicial
feat: agregar endpoints de tareas
fix: corregir estado de postulaciones
chore: configurar docker compose
```

## Estado actual

- Repositorio organizado por carpetas.
- Proyecto Android inicial creado en `mobile/`.
- Backend Spring Boot inicial creado en `backend/`.
- Docker Compose configurado para API y PostgreSQL.
- Esquema PostgreSQL migrado en `database/schema-postgresql.sql`.
- Datos demo agregados para probar el flujo Marketplace Core.
- Diagramas colocados en `docs/diagramas/`.

## Proximos pasos

1. Probar el backend con Docker Compose.
2. Validar endpoints con Swagger/Postman.
3. Conectar la app Android al backend local.
4. Implementar autenticacion real.
5. Preparar despliegue en hosting gratuito.
