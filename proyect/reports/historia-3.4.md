# Historia 3.4 — Finalizar compra

**Estado**: Completada  
**Fecha**: 2026-05-01

---

## Criterios de aceptación — verificación

| Criterio | Estado |
|---|---|
| Botón "Finalizar compra" en el detalle | ✅ |
| Diálogo de confirmación | ✅ |
| Setea `status = COMPLETED`, `completedAt = now` | ✅ |
| La lista deja de aparecer en el home y aparece en historial | ✅ (el home filtra por ACTIVE; historial pendiente en 3.5) |

---

## Archivos modificados

- `ui/screens/listdetail/ListDetailViewModel.kt` — imports `ListStatus`, `MutableSharedFlow`, `SharedFlow`; `navigateBack: SharedFlow<Unit>`; `completeList()`: obtiene la lista, la actualiza con `COMPLETED` + `System.currentTimeMillis()`, emite navegación
- `ui/screens/listdetail/ListDetailScreen.kt` — `LaunchedEffect` colecta `navigateBack` → `popBackStack()`; `showCompleteDialog` state; `TextButton("Finalizar")` en TopAppBar actions (solo cuando `totalItems > 0`); `AlertDialog` de confirmación

---

## Decisiones de diseño

**Botón en TopAppBar, no como FAB/botón de fondo**: el FAB ya está ocupado con "Agregar". Un `TextButton` en las actions es el patrón establecido en esta app (igual que "Guardar" en el form de producto). Se muestra solo cuando `totalItems > 0` — no tiene sentido finalizar una lista vacía.

**`navigateBack` SharedFlow** (mismo patrón que `ProductFormViewModel`): la navegación es un evento de UI de un solo disparo, no estado persistente.

**`popBackStack()` en vez de navegar explícitamente al home**: si el usuario llegó al detalle desde el home, popBackStack lo regresa al home. Si en el futuro el historial también navega al detalle, la misma lógica aplica.

---

## Build

```
BUILD SUCCESSFUL in 20s — sin warnings
```
