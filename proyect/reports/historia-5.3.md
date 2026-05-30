# Reporte — Historia 5.3: Escanear código desde formulario de producto

## Cambios implementados

### `AppNavigation.kt`
- Nuevo helper `Routes.scanner()` = `"scanner"` (sin listId). El argumento `listId` tiene `defaultValue = -1L`, por lo que Navigation Compose lo resuelve correctamente sin necesidad de pasarlo.

### `ProductFormScreen.kt`
- Nuevo import: `Icons.Outlined.QrCodeScanner`, `Routes`.
- Nuevo `LaunchedEffect(Unit)` que lee `navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<String?>("barcode", null)`. Cuando llega un valor no nulo, lo remueve del handle y llama `viewModel.onBarcodeChange(barcode)`.
- `ProductForm` recibe nuevo parámetro `onScanClick: () -> Unit`, pasado desde `ProductFormScreen` como `{ navController.navigate(Routes.scanner()) }`.
- Campo "Código de barras" tiene `trailingIcon` con `IconButton(QrCodeScanner)` que llama `onScanClick`.

## Flujo completo

1. Usuario toca el ícono de scanner en el campo barcode.
2. Navega a `ScannerScreen` (sin listId).
3. El scanner detecta el código, escribe en `previousBackStackEntry?.savedStateHandle["barcode"]` y hace `popBackStack()`.
4. `ProductFormScreen` recibe el barcode vía `LaunchedEffect` y lo carga en el campo.
5. Si el código ya existe en otro producto, el error de constraint UNIQUE aparece al intentar guardar (comportamiento preexistente).

## Verificado

- `assembleDebug` exitoso en 2m 18s sin errores.
