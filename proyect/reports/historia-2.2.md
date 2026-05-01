# Historia 2.2 — Pantalla "Listas activas" como home

**Estado**: Completada  
**Fecha**: 2026-05-01

---

## Criterios de aceptación — verificación

| Criterio | Estado |
|---|---|
| LazyColumn con nombre, fecha de creación, contador "X items / Y listos" | ✅ |
| FAB para crear nueva lista | ✅ |
| Acceso a "Historial", "Catálogo" y "Categorías" via menú | ✅ |

---

## Archivos creados

- `ui/screens/lists/ActiveListsViewModel.kt` — StateFlow de listas activas con contadores
- `ui/screens/lists/ActiveListsScreen.kt` — reemplaza el placeholder con la pantalla completa

## Archivos modificados

- `data/db/dao/ShoppingListItemDao.kt` — nueva query `getCountsPerList()` + data class `ListItemCounts`
- `domain/repository/ShoppingListRepository.kt` — nuevo método `getItemCountsPerList()`
- `data/repository/ShoppingListRepositoryImpl.kt` — implementación de `getItemCountsPerList()`

---

## Decisiones de diseño

**Query de contadores agregada (`getCountsPerList`)**: en lugar de hacer un Flow por cada lista para sus ítems, se hace una sola query SQL que devuelve `(listId, total, checked)` agrupado. El ViewModel combina este Flow con el de listas activas mediante `combine`. Más eficiente y evita N subscripciones.

**Menú overflow (MoreVert)**: los tres destinos de navegación (Catálogo, Historial, Categorías) quedan en un `DropdownMenu`. En la home no tiene sentido saturar la TopAppBar con íconos de navegación secundaria; el criterio decía "menú/drawer" y el dropdown es la opción más liviana de Material 3.

**FAB con `onCreateList: () -> Unit`**: el botón existe y es funcional en su firma, pero la historia 2.3 conectará el diálogo de creación. La pantalla acepta el callback como parámetro para no mezclar la responsabilidad de UI con la lógica de creación.

**Barra de progreso solo cuando hay ítems**: si la lista tiene 0 ítems se muestra "Sin items" y no se muestra la `LinearProgressIndicator` (evita dividir por cero y muestra información relevante).

**Fecha formateada en el ViewModel**: la conversión de `Long` a string legible queda en el ViewModel, no en la UI. El Composable solo recibe strings listos para mostrar.

---

## Build

```
BUILD SUCCESSFUL in ~51s
```
Sin warnings relevantes.
