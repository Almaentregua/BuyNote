# Historia 2.4 — Pantalla "Detalle de lista" (vacía)

**Estado**: Completada  
**Fecha**: 2026-05-01

---

## Criterios de aceptación — verificación

| Criterio | Estado |
|---|---|
| Top bar con nombre de la lista + contador de progreso | ✅ |
| Lista de items vacía con CTA | ✅ |
| Dos opciones para sumar item: "desde catálogo" y "ad-hoc" | ✅ |

---

## Archivos creados

- `ui/screens/listdetail/ListDetailViewModel.kt`

## Archivos modificados

- `ui/screens/listdetail/ListDetailScreen.kt` — reemplaza el placeholder con la pantalla completa

---

## Decisiones de diseño

**`listId` via `SavedStateHandle`**: el ViewModel lee el argumento de navegación desde `SavedStateHandle["listId"]`, patron estándar de Hilt Navigation. No se necesita cambiar `AppNavigation`.

**`flow { emit(repository.getById(listId)) }` + `combine` con items**: el nombre de la lista es una carga puntual (no hay edición en este scope), pero el contador de progreso sí es reactivo a los items. El `combine` mezcla el nombre estático con el Flow de items para armar el `UiState` completo.

**Contador en el subtítulo de la TopAppBar**: se muestra en el slot `title` como un `Column` con título y subtítulo (`labelSmall`). Solo aparece cuando `totalItems > 0` para no mostrar "0 de 0 listos" con lista vacía.

**`ModalBottomSheet` como punto de entrada único para agregar**: tanto el FAB "Agregar" como las opciones del estado vacío abren el mismo bottom sheet con las dos opciones. El empty state muestra visualmente las opciones directamente para que el usuario entienda qué puede hacer; el FAB las agrupa en un sheet una vez que ya hay items.

**Callbacks `onAddFromCatalog` / `onAddAdHoc` como parámetros con default `{}`**: la pantalla ya tiene la firma lista para que 2.5 y 2.6 conecten la lógica real sin tocar el resto de la UI. `AppNavigation` los puede proporcionar en su momento.

---

## Pendiente para historias siguientes

- 2.5 conecta `onAddFromCatalog` → picker de catálogo
- 2.6 conecta `onAddAdHoc` → formulario ad-hoc
- 2.5+ reemplaza el `Box` vacío del body por el `LazyColumn` de items

---

## Build

```
BUILD SUCCESSFUL in 27s — sin warnings
```
