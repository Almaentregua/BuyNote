# Historia 3.3 — Agrupar items por categoría

**Estado**: Completada  
**Fecha**: 2026-05-01

---

## Criterios de aceptación — verificación

| Criterio | Estado |
|---|---|
| Toggle entre "orden de carga" y "agrupado por categoría" | ✅ |
| Items ad-hoc o sin categoría van al final | ✅ |

---

## Archivos modificados

- `ui/screens/listdetail/ListDetailViewModel.kt` — `SortMode` enum; `CategoryRepository` inyectado; `combine` extendido a 4 fuentes; `categoryName: String?` en `ShoppingListItemUiModel`; `sortMode: StateFlow<SortMode>` + `toggleSortMode()`
- `ui/screens/listdetail/ListDetailScreen.kt` — `sortMode` colectado; botón toggle en `TopAppBar.actions` (solo visible cuando hay items); `ItemsList` recibe `sortMode` y maneja ambos modos

---

## Decisiones de diseño

**Toggle en TopAppBar solo cuando `totalItems > 0`**: no tiene sentido mostrar el botón con la lista vacía.

**Agrupación en el composable, no en el ViewModel**: el ViewModel provee la lista plana con `categoryName`. La separación visual (groupBy + sortedWith) es un concern de presentación. El ViewModel solo expone `sortMode` como preferencia de UI.

**`sortedWith(compareBy({ it.key == null }, { it.key }))`**: primero ordena por si la clave es null (false < true, así que no-null va primero), luego alfabético. Resultado: categorías ordenadas A→Z, "Sin categoría" al final.

**Dentro de cada grupo en BY_CATEGORY: `sortedBy { it.isChecked }`**: los items tildados bajan al final del grupo (false < true). Sin divider "Listos" dentro de cada grupo — el alpha es suficiente, agregar más dividers sería ruido visual.

**Icono del toggle**: muestra lo que el tap va a hacer (no el estado actual). En INSERTION → icono de categoría; en BY_CATEGORY → icono de lista.

---

## Build

```
BUILD SUCCESSFUL in 21s — sin warnings
```
