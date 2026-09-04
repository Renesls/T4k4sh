# T4KASH — Auditoría de migración a iOS

Documento de diagnóstico previo a la implementación de la aplicación iOS.
Todo lo que aparece aquí fue verificado leyendo el código real del repositorio
(`backend/`, `mobile/`, `database/`), no supuesto.

- **Commit base auditado:** `51e76e1` (`main`)
- **Rama de trabajo:** `feature/ios-migration`
- **Fecha de auditoría:** 2026-09-04

---

## 1. Arquitectura actual

```
T4k4sh/
├── backend/     API REST Spring Boot (Java 21) — 169 archivos .java
├── database/    Esquema PostgreSQL 16 (schema-postgresql.sql)
├── mobile/      App Android (Kotlin + Jetpack Compose) — 65 archivos .kt, ~18.5k líneas
├── docs/        Diagramas, capturas y esta auditoría
├── ios/         App iOS (nueva, esta migración)
└── render.yaml  Despliegue de la API en Render
```

### Backend

Spring Boot modular por dominio. Cada módulo separa
`controller` / `service` / `repository` / `entity` / `dto`:

| Módulo | Responsabilidad |
|---|---|
| `identity` | Registro, login 2FA, verificación de correo, recuperación, perfiles públicos, verificación estudiantil, KYC (Didit) |
| `marketplace` | Categorías, tareas, tareas rápidas, postulaciones, trabajos, entregas, adjuntos |
| `communication` | Conversaciones, mensajes, notificaciones internas |
| `finance` | Wallet, pagos Pagadito, efectivo, disputas, reembolsos, desembolsos |
| `network` | Feed universitario: publicaciones, comentarios, reacciones, guardados |
| `moderation` | Reportes de tareas |
| `admin` | Panel administrativo, revisión de reportes y disputas |
| `health` | Health check |

**Puntos arquitectónicos relevantes para iOS:**

- **No hay Spring Security.** La autenticación se resuelve con un
  `HandlerMethodArgumentResolver` propio (`identity/web/CurrentUserArgumentResolver`)
  que lee el header `Authorization` y lo resuelve contra `AuthenticatedUserService`
  mediante la anotación `@CurrentUser(role = "...")`. La autorización por rol es
  **por parámetro de método**, no por filtro de cadena.
- **Los errores son RFC 7807 `ProblemDetail`** (`exception/GlobalExceptionHandler`).
  El cuerpo de error trae `{ type, title, status, detail, instance }`; el mensaje
  legible siempre está en `detail`.
- **Las fechas viajan como `LocalDateTime` sin zona horaria**, serializadas por
  Jackson en ISO-8601 local: `yyyy-MM-dd'T'HH:mm:ss` con fracción de segundo
  opcional. **No llevan `Z` ni offset.** Android lo confirma en
  `ui/DisplayFormatters.kt` (`API_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"`).
- **Los importes son `BigDecimal`** en backend y llegan como número JSON.
- **Paginación:** `?page=&size=` con `size` máximo 100 (`config/PaginationSupport`).
  Las respuestas son **arrays planos**, no objetos `Page` — no hay metadatos de
  paginación en el cuerpo.
- CORS abierto a `app.cors.allowed-origins` (`*` por defecto).
- Multipart limitado a 10 MB por archivo / 11 MB por request.

### Android

MVVM sobre Compose, sin inyección de dependencias (objetos `object` singleton):

```
ui/
├── service/      Interfaces Retrofit + RetrofitClient (OkHttp, Gson)
├── repository/   Envuelven las llamadas en ApiResult<T>
├── viewmodel/    AndroidX ViewModel + StateFlow
├── model/        DTOs (data class)
├── screen/       Composables de pantalla
├── components/   Componentes compartidos
├── navigation/   NavGraph + Routes (rutas string)
├── session/      UserSession + SecureTokenStore (AES/GCM en AndroidKeyStore)
└── theme/        Color.kt, Theme.kt, Type.kt
```

---

## 2. Inventario completo de funcionalidades

### Identidad y sesión
- Registro institucional con detección automática de universidad por dominio de correo.
- Carreras filtradas por universidad detectada.
- Verificación de correo por código de 6 dígitos + reenvío.
- Login en **dos pasos**: `login` (envía código) → `login/verify` (entrega token).
- Reenvío del código de login.
- Recuperación de contraseña por código (`password/forgot` → `password/reset`).
- Sesión persistente con token opaco; logout revoca en servidor.
- Perfil público por `@username`, cambio de nombre de usuario con bloqueo de 30 días.
- Verificación estudiantil: subida de comprobante + revisión administrativa.
- Verificación de identidad (KYC) alojada en Didit con webhook firmado.
- Roles: `ADMIN`, `EVALUADOR` (asignados por email en configuración del backend).

### Marketplace
- 24 categorías de oportunidades.
- Publicación, edición y cancelación de tareas.
- Tipos de oportunidad: `TAREA` y `RAPIDA`.
- Modalidades: `REMOTA`, `PRESENCIAL`, `HIBRIDA`.
- Visibilidad: `PUBLICA`.
- Ubicación (lat/lon + dirección de referencia) para presencial e híbrida.
- Mapa con marcadores y radio de búsqueda.
- Tareas rápidas: radar por cercanía, ventana de 24 h, entrega en 3 h,
  radio 0.25–5 km, pago máximo C$1000, asignación inmediata por reclamo.
- Postulaciones (máx. 3 intentos por tarea), aceptación/rechazo.
- Trabajos asignados y entregas con comentarios y revisiones.
- Adjuntos en tareas, entregas y verificación estudiantil.
- Reportes de tareas con 5 categorías.

### Comunicación
- Conversaciones ligadas a tarea/trabajo con contador de no leídos.
- Mensajes con estado de lectura y marca de conversación leída.
- Notificaciones internas (listar, marcar una, marcar todas).

### Red universitaria (feed)
- Feed con tres alcances: `PARA_TI`, `CONEXIONES`, `UNIVERSIDAD`.
- Publicaciones de 9 tipos, 3 visibilidades, comentarios anidados.
- Reacciones: `ME_GUSTA`, `APOYO`, `CELEBRAR`, `INTERESA`.
- Guardados, edición y borrado propio.

### Finanzas
- Wallet calculada: balance disponible, fondos retenidos, total ganado.
- Pagos con Pagadito Sandbox (checkout web) o efectivo (solo presencial/rápida).
- Comisión de plataforma visible antes de confirmar.
- Refresh de estado de pago, disputas, reembolsos y desembolsos.

### Administración
- Resumen de métricas, cancelación de tareas, revisión de reportes,
  resolución de disputas, aprobación/rechazo de verificaciones estudiantiles.

---

## 3. Pantallas existentes (Android)

| Ruta | Pantalla | Archivo |
|---|---|---|
| `splash` | Arranque y restauración de sesión | `SplashScreen.kt` |
| `login` | Credenciales | `LoginScreen.kt` |
| `login-verification` | Código 2FA de acceso | `LoginVerificationScreen.kt` |
| `register` | Registro institucional | `RegisterScreen.kt` |
| `verify-email` | Activación de cuenta | `VerifyEmailScreen.kt` |
| `forgot-password` / `reset-password` | Recuperación | `PasswordRecoveryScreens.kt` |
| `marketplace` | Listado de oportunidades | `MarketplaceScreen.kt` |
| `opportunity/{taskId}` | Detalle y postulación | `OpportunityDetailScreen.kt` |
| `opportunity/{taskId}/edit` · `post` | Publicar/editar | `PostTaskScreen.kt` |
| `opportunity-map` | Mapa de oportunidades | `OpportunityMapScreen.kt` |
| `quick-tasks` | Radar de tareas rápidas | `OpportunityMapScreen.kt` |
| `opportunity/{taskId}/applications` | Gestión de postulaciones | `ApplicationManagementScreen.kt` |
| `application-sent` | Postulaciones enviadas | `ApplicationSentScreen.kt` |
| `profile/jobs` · `profile/jobs/{jobId}` | Trabajos y detalle | `AssignedJobsScreen.kt`, `JobDetailScreen.kt` |
| `profile/publications/{filter}` | Mis publicaciones | `MyPublicationsScreen.kt` |
| `wallet` | Billetera | `WalletScreen.kt` |
| `chat` · `chat/{conversationId}` | Conversaciones y mensajes | `ChatScreens.kt` |
| `notifications` | Notificaciones | `PlaceholderScreens.kt` |
| `network` | Feed universitario | `NetworkScreen.kt` |
| `profile` | Perfil propio | `ProfileScreen.kt` |
| `profile/public/{username}` | Perfil público | `PublicProfileScreen.kt` |
| `profile/settings` | Ajustes | `SettingsScreen.kt` |
| `profile/identity-verification` | KYC Didit | `IdentityVerificationScreen.kt` |
| `profile/admin` | Panel administrativo | `AdminScreen.kt` |

---

## 4. Flujos de usuario

1. **Alta:** registro → código por correo → cuenta activa → sesión.
2. **Acceso:** correo + contraseña → código 2FA → token opaco → sesión persistente.
3. **Publicación:** cliente crea tarea (categoría, presupuesto, modalidad,
   ubicación si aplica) → queda `PUBLICADA` y visible en marketplace y mapa.
4. **Postulación:** estudiante postula (mensaje + precio propuesto opcional) →
   estado `PENDIENTE`, máximo 3 intentos.
5. **Asignación:** cliente acepta indicando `metodoPago` (`PAGADITO` | `EFECTIVO`)
   → se crea `TrabajoAsignado` (`EN_PROCESO`) y el `Pago` asociado.
6. **Pago protegido:** checkout Pagadito → webhook confirma → `FONDOS_RETENIDOS`.
7. **Efectivo:** solo presencial/rápida, confirmación de recepción por ambas partes.
8. **Entrega:** estudiante registra entrega (`ENVIADA`) con adjuntos →
   cliente aprueba (`APROBADA`) o pide cambios (`CAMBIOS_SOLICITADOS`).
9. **Liberación:** al aprobar, el monto pasa a balance disponible del estudiante.
10. **Tarea rápida:** cliente publica `RAPIDA` con ubicación → estudiantes cercanos
    la ven en el radar → el primero que reclama queda asignado de inmediato.
11. **Disputa:** cualquiera de las partes abre disputa sobre el pago →
    un administrador resuelve liberando o reembolsando.

---

## 5. Endpoints utilizados

Base URL de producción: `https://t4k4sh.onrender.com/api/`
Autenticación: `Authorization: Bearer <token opaco>`

### `identity`
| Método | Ruta | Auth |
|---|---|---|
| POST | `/auth/register` | público |
| POST | `/auth/verify-email` | público |
| POST | `/auth/resend-verification` | público |
| POST | `/auth/login` | público |
| POST | `/auth/login/verify` | público |
| POST | `/auth/login/resend` | público |
| POST | `/auth/password/forgot` | público |
| POST | `/auth/password/reset` | público |
| GET | `/auth/me` | sesión |
| POST | `/auth/logout` | sesión |
| GET | `/identity/universities` | público |
| GET | `/identity/universities/{universityId}/careers` | público |
| GET | `/profiles/{username}` | sesión |
| PUT | `/profiles/me/username` | sesión |
| GET | `/student-verifications/me` | sesión |
| POST | `/student-verifications/me/attachments` (multipart `file`) | sesión |
| GET | `/student-verifications/pending` | ADMIN |
| POST | `/student-verifications/{userId}/approve` | ADMIN |
| POST | `/student-verifications/{userId}/reject` | ADMIN |
| GET | `/identity-verifications/me` | sesión |
| POST | `/identity-verifications/me/session?origen=PERFIL` | sesión |
| POST | `/identity-verifications/me/refresh` | sesión |
| POST | `/identity-verifications/webhook` | firma Didit |
| GET | `/identity-verifications/callback` | HTML de retorno |

### `marketplace`
| Método | Ruta | Auth |
|---|---|---|
| GET | `/categories` | público |
| GET | `/tasks?page=&size=` | público |
| GET | `/tasks/{idTarea}` | público |
| POST | `/tasks` | sesión |
| PUT | `/tasks/{idTarea}` | sesión (dueño) |
| DELETE | `/tasks/{idTarea}` | sesión (dueño) |
| GET | `/quick-tasks/nearby?latitude=&longitude=&radiusKm=` | sesión |
| POST | `/quick-tasks/{idTarea}/claim` | sesión |
| GET | `/tasks/{idTarea}/applications` | sesión (dueño) |
| POST | `/tasks/{idTarea}/applications` | sesión |
| GET | `/applications/me` | sesión |
| POST | `/applications/{idPostulacion}/accept` | sesión (dueño) |
| POST | `/applications/{idPostulacion}/reject` | sesión (dueño) |
| GET | `/jobs` | sesión |
| GET | `/jobs/{idTrabajo}/deliveries` | sesión |
| POST | `/jobs/{idTrabajo}/deliveries` | sesión (estudiante) |
| POST | `/deliveries/{idEntrega}/approve` | sesión (cliente) |
| POST | `/deliveries/{idEntrega}/request-changes` | sesión (cliente) |
| POST | `/deliveries/{idEntrega}/comments` | sesión |
| GET | `/tasks/{taskId}/attachments` | sesión |
| POST | `/tasks/{taskId}/attachments` (multipart `file`) | sesión |
| GET | `/jobs/{jobId}/attachments` | sesión |
| GET | `/deliveries/{deliveryId}/attachments` | sesión |
| POST | `/deliveries/{deliveryId}/attachments` (multipart `file`) | sesión |
| GET | `/attachments/{attachmentId}/download` | sesión (bytes) |

### `communication`
| Método | Ruta |
|---|---|
| GET | `/conversations?page=&size=` |
| GET | `/conversations/{conversationId}/messages?page=&size=` |
| POST | `/conversations/{conversationId}/messages` |
| POST | `/conversations/{conversationId}/read` |
| GET | `/notifications?page=&size=` |
| POST | `/notifications/{notificationId}/read` |
| POST | `/notifications/read-all` |

### `finance`
| Método | Ruta |
|---|---|
| GET | `/wallet?page=&size=` |
| GET | `/jobs/{idTrabajo}/payment` |
| POST | `/jobs/{idTrabajo}/payment/checkout` |
| POST | `/jobs/{idTrabajo}/payment/cash/confirm-receipt` |
| POST | `/payments/{idPago}/refresh` |
| POST | `/payments/{idPago}/disputes` |
| GET | `/disputes/me?page=&size=` |
| POST | `/payments/pagadito/webhook` (servidor a servidor) |
| GET | `/payments/pagadito/return` (HTML de retorno) |

### `network`
| Método | Ruta |
|---|---|
| GET | `/network/feed?alcance=&page=&size=` |
| GET | `/network/saved?page=&size=` |
| GET | `/network/posts/{idPublicacion}` |
| POST | `/network/posts` |
| PUT | `/network/posts/{idPublicacion}` |
| DELETE | `/network/posts/{idPublicacion}` |
| PUT · DELETE | `/network/posts/{idPublicacion}/reaction` |
| PUT · DELETE | `/network/posts/{idPublicacion}/saved` |
| GET · POST | `/network/posts/{idPublicacion}/comments` |
| PUT · DELETE | `/network/comments/{idComentario}` |

### `moderation` y `admin`
| Método | Ruta | Auth |
|---|---|---|
| POST | `/tasks/{taskId}/reports` | sesión |
| GET | `/reports/me?page=&size=` | sesión |
| GET | `/admin/summary` | ADMIN |
| GET | `/admin/tasks?page=&size=` | ADMIN |
| DELETE | `/admin/tasks/{taskId}` | ADMIN |
| GET | `/admin/reports?page=&size=` | ADMIN |
| POST | `/admin/reports/{reportId}/review` | ADMIN |
| GET | `/admin/payment-disputes` | ADMIN |
| POST | `/admin/payment-disputes/{disputeId}/resolve` | ADMIN |
| GET | `/health` | público |

---

## 6. Modelos utilizados

Los modelos iOS son `Codable` con **los mismos nombres de campo** que los
`record` de Java (`backend/src/main/java/com/t4kash/api/**/dto`). No se renombró
ningún campo ni se inventó ninguno.

| Dominio | Modelos |
|---|---|
| Identidad | `AuthenticatedUserResponse`, `AuthResponse`, `LoginChallengeResponse`, `RegistrationResponse`, `MessageResponse`, `UniversityResponse`, `CareerResponse`, `PublicIdentityResponse`, `PublicProfileResponse`, `StudentVerificationResponse`, `IdentityVerificationStatusResponse`, `IdentityVerificationSessionResponse` |
| Marketplace | `CategoriaResponse`, `TaskResponse`, `QuickTaskResponse`, `ApplicationResponse`, `JobResponse`, `DeliveryResponse`, `DeliveryCommentResponse`, `DeliveryReviewResponse`, `AttachmentResponse` |
| Finanzas | `WalletResponse`, `PaymentResponse`, `WalletMovementResponse`, `PaymentDisputeResponse`, `RefundResponse`, `PayoutResponse`, `CheckoutResponse` |
| Comunicación | `ConversationResponse`, `MessageResponse`, `NotificationResponse` |
| Red | `PostResponse`, `CommentResponse` |
| Moderación / Admin | `ReportResponse`, `AdminSummaryResponse` |

**Constantes de dominio verificadas en el backend** (no inventadas):

- Modalidad: `REMOTA`, `PRESENCIAL`, `HIBRIDA` — `TaskService.MODALIDADES_VALIDAS`
- Tipo de oportunidad: `TAREA`, `RAPIDA` — `TaskService`, `PostTaskScreen.kt`
- Estado de tarea: `PUBLICADA`, `ASIGNADA`, `CERRADA`, `CANCELADA`
- Estado de postulación: `PENDIENTE`, `ACEPTADA`, `RECHAZADA`, `CANCELADA_TAREA`
- Estado de trabajo: `PENDIENTE_PAGO`, `EN_PROCESO`, `FINALIZADO`
- Estado de entrega: `ENVIADA`, `APROBADA`, `CAMBIOS_SOLICITADOS`
- Estado de pago: `PENDIENTE_PAGO`, `FONDOS_RETENIDOS`, `PAGO_FALLIDO`,
  `PAGO_CANCELADO`, `PAGO_EXPIRADO`, `PAGO_REVOCADO`, `EN_DISPUTA`, `REEMBOLSADO`
- Método de pago: `PAGADITO`, `EFECTIVO`
- Solución de disputa solicitada: `PAGO_ESTUDIANTE`, `REEMBOLSO_CLIENTE`
- Decisión de disputa (admin): `LIBERAR_ESTUDIANTE`, `REEMBOLSAR_CLIENTE`
- Tipos de publicación: `TEXTO`, `IMAGEN`, `VIDEO`, `PROYECTO`, `LOGRO`,
  `PREGUNTA`, `RECURSO`, `EVENTO`, `COMPARTIDA` — `NetworkService.POST_TYPES`
- Visibilidad de publicación: `PUBLICA`, `CONEXIONES`, `UNIVERSIDAD`
- Alcance de feed: `PARA_TI`, `CONEXIONES`, `UNIVERSIDAD`
- Reacciones: `ME_GUSTA`, `APOYO`, `CELEBRAR`, `INTERESA` — `NetworkService.REACTIONS`
- Categorías de reporte: `CONTENIDO_INAPROPIADO`, `POSIBLE_ESTAFA`,
  `INFORMACION_FALSA`, `PUBLICACION_DUPLICADA`, `OTRO` — `ReportService`
- Estados de revisión de reporte: `RESUELTO`, `DESCARTADO`

---

## 7. Dependencias externas

| Servicio | Uso | Quién lo llama |
|---|---|---|
| Render | Hosting de la API | — |
| Supabase PostgreSQL | Base de datos | backend |
| Supabase Storage | Bucket privado de adjuntos | backend |
| Brevo | Envío de códigos por correo | backend |
| Didit | KYC alojado (documento, liveness, face match) | backend + webview del cliente |
| Pagadito Sandbox | Checkout WSPG y webhooks firmados | backend + webview del cliente |
| OpenFreeMap / MapLibre | Teselas de mapa | **solo Android** |

**Hecho clave para la migración:** ninguna integración externa se consume con SDK
nativo desde el cliente. Didit y Pagadito se resuelven **abriendo una URL web** que
entrega el backend (`urlVerificacion`, `checkoutUrl`). Por eso **no se necesita
ningún SDK de terceros en iOS**: basta `SFSafariViewController` (`ASWebAuthenticationSession`
no aplica porque no hay redirección a esquema propio). Esto elimina el riesgo
principal de las Fases 10 y 11.

---

## 8. Funciones exclusivas de Android

| Elemento Android | Detalle |
|---|---|
| `SecureTokenStore` | AES/GCM con clave en `AndroidKeyStore` sobre `SharedPreferences` |
| `UserSession` | `SharedPreferences` para datos de perfil |
| Retrofit + OkHttp + Gson | Cliente HTTP y serialización |
| `HttpLoggingInterceptor` | Log de red en debug |
| MapLibre Compose + OpenFreeMap | Mapa y teselas |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Permisos de ubicación |
| Navigation Compose (rutas string) | Navegación |
| `AndroidViewModel` + `StateFlow` | Estado |
| `ActivityResultContracts` | Selección de archivos |
| `Theme.T4KASH` (Material 3) | Tema |
| `windowSoftInputMode=adjustResize` + `KeyboardSupport.kt` | Manejo de teclado |

## 9. Equivalentes recomendados para iOS

| Android | iOS (implementado) |
|---|---|
| `SecureTokenStore` (AndroidKeyStore) | **Keychain** (`kSecClassGenericPassword`, `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`) |
| `SharedPreferences` (perfil) | `UserDefaults` — solo datos no sensibles |
| Retrofit + OkHttp + Gson | `URLSession` + `Codable` + `APIClient` propio (async/await) |
| `HttpLoggingInterceptor` | Logger propio que **nunca** imprime el token ni cuerpos con credenciales |
| MapLibre + OpenFreeMap | **MapKit** (`Map` de SwiftUI, `MKLocalSearch`, anotaciones) |
| Permisos de ubicación en manifiesto | `NSLocationWhenInUseUsageDescription` + `CLLocationManager` |
| Navigation Compose | `NavigationStack` + `TabView` con rutas tipadas (`enum Route: Hashable`) |
| `ViewModel` + `StateFlow` | `@Observable` (Observation) + `@MainActor` |
| `ActivityResultContracts` | `PhotosPicker`, `UIImagePickerController` (cámara), `fileImporter` |
| Material 3 | Design system propio en SwiftUI respetando la identidad T4KASH |
| `adjustResize` | Comportamiento nativo + `ScrollView` y `.scrollDismissesKeyboard` |
| Checkout/KYC en navegador | `SFSafariViewController` envuelto en `UIViewControllerRepresentable` |

## 10. Funciones que requieren cambios en backend

**Ninguna para la paridad funcional.** Todos los endpoints que Android consume
funcionan sin cambios para iOS: el token es opaco y agnóstico de plataforma, los
adjuntos son multipart estándar y los flujos de Didit/Pagadito son web.

Cambios **no realizados** (documentados, fuera del alcance de esta migración
porque tocarían el backend en producción):

1. **Notificaciones push (APNs).** El backend **no tiene** registro de dispositivos
   ni envío push — el README lo marca como *Pendiente* también para Android (FCM).
   No existe tabla de tokens de dispositivo ni endpoint de registro. Implementarlo
   exigiría: tabla `dispositivos_usuario`, endpoint `POST /api/devices`, y un
   emisor APNs en el backend. **No se inventó ningún endpoint.** La app iOS
   implementa el equivalente disponible hoy: **notificaciones internas** vía
   `/api/notifications` con refresco y badge.
2. **Refresh token.** No existe. El backend entrega un token opaco con
   `fechaExpiracion` y responde `401` al vencer. iOS replica exactamente el
   comportamiento de Android: al recibir `401` se limpia la sesión y se vuelve a login.
3. **Paginación con metadatos.** Las respuestas son arrays planos. iOS pagina por
   convención "si llegaron menos de `size` elementos, no hay más páginas".

## 11. Riesgos técnicos

| Riesgo | Impacto | Mitigación aplicada |
|---|---|---|
| Fechas sin zona horaria | Desfase de horas al mostrar | Decoder que interpreta `LocalDateTime` en **zona local del dispositivo**, igual que Android (`SimpleDateFormat` sin TZ) |
| `BigDecimal` como `Double` | Pérdida de precisión en dinero | Se decodifica a `Decimal` mediante `Decimal(string:)` sobre el literal JSON, evitando el binario de `Double` |
| Sin refresh token | Cierres de sesión abruptos | Interceptor 401 → limpieza de sesión + retorno a login con mensaje claro |
| Free tier de Render | Arranque en frío ~50 s | `timeoutIntervalForRequest` amplio (45 s) y estados de carga explícitos |
| Chat sin tiempo real | Mensajes no llegan solos | Igual que Android: **polling**; en iOS con `Task` cancelable cada 5 s solo con la vista visible |
| KYC/Pagos en webview | Flujo fuera de la app | `SFSafariViewController` + refresco de estado explícito al volver |
| Adjuntos ≤ 10 MB | Rechazo del servidor | Validación previa en cliente + compresión JPEG de imágenes |
| Sin push | Sin avisos en segundo plano | Documentado; notificaciones internas implementadas |

## 12. Estado de migración por funcionalidad

| Funcionalidad | Android | Backend | iOS | Estado |
|---|---|---|---|---|
| Registro institucional | existente | existente | implementado | completo |
| Detección de universidad por dominio | existente | existente | implementado | completo |
| Verificación de correo + reenvío | existente | existente | implementado | completo |
| Login (paso 1) | existente | existente | implementado | completo |
| Login 2FA (paso 2) + reenvío | existente | existente | implementado | completo |
| Recuperación de contraseña | existente | existente | implementado | completo |
| Sesión persistente | existente | existente | implementado (Keychain) | completo |
| Logout | existente | existente | implementado | completo |
| Perfil propio | existente | existente | implementado | completo |
| Perfil público por @usuario | existente | existente | implementado | completo |
| Cambio de nombre de usuario | existente | existente | implementado | completo |
| Verificación estudiantil (subida) | existente | existente | implementado | completo |
| KYC Didit | existente | existente | implementado (Safari + refresh) | completo |
| Catálogo de categorías | existente | existente | implementado | completo |
| Marketplace (listado + filtros) | existente | existente | implementado | completo |
| Detalle de oportunidad | existente | existente | implementado | completo |
| Publicar tarea | existente | existente | implementado | completo |
| Editar tarea | existente | existente | implementado | completo |
| Cancelar tarea | existente | existente | implementado | completo |
| Mapa de oportunidades | existente (MapLibre) | existente | implementado (MapKit) | completo |
| Selector de ubicación | existente | existente | implementado (MapKit + búsqueda) | completo |
| Tareas rápidas (radar) | existente | existente | implementado | completo |
| Reclamar tarea rápida | existente | existente | implementado | completo |
| Postular a tarea | existente | existente | implementado | completo |
| Mis postulaciones | existente | existente | implementado | completo |
| Gestión de postulaciones | existente | existente | implementado | completo |
| Aceptar con método de pago | existente | existente | implementado | completo |
| Rechazar postulación | existente | existente | implementado | completo |
| Trabajos asignados | existente | existente | implementado | completo |
| Detalle de trabajo | existente | existente | implementado | completo |
| Crear entrega | existente | existente | implementado | completo |
| Aprobar entrega | existente | existente | implementado | completo |
| Solicitar cambios | existente | existente | implementado | completo |
| Comentar entrega | existente | existente | implementado | completo |
| Adjuntos (subir) | existente | existente | implementado (fotos/cámara/archivos) | completo |
| Adjuntos (descargar) | existente | existente | implementado | completo |
| Wallet | existente | existente | implementado | completo |
| Checkout Pagadito | existente | existente | implementado | completo |
| Confirmar efectivo | existente | existente | implementado | completo |
| Refrescar pago | existente | existente | implementado | completo |
| Abrir disputa | existente | existente | implementado | completo |
| Conversaciones | existente | existente | implementado | completo |
| Mensajes + envío | existente | existente | implementado | completo |
| Marcar conversación leída | existente | existente | implementado | completo |
| Notificaciones internas | existente | existente | implementado | completo |
| Feed (3 alcances) | existente | existente | implementado | completo |
| Crear/editar/borrar publicación | existente | existente | implementado | completo |
| Reacciones | existente | existente | implementado | completo |
| Guardados | existente | existente | implementado | completo |
| Comentarios | existente | existente | implementado | completo |
| Reportar tarea | existente | existente | implementado | completo |
| Mis reportes | existente | existente | implementado | completo |
| Panel admin (resumen) | existente | existente | implementado | completo |
| Admin: revisar reportes | existente | existente | implementado | completo |
| Admin: cancelar tarea | existente | existente | implementado | completo |
| Admin: resolver disputas | existente | existente | implementado | completo |
| Admin: verificaciones estudiantiles | existente | existente | implementado | completo |
| Notificaciones push | pendiente | **no existe** | no aplica | bloqueado por backend |
| Calificaciones y reputación | pendiente | pendiente | no aplica | fuera de alcance |
| Puntos y beneficios | solo BD | solo BD | no aplica | fuera de alcance |
