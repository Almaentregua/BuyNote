# Historia 2.7 — Editar cantidad / unidad de un item

**Estado**: Completada  
**Fecha**: 2026-05-01

---

## Criterios de aceptación — verificación

| Criterio | Estado |
|---|---|
| Tap en el item abre edición (bottom sheet) | ✅ |
| Solo se editan cantidad, unidad y `customName` (si era ad-hoc) | ✅ |

---

## Archivos modificados

- `ui/screens/listdetail/ListDetailViewModel.kt` — `ShoppingListItemUiModel` extendido con `quantity`, `unit`, `customName`, `isAdHoc`; nuevo `updateItem(id, customName?, quantity, unit)`
- `ui/screens/listdetail/ListDetailScreen.kt` — `ItemsList` recibe `onItemClick`; nuevo `EditItemContent` (ModalBottomSheet); estado `editingItem` en el composable raíz

---

## Decisiones de diseño

**`editingItem` como estado local del Composable**: el item seleccionado para edición es estado de UI efímero, no tiene sentido en el ViewModel. `var editingItem by remember { mutableStateOf<ShoppingListItemUiModel?>(null) }` es suficiente.

**Pre-populado de cantidad sin decimales innecesarios**: si `quantity % 1.0 == 0.0`, se muestra como entero (`"2"` en vez de `"2.0"`). Consistente con el comportamiento de `formatQuantity`.

**Campo nombre solo para items ad-hoc**: `EditItemContent` recibe el `ShoppingListItemUiModel` completo y renderiza el campo nombre condicionalmente con `item.isAdHoc`. Para items de catálogo el nombre no es editable (pertenece al producto).

**`updateItem` usa `getItemById` + `.copy()`**: preserva `productId`, `listId`, `isChecked` y solo sobreescribe los campos editables. Sin riesgo de pérdida accidental de datos.

---

## Build

```
BUILD SUCCESSFUL in 23s — sin warnings
```
