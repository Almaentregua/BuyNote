# Historia 3.1 — Tildar / destildar items

**Estado**: Completada  
**Fecha**: 2026-05-01

---

## Criterios de aceptación — verificación

| Criterio | Estado |
|---|---|
| Checkbox en cada item | ✅ (ya existía en la UI) |
| Estado persiste en BD | ✅ |

---

## Archivos modificados

- `ui/screens/listdetail/ListDetailViewModel.kt` — nuevo `toggleItem(id, isChecked)`: obtiene el item y llama `updateItem(item.copy(isChecked = isChecked))`
- `ui/screens/listdetail/ListDetailScreen.kt` — `ItemsList` recibe `onItemToggle`; `Checkbox.onCheckedChange` conectado a `viewModel.toggleItem()`

---

## Build

```
BUILD SUCCESSFUL in 30s — sin warnings
```
