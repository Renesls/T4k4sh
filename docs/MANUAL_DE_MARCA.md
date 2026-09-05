# Manual de marca — T4KASH

Fuente única de verdad para el color, la tipografía y el uso del logotipo de T4KASH.
Todos los valores de este documento están tomados del código y del archivo del
logotipo, no de supuestos: cada tabla indica su origen.

Última revisión: 2026-09-04.

---

## 1. La regla que resuelve el conflicto

El logotipo y las pantallas de la app nacieron con azules distintos. Este manual
**no elige uno y descarta el otro**: les asigna papeles separados, que es como ya
se venían usando en la landing y en la app de iOS.

| Papel | Color | Nombre | Dónde manda |
|---|---|---|---|
| **Color de marca** | `#0A74FF` | Azul T4KASH | Logotipo, ícono, landing, materiales de presentación, pitch, redes |
| **Color de producto** | `#5749FD` | Índigo T4KASH | Dentro de las pantallas: botones, estados activos, énfasis, foco |

**Regla corta:** el azul dice *quiénes somos*, el índigo dice *dónde tocar*.

Nunca se usan los dos como acento en la misma superficie. Si una pantalla ya usa
índigo para sus acciones, el azul solo aparece en el logotipo o en el encabezado
de marca; y si un material es puramente de marca (portada, ícono, banner), no
lleva índigo.

**Zona de frontera — el splash y el onboarding.** Son marca, no producto: fondo o
logotipo en azul `#0A74FF`; si llevan un botón, ese botón va en índigo.

---

## 2. Paleta

### 2.1 Núcleo

| Color | Hex | Uso | Origen |
|---|---|---|---|
| Azul de marca | `#0A74FF` | Logotipo, marca | `drawable-nodpi/t4kash_logo.png` (5.0 % de los píxeles opacos) |
| Azul profundo | `#0053C7` | Variante accesible del azul para texto | `landing/index.html` (`--blue-deep`) |
| Índigo de producto | `#5749FD` | Acciones y énfasis en pantalla | `ui/theme/Color.kt` (`T4Primary`) |
| Tinta | `#191919` | Texto principal | Logotipo (12.4 %) y `Color.kt` (`T4BrandDark`) |
| Menta | `#B9FF66` | Dinero y confirmaciones | `Color.kt` (`T4Mint`) |

La menta es **fondo, nunca texto**: sobre blanco da 1.20:1. Su tinta acompañante
es `#285000` (`T4MintDark`), que sobre menta da 7.85:1.

### 2.2 Superficies y texto

| Rol | Hex | Token Android | Token iOS |
|---|---|---|---|
| Fondo | `#F7F8FC` | `T4Background` | `BrandBackground` |
| Superficie | `#FFFFFF` | `T4Surface` | `BrandSurface` |
| Superficie variante | `#F0F1F7` | `T4SurfaceVariant` | `BrandSurfaceVariant` |
| Borde | `#E2E4EC` | `T4Border` | `BrandBorder` |
| Texto | `#191919` | `T4Text` | `BrandDark` |
| Texto atenuado | `#5E6070` | `T4TextMuted` | `BrandTextMuted` |
| Texto tenue | `#8A8C99` | `T4TextSoft` | `BrandTextSoft` |

### 2.3 Estado

| Rol | Hex | Token Android | Token iOS |
|---|---|---|---|
| Éxito | `#12A957` | `T4Success` | `BrandSuccess` |
| Aviso | `#FF7A1A` | `T4Orange` | `BrandOrange` |
| Error | `#BA1A1A` | `T4Danger` | `BrandDanger` |
| Ámbar (texto) | `#557500` | `T4Amber` | `BrandAmber` |
| Ámbar (fondo) | `#E5FFBE` | `T4AmberContainer` | `BrandAmberContainer` |

### 2.4 Variantes del índigo

| Rol | Hex | Token Android | Token iOS |
|---|---|---|---|
| Índigo suave | `#6B5EFF` | `T4PrimarySoft` | `BrandPrimarySoft` |
| Índigo oscuro | `#3D2FE0` | `T4PrimaryDark` | `BrandPrimaryDark` |
| Contenedor índigo | `#ECE9FF` | `T4PrimaryContainer` | `BrandPrimaryContainer` |

### 2.5 Fondo oscuro (solo landing)

La app es de tema claro en ambas plataformas (Android define solo
`lightColorScheme`; iOS lo replica). El tema oscuro existe únicamente en la
landing, y ahí el azul de marca se aclara para mantener el contraste:

| Rol | Claro | Oscuro | Origen |
|---|---|---|---|
| Azul | `#0A74FF` | `#4E98FF` | `landing/index.html` (`--blue`) |
| Azul profundo | `#0053C7` | `#93C1FF` | `--blue-deep` |
| Lavado de azul | `#E5EFFF` | `#0E2138` | `--blue-wash` |

`#4E98FF` sobre el papel oscuro `#0B0C10` da 6.76:1.

---

## 3. Contraste

Medidas WCAG 2.1 reales de esta paleta:

| Combinación | Ratio | Veredicto |
|---|---|---|
| `#0A74FF` sobre blanco | 4.24:1 | Solo texto grande (≥18.66 px negrita / ≥24 px) y elementos de UI |
| `#0053C7` sobre blanco | 6.86:1 | AA para cualquier texto |
| `#5749FD` sobre blanco | 5.56:1 | AA para cualquier texto |
| Blanco sobre `#5749FD` | 5.56:1 | AA — es el botón primario |
| `#191919` sobre blanco | 17.58:1 | AAA |
| `#5E6070` sobre `#F7F8FC` | 5.85:1 | AA |
| `#557500` sobre blanco | 5.34:1 | AA |
| `#285000` sobre menta | 7.85:1 | AAA |
| `#B9FF66` sobre blanco | 1.20:1 | **Nunca como texto** |

**Regla:** el azul de marca `#0A74FF` no se usa para texto corrido sobre fondo
claro. Para enlaces y texto azul, `#0053C7`.

---

## 4. Tipografía

| Uso | Familia | Dónde |
|---|---|---|
| Títulos | Archivo (500–800) | Landing |
| Texto | Public Sans (400–600) | Landing |
| Montos, estados, etiquetas | JetBrains Mono (400/500/700) | Landing |
| App Android | Tipografía del sistema | `ui/theme/Type.kt` (`FontFamily.Default`) |
| App iOS | San Francisco, diseño `.rounded` en títulos | `Core/Theme.swift` |

Las apps usan la tipografía del sistema a propósito: pesan menos y respetan el
tamaño de letra que el usuario configuró en su teléfono. Archivo y Public Sans
son la voz de la marca **fuera** de las apps.

Los montos siempre van con dígitos monoespaciados —`JetBrains Mono` en la
landing, `.monospacedDigit()` en iOS— para que no bailen al actualizarse.

---

## 5. Logotipo

- **Archivo maestro:** `mobile/app/src/main/res/drawable-nodpi/t4kash_logo.png`
  (347 × 305) — azul `#0A74FF` + tinta `#191919` sobre blanco.
- **Copia en iOS:** `ios/T4KASH/Resources/Assets.xcassets/BrandLogo.imageset/t4kash_logo.png`.
- **Variante:** `Logotipo Negro.png` (192 × 192) en la raíz del repositorio.

Reglas de uso:

- Aire mínimo alrededor: la altura de la "T" del logotipo.
- Fondo claro y liso. Sobre fondo oscuro o sobre foto, va la versión de una sola
  tinta en blanco.
- No se recolorea, ni se estira, ni se le añade sombra o contorno.
- No se pone sobre índigo `#5749FD`: los dos azules chocan.

---

## 6. Cómo se aplica en cada plataforma

| Plataforma | Marca `#0A74FF` | Producto `#5749FD` |
|---|---|---|
| Landing | `--blue` — titulares, acentos, sombras | `--indigo` — solo en las maquetas de pantalla |
| iOS | `Theme.Color.brand` (`BrandBlue`) | `Theme.Color.primary` (`BrandPrimary`), también `AccentColor` |
| Android | **falta el token** — ver pendientes | `T4Primary` |

---

## 7. Pendientes

- [ ] **Android no tiene token de marca.** `ui/theme/Color.kt` define el índigo y
      la tinta, pero no el azul `#0A74FF`. Falta `val T4Brand = Color(0xFF0A74FF)`
      para que las tres plataformas nombren el mismo color.
- [ ] **El ícono de la app sigue siendo el de plantilla de Android.**
      `drawable/ic_launcher_background.xml` es el verde `#3DDC84` por defecto y
      `ic_launcher_foreground.xml` el robot. Debe pasar a logotipo sobre azul de marca.
- [ ] **Falta la versión del logotipo en una sola tinta blanca** para fondos oscuros.
- [ ] **La landing enlaza a `.../releases`** en lugar del APK; cambiar al publicar.
