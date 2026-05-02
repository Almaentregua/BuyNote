# Reporte — Historia 4.3: Búsqueda en catálogo por código → agregar a lista

## Qué se implementó

### `ListDetailViewModel.kt`
- `listId` pasa de `private val` a `val` para que la Screen lo lea al navegar al scanner.
- Nuevo `sealed class BarcodeResult { Found(product), NotFound(barcode) }`.
- Nuevo `_barcodeResult: MutableSharedFlow<BarcodeResult>` + `barcodeResult` expuesto.
- Nueva función `handleScannedBarcode(barcode)`:
  - Llama a `productRepository.findByBarcode(barcode)`.
  - Emite `Found` o `NotFound` según el resultado.

### `ListDetailScreen.kt`
- `snackbarHostState` y `scope` movidos antes de los `LaunchedEffect` (necesario para que los lambdas de coroutine los capturen sin error de compilación).
- **LaunchedEffect — leer barcode del savedStateHandle**: observa `getStateFlow<String?>("barcode", null)` del `currentBackStackEntry`. Al recibir un valor no-nulo lo consume (`remove`) y llama a `viewModel.handleScannedBarcode`.
- **LaunchedEffect — colectar barcodeResult**:
  - `Found` → `pendingProduct = result.product` → dispara el `QuantityPickerDialog` existente.
  - `NotFound` → snackbar "Código no encontrado en el catálogo".
- **Opción "Escanear código de barras"** agregada al bottom sheet de "Agregar item" (con ícono `QrCodeScanner`). Navega a `Routes.scanner(viewModel.listId)`.
- **Snackbar de confirmación** en `QuantityPickerDialog.onConfirm`: tras `addItemFromCatalog`, muestra `"\"${product.name}\" agregado a la lista"`.

## Flujo completo

```
FAB "Agregar" → sheet → "Escanear código de barras"
  → ScannerScreen (ticket 4.2)
  → detección → savedStateHandle["barcode"] = rawValue → popBackStack()
  → ListDetailScreen retoma el foco
  → LaunchedEffect lee barcode del savedStateHandle
  → viewModel.handleScannedBarcode(barcode)
      ├─ encontrado: BarcodeResult.Found → pendingProduct = product
      │    → QuantityPickerDialog → confirm → addItemFromCatalog + snackbar
      └─ no encontrado: BarcodeResult.NotFound → snackbar "no encontrado"
```

## Archivos modificados

- `ui/screens/listdetail/ListDetailViewModel.kt`
- `ui/screens/listdetail/ListDetailScreen.kt`

## Criterios de aceptación verificados

- [x] Lookup en `ProductRepository.findByBarcode()`
- [x] Si existe: pide cantidad/unidad (QuantityPickerDialog) y agrega
- [x] Snackbar de confirmación al agregar
- [x] Snackbar "no encontrado" cuando el código no está en el catálogo
- [x] Botón para acceder al scanner desde el detalle de lista
- [x] `assembleDebug` exitoso sin errores
