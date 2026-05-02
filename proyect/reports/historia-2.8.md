# Historia 2.8 — Eliminar item de la lista

**Estado**: Completada  
**Fecha**: 2026-05-01

---

## Criterios de aceptación — verificación

| Criterio | Estado |
|---|---|
| Swipe-to-delete (derecha → izquierda) con fondo rojo + ícono de borrar | ✅ |
| Snackbar de "Deshacer" (~4s) | ✅ |
| "Deshacer" re-inserta el item en la lista | ✅ |

---

## Archivos modificados

- `ui/screens/listdetail/ListDetailViewModel.kt` — `deleteItem(id)`: guarda el item en `lastDeletedItem` y lo borra del repositorio; `undoDeleteItem()`: re-inserta `lastDeletedItem` con `id = 0L`
- `ui/screens/listdetail/ListDetailScreen.kt` — `SnackbarHost` en el Scaffold; `onItemDelete` lambda que dispara delete + snackbar + undo; `ItemsList` envuelve cada fila en `SwipeToDismissBox`

---

## Decisiones de diseño

**`LaunchedEffect(dismissState.currentValue)` en vez de `confirmValueChange`**: `confirmValueChange` está deprecado en la versión actual de Material3. La alternativa recomendada es observar `currentValue` con un `LaunchedEffect` y disparar la acción cuando el estado se asienta en `EndToStart`. Como el item desaparece de la lista (Room emite la actualización), el efecto no se vuelve a disparar.

**Solo swipe de derecha a izquierda** (`enableDismissFromStartToEnd = false`): el gesto de izquierda a derecha no tiene semántica clara en este contexto; el borrado es la única acción del swipe.

**`lastDeletedItem` en el ViewModel**: campo simple `private var`, no StateFlow. Solo se necesita internamente para el undo; no tiene sentido exponerlo a la UI.

**Re-insertar con `id = 0L`**: preserva todos los campos del item original pero deja que Room genere un nuevo id. El item se agrega al final de la lista si se deshace, lo cual es el comportamiento estándar de operaciones de undo en apps de lista.

**Duración del snackbar `Short` (~4s)**: Material3 no tiene duración personalizada; `Short` es la opción más cercana al criterio de 5s.

---

## Build

```
BUILD SUCCESSFUL in 15s — sin warnings
```
