# Landing de T4KASH

Página estática de una sola pieza. Sin build ni dependencias: `index.html` contiene el
HTML, el CSS y el JS. Las tipografías se cargan desde Google Fonts.

## Ver en local

```bash
cd landing
python3 -m http.server 8099
# http://localhost:8099
```

## Publicar en GitHub Pages

Settings → Pages → Source: `main`, carpeta `/landing`.

## Origen de cada dato de la página

Todo el contenido sale del código de la rama `Carlos`, no de supuestos.

| Dato en la página | Origen |
|---|---|
| Comisión total 15 %: 10 % al cliente + 5 % retenido al estudiante | `PaymentService.java` (`clientFeePercent` / `studentFeePercent`), `application.properties` |
| Referencia de mercado 20 %–30 % | «revision mejorada plan presupuestal.xlsx», hoja *Valor Diferenciado* |
| El cliente paga el precio acordado + su 10 % | `total = agreedAmount + clientFee + processorFee + processorTax` |
| El estudiante recibe el precio acordado − su 5 % | `studentAmount = agreedAmount - studentFee` |
| Comisión 0 % en efectivo | `cash ? BigDecimal.ZERO : ...` en ambos porcentajes |
| Moneda NIO | `schema-postgresql.sql` (`moneda_cobro DEFAULT 'NIO'`) |
| Estados `PENDIENTE_PAGO`, `FONDOS_RETENIDOS`, `PAGO_LIBERADO` | `finance/service/PaymentService.java` |
| 92 endpoints REST | mapeos `@*Mapping` en `backend/src/main/java` (12 controladores) |
| 54 tablas | `CREATE TABLE` en `database/schema-postgresql.sql` |
| 131 pruebas: 108 backend + 23 Android | `@Test` en `backend/src/test` y `mobile/app/src/test` |
| Calificación mutua de 1 a 5 | `CalificacionService`, tabla `calificaciones`, `RatingActions.kt` |
| Código 10 min, 60 s entre reenvíos, 5 intentos/15 min | README principal, Inicio de Sesión en Dos Pasos |
| Radio 250 m – 5 km, tope C$ 1,000, doble confirmación | README principal, Uso del MVP |
| 24 categorías | catálogo `categorias_tarea` de `schema-postgresql.sql` |
| Radio máximo de búsqueda 50 km | README principal, Uso del MVP |
| Android 7.0+ (`minSdk 24`) | `mobile/app/build.gradle.kts` |
| Capturas de inicio, wallet y chat | `docs/capturas/` de la rama `ios-clean`, incrustadas como WebP en base64 |

## Pendientes de contenido

- El CTA apunta a `github.com/Renesls/T4k4sh/releases`. Cambiar por el enlace directo
  del APK cuando exista el release.
- **La app todavía muestra «Servicio T4KASH 1.0 %».** `WalletScreen.kt` imprime
  `porcentajeComisionPlataforma`, que ahora vale 15.00 y corresponde a la suma de los dos
  lados. Para el estudiante conviene mostrar solo su 5 %, así que ese texto necesita
  revisión aparte.
- El radar de tareas rápidas sigue reconstruido en HTML/CSS. La captura
  `docs/capturas/tareas-rapidas.png` **no se puede usar**: muestra el estado vacío con el
  error «Tu cuenta no tiene permiso para realizar esta accion» y el mapa del emulador
  (Mountain View, California).
- La landing no enlaza el Swagger (`t4k4sh.onrender.com/swagger-ui/index.html`) ni corrige
  el enlace del pie a `t4k4sh.onrender.com/api/`, que no es una página navegable.
- Las cifras de la sección «Plataforma» son las de la rama `Carlos`. En `ios-clean` hay
  además una app iOS completa (26 vistas, 60 pruebas) y el esquema llega a 54 tablas; esas
  ramas todavía no están integradas.

## Identidad visual

| Elemento | Valor | Origen |
|---|---|---|
| Azul de marca | `#0A74FF` | `mobile/app/src/main/res/drawable-nodpi/t4kash_logo.png` |
| Tinta | `#191919` | logotipo y `ui/theme/Color.kt` |
| Índigo de producto | `#5749FD` | `ui/theme/Color.kt` (solo dentro de las pantallas) |
| Menta | `#B9FF66` | `ui/theme/Color.kt` (dinero y confirmaciones) |

El logotipo usa `#0A74FF` y la app usa `#5749FD`: el azul es el color de **marca** y el
índigo el color de **producto** (solo dentro de las pantallas). La regla completa, con
contrastes y uso del logotipo, está en [`docs/MANUAL_DE_MARCA.md`](../docs/MANUAL_DE_MARCA.md).

Para texto azul sobre fondo claro se usa `#0053C7` (`--blue-deep`): `#0A74FF` sobre blanco
da 4.24:1 y no alcanza el 4.5:1 que pide WCAG AA para texto normal.

## Tipografía

Archivo (títulos) · Public Sans (texto) · JetBrains Mono (montos, estados y etiquetas).
