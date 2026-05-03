# Reporte — Historia 5.1: Eliminar lista activa

## Cambios implementados

### `ActiveListsViewModel`
- Nuevo método `deleteList(id: Long)`: llama a `repository.deleteById(id)`. Room aplica `CASCADE` y elimina todos los items en cascada automáticamente (FK definida en `ShoppingListItemEntity`).

### `ActiveListsScreen`
- Nuevos imports: `SwipeToDismissBox`, `SwipeToDismissBoxValue`, `rememberSwipeToDismissBoxState`, `background`, `Delete`, `rememberCoroutineScope`, `kotlinx.coroutines.launch`.
- `ShoppingListColumn` recibe nuevo parámetro `onListDelete: (Long) -> Unit`.
- Nuevo composable `SwipeableShoppingListCard`: envuelve `ShoppingListCard` con `SwipeToDismissBox` (solo EndToStart). Al dispararse el swipe, muestra un `AlertDialog` de confirmación. En cancelar llama `dismissState.reset()` para que la card vuelva a su lugar. En confirmar llama `onConfirmDelete()`.
- El dialog muestra el nombre de la lista y advierte que la acción no se puede deshacer.

### `ListDetailViewModel`
- Nuevo método `deleteList()`: llama a `repository.deleteById(listId)` y emite en `_navigateBack` para que la pantalla vuelva al home.

### `ListDetailScreen`
- Nueva variable de estado `showDeleteListDialog`.
- Nuevo `IconButton` con `Icons.Default.Delete` en el top bar, visible solo cuando `!uiState.isCompleted`.
- Nuevo `AlertDialog` de confirmación que muestra el nombre de la lista.

## Verificado

- `assembleDebug` exitoso en 38s sin errores.
- Cascade delete manejado por Room (FK `onDelete = ForeignKey.CASCADE` en `ShoppingListItemEntity.listId`).
