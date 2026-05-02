# Reporte — Historia 4.1: Permisos de cámara

## Qué se implementó

**`AndroidManifest.xml`**
- Se agregó `<uses-permission android:name="android.permission.CAMERA" />`.

**`ScannerScreen.kt`** — reescritura completa con máquina de estados de 4 valores:

| Estado | Cuándo ocurre | UI |
|---|---|---|
| `Checking` | Mientras se lanza el diálogo del sistema por primera vez | `CircularProgressIndicator` |
| `Granted` | Permiso concedido | Placeholder "Cámara lista" (para 4.2) |
| `Rationale` | Denegado una vez; `shouldShowRationale = true` | Explicación + botón "Conceder permiso" |
| `PermanentlyDenied` | "No volver a preguntar"; `shouldShowRationale = false` | Mensaje + botón "Ir a Ajustes" que abre `ACTION_APPLICATION_DETAILS_SETTINGS` |

## Flujo de estados

```
Entrar a ScannerScreen
  └─ ya concedido?          → Granted
  └─ shouldShowRationale?   → Rationale (reintento manual)
  └─ primera vez            → lanza diálogo → callback:
        ├─ concedido         → Granted
        ├─ shouldShowRationale → Rationale
        └─ denegado siempre  → PermanentlyDenied
```

## Archivos modificados

- `app/src/main/AndroidManifest.xml` — permiso de cámara declarado
- `app/src/main/java/com/martinjm/buynote/ui/screens/scanner/ScannerScreen.kt` — reescritura completa

## Criterios de aceptación verificados

- [x] Permiso declarado en manifest
- [x] Rationale con explicación y botón de reintento
- [x] "Denegado para siempre" con CTA a ajustes del sistema
- [x] `assembleDebug` exitoso sin errores
