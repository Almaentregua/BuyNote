# Reporte — Historia 4.4: Si no existe → form de producto con código precargado

## Qué se implementó

### `AppNavigation.kt` / `Routes`
- Ruta `PRODUCT_FORM` extendida con `&barcode={barcode}`.
- `Routes.productForm()` acepta el nuevo parámetro opcional `barcode: String?`.
- `navArgument("barcode")` agregado como `StringType` nullable en el `composable`.

### `ProductFormViewModel.kt`
- `private val initialBarcode: String?` leído del `savedStateHandle["barcode"]`.
- `val fromScanner: Boolean = !isEditing && initialBarcode != null` — diferencia el flujo scanner del flujo "guardar también al catálogo", evitando que este último dispare la adición automática a la lista.
- En el `init`, cuando no se está editando, se precarga también `barcode` en el estado si vino del scanner.
- `navigateBack: SharedFlow<Unit>` → `SharedFlow<Long?>`: emite el ID del nuevo producto al insertar, `null` al editar o eliminar.

### `ProductFormScreen.kt`
- `navigateBack.collect` ahora recibe `Long?`.
- Si `newProductId != null && viewModel.fromScanner` → lo deposita en `navController.previousBackStackEntry?.savedStateHandle["newProductId"]`.
- Siempre hace `popBackStack()`.

### `ListDetailViewModel.kt`
- Nueva función `addProductFromScanner(productId: Long)`: hace `productRepository.getById(productId)` y emite `BarcodeResult.Found(product)`, reutilizando el flujo del `QuantityPickerDialog` ya implementado en 4.3.

### `ListDetailScreen.kt`
- `BarcodeResult.NotFound` ya no muestra snackbar: navega a `Routes.productForm(barcode = result.barcode)`.
- Nuevo `LaunchedEffect` que observa `savedStateHandle["newProductId"]` como `StateFlow<Long?>`. Al recibir un ID válido, lo consume (`remove`) y llama a `viewModel.addProductFromScanner(productId)`.

## Flujo completo 4.3 + 4.4

```
FAB → "Escanear código de barras"
  → ScannerScreen → detecta barcode → savedStateHandle["barcode"] → popBackStack()
  → ListDetail observa barcode → handleScannedBarcode()
      ├─ ENCONTRADO (4.3):
      │    BarcodeResult.Found → pendingProduct → QuantityPickerDialog
      │    → addItemFromCatalog + snackbar confirmación
      │
      └─ NO ENCONTRADO (4.4):
           BarcodeResult.NotFound → navegar a ProductForm(barcode = ...)
           → formulario con barcode precargado → usuario llena y guarda
           → navigateBack emite newProductId
           → savedStateHandle["newProductId"] = newProductId → popBackStack()
           → ListDetail observa newProductId → addProductFromScanner(productId)
           → BarcodeResult.Found → pendingProduct → QuantityPickerDialog
           → addItemFromCatalog + snackbar confirmación
```

## Decisión: `fromScanner`

`ProductFormScreen` se abre también desde "Guardar también al catálogo" (flujo ad-hoc). En ese caso NO debe propagarse `newProductId` de vuelta a la lista (el ítem ya fue agregado como ad-hoc). El flag `fromScanner = !isEditing && initialBarcode != null` diferencia ambos casos correctamente.

## Archivos modificados

- `ui/navigation/AppNavigation.kt`
- `ui/screens/productform/ProductFormViewModel.kt`
- `ui/screens/productform/ProductFormScreen.kt`
- `ui/screens/listdetail/ListDetailViewModel.kt`
- `ui/screens/listdetail/ListDetailScreen.kt`

## Criterios de aceptación verificados

- [x] Navega a `product_form` con barcode prellenado cuando no existe en catálogo
- [x] Al guardar, vuelve al detalle de la lista y abre el QuantityPickerDialog para sumar el producto recién creado
- [x] El flujo "Guardar también al catálogo" (ad-hoc) no es afectado
- [x] `assembleDebug` exitoso sin errores
