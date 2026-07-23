# Despliegue de T4KASH

Este documento deja preparado el camino para publicar el MVP con servicios gratuitos:

```text
Android -> Render API -> Supabase PostgreSQL
```

Supabase se usa como base de datos PostgreSQL. Render se usa para ejecutar la API Spring Boot en un contenedor Docker.

## 1. Preparar Supabase

1. Crear un proyecto en Supabase.
2. Abrir SQL Editor.
3. Copiar y ejecutar el contenido de `database/schema-postgresql.sql`.
4. Ir a la seccion de conexion del proyecto.
5. Copiar los datos de conexion para PostgreSQL.

Para Render se recomienda usar el Session Pooler de Supabase porque funciona mejor con hostings que solo tienen IPv4.

Formato recomendado para Spring Boot:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-region.pooler.supabase.com:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres.PROJECT_REF
SPRING_DATASOURCE_PASSWORD=password-de-la-base
```

Evitar Transaction Pooler para esta API salvo que se ajuste la configuracion de JPA, porque puede dar problemas con prepared statements.

## 2. Preparar Render

El archivo `render.yaml` vive en la raiz del repositorio y le indica a Render que debe construir el backend desde `backend/`.

Pasos:

1. Subir los cambios a GitHub.
2. Entrar a Render.
3. Crear un nuevo Blueprint o Web Service desde el repositorio.
4. Verificar que Render detecte `render.yaml`.
5. Configurar estas variables de entorno en Render:

| Variable | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC de Supabase con `sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de Supabase |
| `SPRING_DATASOURCE_PASSWORD` | Password de Supabase |

Las demas variables ya tienen valores definidos en `render.yaml`.

## 3. Probar la API publicada

Cuando Render termine el deploy, probar:

```text
https://NOMBRE-DEL-SERVICIO.onrender.com/api/health
https://NOMBRE-DEL-SERVICIO.onrender.com/swagger-ui
```

En el plan gratuito, el primer request puede tardar porque el servicio se duerme despues de un rato sin uso.

## 4. Conectar Android a la API publicada

En desarrollo local, Android sigue usando por defecto:

```text
http://10.0.2.2:8080/api/
```

Para compilar apuntando a Render:

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug -PT4KASH_API_BASE_URL="https://NOMBRE-DEL-SERVICIO.onrender.com/api/"
```

Tambien se puede usar una variable de entorno:

```powershell
$env:T4KASH_API_BASE_URL="https://NOMBRE-DEL-SERVICIO.onrender.com/api/"
.\gradlew.bat :app:assembleDebug
```

La URL debe incluir `/api/` al final porque los endpoints del backend viven bajo ese prefijo.

## 5. Problemas comunes

- Si Render no conecta con Supabase, revisar que la URL use `sslmode=require`.
- Si falla el usuario de Supabase, revisar el formato `postgres.PROJECT_REF`.
- Si Android no carga datos, confirmar que la URL base termina en `/api/`.
- Si el primer request tarda, es normal en servicios gratuitos que se duermen por inactividad.

## Referencias

- Render Blueprint YAML Reference: https://render.com/docs/blueprint-spec
- Render Free Instances: https://render.com/docs/free
- Supabase PostgreSQL Connections: https://supabase.com/docs/guides/database/connecting-to-postgres
- Supabase Pricing: https://supabase.com/pricing
