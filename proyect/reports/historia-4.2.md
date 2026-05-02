# Reporte — Historia 4.2: Pantalla scanner con CameraX + ML Kit

## Qué se implementó

### `AndroidManifest.xml`
- Se agregó `<uses-permission android:name="android.permission.VIBRATE" />`.

### `ScannerScreen.kt` — reescritura completa

Se consolidaron las historias 4.1 y 4.2 en el mismo archivo. Cuando el permiso es concedido, se muestra el scanner fullscreen en lugar del placeholder anterior.

**Nuevos composables:**

| Composable | Responsabilidad |
|---|---|
| `CameraScanner` | Box fullscreen: preview + botón back flotante con fondo semitransparente |
| `CameraPreview` | `AndroidView` con `PreviewView` de CameraX + análisis continuo con ML Kit |

**Función top-level:**
- `processImageProxy` — extrae `InputImage` del `ImageProxy` y lo pasa al `BarcodeScanner` de ML Kit. Anotada con `@ExperimentalGetImage`.

## Flujo al detectar un código

```
Frame de cámara → ML Kit (EAN-13 / EAN-8 / UPC-A)
  └─ rawValue encontrado
        ├─ AtomicBoolean.compareAndSet(false, true) → evita detecciones duplicadas
        ├─ vibrate(context) — 150ms, DEFAULT_AMPLITUDE
        ├─ savedStateHandle["barcode"] = rawValue  ← listo para que 4.3 lo consuma
        └─ navController.popBackStack()
```

## Decisiones técnicas

- **Fullscreen real**: se elimina el `Scaffold` cuando el permiso está concedido; el botón back flota sobre el preview con fondo semitransparente (`CircleShape`, alpha 35%).
- **`AtomicBoolean`**: garantiza que aunque el analizador dispare varios frames seguidos con el mismo código, solo se ejecuta una vez el callback.
- **`savedStateHandle`**: el barcode se deja en el backstack entry del destino anterior para que 4.3 lo levante sin necesidad de retocar el scanner.
- **`STRATEGY_KEEP_ONLY_LATEST`**: descarta frames si el análisis no termina a tiempo, evitando acumulación de work.

## Archivos modificados

- `app/src/main/AndroidManifest.xml` — permiso VIBRATE
- `app/src/main/java/com/martinjm/buynote/ui/screens/scanner/ScannerScreen.kt` — implementación completa

## Criterios de aceptación verificados

- [x] Preview de cámara fullscreen
- [x] Detección de barcodes EAN-13, EAN-8, UPC-A
- [x] Vibración al detectar (150ms)
- [x] Cierre del scanner al detectar
- [x] `assembleDebug` exitoso sin errores
