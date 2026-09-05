# T4KASH para iOS

Aplicación iOS nativa de T4KASH, escrita en Swift y SwiftUI contra la **misma API
REST** que usa la aplicación Android. No sustituye ni modifica el proyecto
Android: convive con él en el mismo repositorio.

- Diagnóstico previo: [`docs/IOS_MIGRATION_AUDIT.md`](../docs/IOS_MIGRATION_AUDIT.md)
- API: `https://t4k4sh.onrender.com/api/`

---

## Requisitos

| Herramienta | Versión mínima |
|---|---|
| macOS | Sonoma 14 |
| Xcode | 16.0 |
| iOS de destino | 17.0 |
| Swift | 5.9 (lenguaje 5) |

No hay dependencias de terceros: ni CocoaPods, ni Carthage, ni Swift Package
Manager. Todo se resuelve con frameworks del sistema (`SwiftUI`, `MapKit`,
`CoreLocation`, `PhotosUI`, `SafariServices`, `Security`, `Observation`).

## Abrir y ejecutar

```bash
cd ios
open T4KASH.xcodeproj
```

El proyecto usa **grupos sincronizados con el sistema de archivos**
(`PBXFileSystemSynchronizedRootGroup`, Xcode 16). Cualquier archivo `.swift` que
se añada bajo `T4KASH/` entra al target automáticamente, sin editar el
`.pbxproj` a mano.

Desde la línea de comandos:

```bash
xcodebuild -project T4KASH.xcodeproj -scheme T4KASH \
  -destination 'platform=iOS Simulator,name=iPhone 16' build

xcodebuild -project T4KASH.xcodeproj -scheme T4KASH \
  -destination 'platform=iOS Simulator,name=iPhone 16' test
```

## Configuración

Todo lo configurable son *build settings* del proyecto, sin secretos en el código:

| Build setting | Valor por defecto | Para qué sirve |
|---|---|---|
| `T4KASH_API_BASE_URL` | `https://t4k4sh.onrender.com/api/` | URL base de la API. Se inyecta en `Info.plist` (`T4KASHAPIBaseURL`) y la lee `AppConfig`. |
| `T4KASH_BUNDLE_ID` | `com.t4kash.app` | Bundle Identifier de la app; el target de pruebas usa `<id>.tests`. |
| `T4KASH_DEVELOPMENT_TEAM` | vacío | Equipo de firma. Vacío permite compilar para simulador. |

Apuntar a un backend local:

```bash
xcodebuild -project T4KASH.xcodeproj -scheme T4KASH \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  T4KASH_API_BASE_URL='http://localhost:8080/api/' build
```

> El simulador de iOS comparte la red del Mac, así que usa `localhost`
> directamente. El `10.0.2.2` del emulador de Android **no** aplica aquí.
>
> `Info.plist` declara `NSAllowsArbitraryLoads = false`. Para probar contra un
> backend local por **HTTP sin TLS** hay que añadir temporalmente una excepción
> `NSExceptionDomains` para `localhost`; no la dejes en una compilación de
> Release.

## Arquitectura

```
ios/
├── T4KASH.xcodeproj
├── T4KASH/
│   ├── App/            Punto de entrada, raíz de composición y arranque
│   ├── Core/           Configuración, tema, formateo y constantes de dominio
│   ├── Networking/     APIClient, peticiones, errores, multipart, codificación
│   ├── Models/         Modelos Codable equivalentes a los DTO del backend
│   ├── Services/       Keychain, sesión, ubicación, cámara, adjuntos, Safari
│   ├── Repositories/   Un repositorio por módulo del backend
│   ├── ViewModels/     Estado observable por pantalla (@Observable)
│   ├── Views/          Pantallas SwiftUI agrupadas por dominio
│   ├── Components/     Componentes compartidos y estados de carga/vacío/error
│   ├── Navigation/     TabView y pilas de navegación
│   ├── Utilities/      Validaciones y detección de dominio institucional
│   └── Resources/      Info.plist y Assets.xcassets
└── T4KASHTests/        Pruebas unitarias
```

**MVVM con repositorios.** Las vistas no conocen HTTP: hablan con ViewModels
`@Observable`, que hablan con repositorios, que usan un único `APIClient`.

## Decisiones técnicas

| Decisión | Motivo |
|---|---|
| iOS 17 como mínimo | Permite `@Observable`, `NavigationStack`, la API de `Map` con contenido y `ContentUnavailableView`-style propios sin capas de compatibilidad. |
| Sin dependencias externas | El backend no exige ningún SDK de cliente: Pagadito y Didit son flujos web. Menos superficie de riesgo y cero configuración de paquetes. |
| MapKit en vez de MapLibre | Android usa MapLibre con teselas de OpenFreeMap; en iOS, MapKit es nativo, no necesita proveedor de teselas ni clave, e incluye búsqueda de lugares y geocodificación inversa. |
| Keychain para el token | Equivale al `SecureTokenStore` de Android (AES/GCM sobre AndroidKeyStore). `UserDefaults` solo guarda el perfil visible, que no es sensible. |
| `SFSafariViewController` para pagos y KYC | El backend entrega `checkoutUrl` y `urlVerificacion`; al cerrar el navegador se consulta el estado real contra la API en vez de confiar en la redirección. |
| Sondeo en el chat | El backend no expone websockets ni SSE. Se replica el comportamiento de Android con un bucle cancelable atado al ciclo de vida de la vista. |
| Tema claro | Android define solo `lightColorScheme`; mantener una sola apariencia conserva la identidad y evita divergencias de diseño. |
| Solo retrato | Coincide con la app Android y con el uso real del producto. |

## Qué hay que verificar en Xcode

Esta migración se desarrolló en Linux, **sin acceso a macOS ni a Xcode**, así que
el proyecto no ha sido compilado ni ejecutado. Antes de darlo por bueno hay que
recorrer esta lista en un Mac:

**Compilación**
- [ ] `xcodebuild ... build` termina sin errores para simulador.
- [ ] Corregir avisos de concurrencia si se activa el modo de lenguaje Swift 6
      (el proyecto está fijado a Swift 5, donde son avisos y no errores).
- [ ] `xcodebuild ... test` pasa las pruebas unitarias.

**Recursos y permisos**
- [ ] `Assets.xcassets` carga sin avisos y `AppIcon` (1024×1024, sin canal alfa)
      se acepta. El icono actual es un marcador generado desde el logotipo:
      **sustitúyelo por el arte definitivo antes de publicar**.
- [ ] Los diálogos de ubicación, cámara y fotos muestran los textos de
      `Info.plist`.
- [ ] La cámara solo se ofrece en dispositivo real (`CameraPicker.isAvailable`).

**Firma y distribución**
- [ ] Asignar `T4KASH_DEVELOPMENT_TEAM` y verificar la firma automática.
- [ ] Comprobar que el Bundle Identifier no choca con el de otra app del equipo.
- [ ] La app no declara *entitlements* especiales: no usa push, iCloud, App
      Groups ni Sign in with Apple. No hace falta archivo `.entitlements`.

**Funcional, contra el backend real**
- [ ] Registro con correo institucional, detección de universidad y activación.
- [ ] Login en dos pasos y persistencia de la sesión al reabrir la app.
- [ ] Publicar, editar y cancelar una oportunidad; selector de ubicación.
- [ ] Postular, aceptar con Pagadito y con efectivo, entregar y aprobar.
- [ ] Checkout de Pagadito Sandbox y refresco del estado al volver.
- [ ] Verificación de identidad con Didit de principio a fin.
- [ ] Subida y descarga de adjuntos, incluido el límite de 10 MB.
- [ ] Radar de tareas rápidas con permiso concedido y denegado.
- [ ] Chat entre dos cuentas y contadores de no leídos.
- [ ] Panel de administración con una cuenta incluida en
      `APP_AUTH_ADMIN_EMAILS`.

## Limitaciones conocidas

1. **Sin notificaciones push.** El backend no tiene registro de dispositivos ni
   emisor de push (`docs/IOS_MIGRATION_AUDIT.md`, sección 10). No se inventó
   ningún endpoint: la app usa las notificaciones internas de
   `/api/notifications`. Añadir APNs exige cambios de backend y base de datos.
2. **Sin refresh token.** El backend entrega un token opaco sin renovación. Un
   `401` limpia la sesión y devuelve al login, igual que en Android.
3. **Paginación sin metadatos.** Las respuestas son arrays planos; la app asume
   que hay más páginas mientras lleguen tantos elementos como el tamaño pedido.
4. **Pagadito y Didit dependen de credenciales de entorno.** Con
   `APP_PAGADITO_ENABLED=false` o `APP_DIDIT_ENABLED=false` el backend no emite
   URLs y esas pantallas mostrarán el error que devuelva la API.
5. **Arranque en frío de Render.** El plan gratuito puede tardar; los tiempos de
   espera están en 45 s y las pantallas muestran estado de carga explícito.
6. **Calificaciones, reputación y puntos** no están implementados en el backend,
   así que tampoco en iOS.
