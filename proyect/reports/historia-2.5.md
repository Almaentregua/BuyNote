# Historia 2.5 — Agregar item desde catálogo

**Estado**: Completada  
**Fecha**: 2026-05-01

---

## Criterios de aceptación — verificación

| Criterio | Estado |
|---|---|
| Picker con búsqueda (reutiliza componente de la 1.5) | ✅ |
| Al elegir, pide cantidad y unidad antes de confirmar | ✅ |
| El item aparece en el detalle de la lista | ✅ |

---

## Archivos modificados

- `ui/screens/listdetail/ListDetailViewModel.kt` — items en `UiState`, join products para nombre, picker search con debounce 250ms, `addItemFromCatalog()`
- `ui/screens/listdetail/ListDetailScreen.kt` — `ItemsList` (LazyColumn), `CatalogPickerSheet` (ModalBottomSheet), `QuantityPickerDialog` (AlertDialog)

---

## Decisiones de diseño

**Resolución de nombre via `combine` con `productRepository.getAll()`**: en lugar de una query JOIN en Room, el ViewModel combina el Flow de items con el Flow de todos los productos usando `associateBy { it.id }`. Es reactivo (si cambia el nombre de un producto, se refleja) y evita agregar una capa de `@Relation` en el DAO. Para el tamaño de catálogo esperado (catálogo personal, pocas decenas de productos) el costo es irrelevante.

**Flujo sin bottom sheets anidados**: el picker de catálogo y el sheet de opciones son mutuamente excluyentes — al abrir el picker se cierra el sheet de opciones primero. Esto evita el anti-patrón de `ModalBottomSheet` dentro de otro.

**`pendingProduct: Product?` como trigger del diálogo de cantidad**: cuando el usuario elige un producto en el picker, `pendingProduct` se setea con ese producto. El `AlertDialog` se muestra si `pendingProduct != null`, y al confirmar o cancelar se limpia. Estado local en el Composable; no ensucia el ViewModel.

**`QuantityPickerDialog`**: `OutlinedTextField` con `KeyboardType.Decimal` para cantidad + `ExposedDropdownMenuBox` para unidad. El botón "Agregar" está deshabilitado hasta que la cantidad sea un número positivo válido. Default: 1 unidad.

**Checkbox no-interactivo en la lista**: los items muestran `Checkbox(onCheckedChange = { /* 3.1 */ })` — visualmente correcto desde ya, la lógica de tildado se agrega en la historia 3.1 sin cambiar la estructura del componente.

**`QuantityUnit.displayLabel()`**: función de extensión definida en el package `listdetail` (visible en Screen y ViewModel). Se moverá a un lugar compartido si 2.6 la necesita.

**`resetPickerQuery()`**: al cerrar el picker (por dismiss o por selección) se limpia la query para que la próxima apertura empiece vacía.

---

## Build

```
BUILD SUCCESSFUL in 23s — sin warnings
```
