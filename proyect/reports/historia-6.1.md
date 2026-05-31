# Historia 6.1 — Mostrar marca del producto en los items de la lista

## Cambios realizados

**`ListDetailViewModel.kt`**
- Agregado campo `brandName: String?` a `ShoppingListItemUiModel`.
- Populado con `product?.brand` al mapear cada item.

**`ListDetailScreen.kt`**
- `ItemRow`: el `supportingContent` ahora muestra `"Marca · cantidad"` cuando la marca existe, o solo cantidad cuando no.

## Criterios verificados
- Items con producto que tiene marca: muestran "Arcor · 2 unidades".
- Items con producto sin marca: muestran solo "2 unidades".
- Items ad-hoc: `brandName = null`, muestran solo cantidad (sin cambio de comportamiento).
- Aplica tanto a listas activas como al historial (mismo composable `ItemRow`).
- Compila sin errores (`./gradlew :app:compileDebugKotlin`).
