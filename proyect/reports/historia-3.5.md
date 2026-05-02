# Historia 3.5 — Historial

**Estado**: Completada  
**Fecha**: 2026-05-01

---

## Criterios de aceptación — verificación

| Criterio | Estado |
|---|---|
| Pantalla con listas completadas, ordenadas por `completedAt` desc | ✅ |
| Tap muestra detalle read-only (sin edición ni borrado) | ✅ |
| Mostrar fecha de creación y de completado | ✅ |

---

## Archivos modificados

- `ui/screens/history/HistoryViewModel.kt` — nuevo; `HistoryListUiModel`, `HistoryUiState`; `combine(getCompleted(), getItemCountsPerList())` ordenado por `completedAt` desc; formato `"d 'de' MMM yyyy"`
- `ui/screens/history/HistoryScreen.kt` — reemplaza placeholder; `HistoryList` (LazyColumn con nombre, fechas y contador); `EmptyHistory` con ícono y texto
- `ui/screens/listdetail/ListDetailViewModel.kt` — `isCompleted: Boolean` en `ListDetailUiState`, derivado de `list?.status == ListStatus.COMPLETED`
- `ui/screens/listdetail/ListDetailScreen.kt` — FAB oculto, "Finalizar" oculto y `ItemsList` con `isReadOnly` cuando `isCompleted`; `ItemRow` refactorizado: en modo read-only omite `SwipeToDismissBox`, pasa `onCheckedChange = null` al checkbox y no aplica `.clickable`

---

## Decisiones de diseño

**La ruta de navegación es la misma (`list_detail/{id}`)**: no se agregó un parámetro `readOnly` a la ruta. El `ListDetailScreen` detecta el estado leyendo `uiState.isCompleted` que viene del repositorio. Más robusto — la fuente de verdad es el dato, no la URL.

**`itemContent` como lambda `@Composable` local en `ItemRow`**: evita duplicar el `ListItem` para los casos read-only y normal, ya que solo el wrapper (`SwipeToDismissBox` vs nada) cambia.

**`onCheckedChange = null` para checkbox read-only**: Material3 interpreta `null` como deshabilitado (visualmente atenuado). Correcto para el modo historial.

**Formato de fecha con año en historial** (`"d 'de' MMM yyyy"`): el historial puede acumular listas de años anteriores; el año es necesario para distinguirlas. En el home se usa `"d 'de' MMM"` (sin año) porque todas son recientes.

---

## Build

```
BUILD SUCCESSFUL in 29s — sin warnings
```
