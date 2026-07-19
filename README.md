# T4KASH

T4KASH es una aplicacion movil para networking universitario y oportunidades de trabajo flexible. La plataforma conecta estudiantes con personas, empresas o instituciones que necesitan microtrabajos, tutorias, colaboraciones, proyectos, servicios, practicas o voluntariados.

La idea es ofrecer una alternativa mas organizada y segura que Facebook Marketplace o grupos de WhatsApp, usando perfiles profesionales, habilidades, postulaciones, entregas, reputacion y seguimiento de pagos.

## Problema

Muchos estudiantes necesitan generar ingresos mientras estudian, pero no siempre pueden aceptar trabajos de tiempo completo por sus horarios variables. Al mismo tiempo, muchas personas necesitan ayuda puntual y no encuentran estudiantes confiables.

Los canales informales actuales provocan problemas como:

- Perfiles falsos.
- Falta de reputacion.
- Poco seguimiento de trabajos.
- Riesgo de estafas.
- Dificultad para validar habilidades.
- Desorden en pagos, entregas y comunicacion.

## Objetivo

Crear una aplicacion movil que permita publicar oportunidades universitarias, postular estudiantes, asignar trabajos y dar seguimiento a entregas y pagos desde una API centralizada.

## Alcance MVP

Para el hackathon se prioriza el flujo principal:

1. Registro e inicio de sesion.
2. Perfil basico del estudiante.
3. Listado de oportunidades.
4. Creacion de oportunidades.
5. Postulacion a una oportunidad.
6. Aceptacion o rechazo de postulaciones.
7. Asignacion de trabajo.
8. Envio y revision de entregas.
9. Estado basico de pago.

Funciones como chat avanzado, reportes completos, verificacion institucional automatica, auditoria, recomendaciones y administracion avanzada quedan planteadas en el modelo de datos para siguientes iteraciones.

## Tecnologias

| Capa | Tecnologia |
|---|---|
| Aplicacion movil | Kotlin, Android Studio, Jetpack Compose |
| Backend | Java, Spring Boot, Spring Data JPA |
| API | REST, Swagger / OpenAPI |
| Base de datos | PostgreSQL |
| Pruebas locales | Docker Compose |
| Diseno | Figma |
| Versionamiento | GitHub, Conventional Commits |

## Arquitectura

```text
Aplicacion Android (Kotlin)
          |
          v
API REST (Spring Boot)
          |
          v
PostgreSQL
```

La aplicacion movil consumira la API REST. El backend centralizara la logica de negocio y se conectara a PostgreSQL. Para pruebas locales, Android Emulator debe consumir la API con:

```text
http://10.0.2.2:8080/api/
```

Desde navegador o Postman:

```text
http://localhost:8080/api/
```

## Diseno

Prototipo de referencia en Figma:

[Figma - T4KASH](https://www.figma.com/design/k5PeSZUFQJgIja3Mw2BpAw/Sin-t%C3%ADtulo?node-id=0-1&t=mX1bigfYctB3XCdZ-0)

Las pantallas nuevas deben mantener consistencia visual con el prototipo: colores, espaciado, componentes, jerarquia y estilo general.

## Base de datos

El modelo original fue disenado en SQL Server para facilitar la diagramacion. Para el desarrollo real se migro a PostgreSQL.

Archivo principal:

```text
schema-postgresql.sql
```

El archivo contiene:

- 34 tablas del modelo original.
- Llaves primarias.
- Relaciones por llaves foraneas.
- Restricciones unicas principales.
- Datos iniciales minimos para roles, universidad, carreras, categorias y habilidades.

## Modulos del sistema

| Modulo | Tablas principales | Responsable |
|---|---|---|
| Identidad y Perfiles | usuarios, roles, sesiones_usuario, carreras, habilidades, verificaciones_usuario | Dev 1 |
| Marketplace Core | tareas, categorias_tarea, postulaciones, trabajos_asignados, entregas | Dev 2 |
| Social y Comunicacion | mensajes, conversaciones, notificaciones, calificaciones, recomendaciones | Dev 1 |
| Finanzas y Sistema | pagos, transacciones_pago, reportes, auditoria_sistema, archivos_adjuntos | Dev 2 |

## Flujo principal

1. Un usuario se registra o inicia sesion.
2. El usuario solicitante publica una tarea u oportunidad.
3. El estudiante revisa oportunidades disponibles.
4. El estudiante envia una postulacion.
5. El solicitante acepta o rechaza postulaciones.
6. Al aceptar una postulacion se crea un trabajo asignado.
7. El estudiante envia una entrega.
8. El solicitante aprueba, rechaza o solicita correccion.
9. Se actualizan estados de trabajo, entrega y pago.

## Endpoints MVP esperados

| Metodo | Ruta | Uso |
|---|---|---|
| `POST` | `/api/auth/register` | Registrar usuario |
| `POST` | `/api/auth/login` | Iniciar sesion |
| `GET` | `/api/tasks` | Listar oportunidades |
| `POST` | `/api/tasks` | Crear oportunidad |
| `GET` | `/api/tasks/{id}` | Ver detalle de oportunidad |
| `POST` | `/api/tasks/{id}/applications` | Postularse a una oportunidad |
| `GET` | `/api/tasks/{id}/applications` | Ver postulaciones |
| `POST` | `/api/applications/{id}/accept` | Aceptar postulacion |
| `POST` | `/api/applications/{id}/reject` | Rechazar postulacion |
| `GET` | `/api/jobs` | Listar trabajos asignados |
| `POST` | `/api/jobs/{id}/deliveries` | Enviar entrega |
| `POST` | `/api/deliveries/{id}/approve` | Aprobar entrega |

## Como ejecutar la base en PostgreSQL

Crear la base:

```sql
CREATE DATABASE t4kash;
```

Ejecutar el esquema:

```powershell
psql -U postgres -d t4kash -f schema-postgresql.sql
```

## Conventional Commits

Ejemplos de commits para el equipo:

```text
docs: update project readme
feat: add postgresql database schema
feat: add marketplace task endpoints
feat: add android opportunity list screen
fix: correct application status flow
chore: configure local docker environment
```

## Estado actual

El proyecto esta en fase de organizacion inicial y definicion tecnica. La prioridad inmediata es crear el backend con Spring Boot usando el esquema PostgreSQL y luego conectar la aplicacion Android mediante Retrofit.
