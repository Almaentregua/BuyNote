# Historia 2.3 — Crear nueva lista

**Estado**: Completada  
**Fecha**: 2026-05-01

---

## Criterios de aceptación — verificación

| Criterio | Estado |
|---|---|
| Diálogo simple con campo "nombre" | ✅ |
| Validación: nombre obligatorio | ✅ |
| Al crear, navega al detalle de la lista recién creada | ✅ |

---

## Archivos modificados

- `ui/screens/lists/ActiveListsViewModel.kt` — `createList(name)` + `navigateToDetail: SharedFlow<Long>`
- `ui/screens/lists/ActiveListsScreen.kt` — diálogo `CreateListDialog`, colección del evento de navegación, FAB conectado

---

## Decisiones de diseño

**`SharedFlow<Long>` para navegación post-creación**: el ViewModel emite el ID de la lista recién insertada como evento de navegación de una sola vez. La pantalla lo colecta en un `LaunchedEffect` y navega a `list_detail/{id}`. Esto evita guardar el ID en el `UiState` (que es estado persistente, no un evento).

**Diálogo manejado con estado local en el Composable**: `showCreateDialog` es un `remember { mutableStateOf(false) }` dentro del Composable, no en el ViewModel, porque es estado de UI puro (si se gira la pantalla mientras el diálogo está abierto, es razonable que se cierre).

**`FocusRequester` en el `TextField`**: el teclado aparece automáticamente al abrir el diálogo via `LaunchedEffect(Unit) { focusRequester.requestFocus() }`. Mejora la UX sin lógica extra.

**Botón "Crear" deshabilitado si el nombre está en blanco**: `enabled = name.isNotBlank()`. No se muestra mensaje de error hasta que el usuario haya escrito algo y lo haya borrado (`isError = name.isBlank() && name.isNotEmpty()`), evitando errores prematuros.

**`onCreateList` eliminado como parámetro**: el parámetro de 2.2 era un placeholder. Ahora el FAB llama directamente a `showCreateDialog = true` dentro del Composable.

---

## Build

```
BUILD SUCCESSFUL in 28s — sin warnings
```
