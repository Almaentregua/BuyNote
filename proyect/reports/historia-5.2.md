# Reporte — Historia 5.2: Eliminar listas del historial

## Cambios implementados

### `ShoppingListDao`
- Nuevo query: `@Query("DELETE FROM shopping_lists WHERE status = 'COMPLETED'") suspend fun deleteAllCompleted()`

### `ShoppingListRepository` (interfaz)
- Nuevo método: `suspend fun deleteAllCompleted()`

### `ShoppingListRepositoryImpl`
- Implementación de `deleteAllCompleted()` delegando a `listDao.deleteAllCompleted()`.

### `HistoryViewModel`
- Nuevo método `deleteList(id: Long)`: lanza coroutine con `repository.deleteById(id)`.
- Nuevo método `deleteAll()`: lanza coroutine con `repository.deleteAllCompleted()`.

### `HistoryScreen`
- `HistoryList` recibe nuevo parámetro `onListDelete: (Long) -> Unit`.
- Nuevo composable `SwipeableHistoryItem`: envuelve `ListItem` con `SwipeToDismissBox` (solo EndToStart). Al dispararse el swipe muestra `AlertDialog` de confirmación. En cancelar llama `dismissState.reset()`. En confirmar llama `onConfirmDelete()`.
- Botón `DeleteSweep` en el top bar, visible solo cuando hay listas en el historial. Abre `AlertDialog` de confirmación con advertencia "Esta acción no se puede deshacer".
- El cascade delete de items es automático por FK `onDelete = ForeignKey.CASCADE` en `ShoppingListItemEntity`.

## Verificado

- `assembleDebug` exitoso en 32s sin errores.
