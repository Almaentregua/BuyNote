# Historia 1.4 — Eliminar producto

## Qué se implementó

Acción de eliminación de productos con diálogo de confirmación. Solo visible al editar un producto existente. Se agregó `deleteById` en toda la cadena de datos para evitar tener que reconstruir un objeto completo solo para borrar por ID.

---

## Archivos creados / modificados

```
app/src/main/java/com/martinjm/buynote/
│
├── data/db/dao/ProductDao.kt              ← MODIFICADO: se agregó deleteById con @Query
├── domain/repository/ProductRepository.kt ← MODIFICADO: se agregó deleteById a la interfaz
├── data/repository/ProductRepositoryImpl.kt ← MODIFICADO: se implementó deleteById
│
└── ui/screens/productform/
    ├── ProductFormViewModel.kt            ← MODIFICADO: se agregó fun delete()
    └── ProductFormScreen.kt               ← MODIFICADO: ícono eliminar en TopAppBar + AlertDialog
```

---

## Conceptos clave

### Por qué `deleteById` en lugar de `delete(product)`

El DAO ya tenía:
```kotlin
@Delete
suspend fun delete(product: ProductEntity): Int
```

`@Delete` usa el **primary key** del objeto para generar `DELETE FROM products WHERE id = ?`. El resto de los campos son ignorados por Room.

Podríamos haber pasado un `ProductEntity(id = productId, name = "")` y habría funcionado. Pero eso es engañoso: parece que el objeto completo importa cuando en realidad no. Además requiere tener el objeto en memoria.

La solución limpia es una `@Query` directa:
```kotlin
@Query("DELETE FROM products WHERE id = :id")
suspend fun deleteById(id: Long): Int
```

Esto es explícito, no necesita ningún objeto, y el DAO expresa claramente su contrato.

### Estado local del Composable: el dialog

El estado del dialog de confirmación (`showDeleteDialog`) **no va al ViewModel**. ¿Por qué?

- No afecta a la lógica de negocio
- No necesita sobrevivir a una rotación de pantalla (si el usuario rota mientras el dialog está abierto, que se cierre está bien)
- Es puramente presentacional: "¿está visible este dialog en este momento?"

```kotlin
var showDeleteDialog by remember { mutableStateOf(false) }
```

`remember { mutableStateOf(...) }` crea un estado que:
1. **Se recuerda** entre recomposiciones del mismo Composable (no se reinicia con cada redibujado)
2. **No sobrevive** a la destrucción del Composable (rotación de pantalla, navegación)

Contraste con el ViewModel: el ViewModel sobrevive a rotaciones porque Android lo preserva. `remember` no.

**Regla práctica**: si el estado solo importa para *mostrar u ocultar* algo en la pantalla, puede vivir en el Composable con `remember`. Si afecta datos, lógica o necesita sobrevivir a rotaciones, va al ViewModel.

### AlertDialog: anatomía

```kotlin
AlertDialog(
    onDismissRequest = onDismiss,          // se llama cuando el usuario toca fuera o presiona back
    title = { Text("¿Eliminar producto?") },
    text = { Text("...") },               // cuerpo del dialog
    confirmButton = {
        TextButton(onClick = onConfirm) {
            Text("Eliminar", color = MaterialTheme.colorScheme.error)  // rojo = destructivo
        }
    },
    dismissButton = {
        TextButton(onClick = onDismiss) { Text("Cancelar") }
    }
)
```

Convenciones de Material 3 para dialogs de confirmación destructiva:
- El botón de cancelar va a la **izquierda** (dismiss)
- El botón de confirmar va a la **derecha** (confirm)
- Las acciones destructivas se colorean con `MaterialTheme.colorScheme.error` (rojo del tema)
- `onDismissRequest` maneja el cierre por interacción externa (toque fuera, botón back del sistema)

### Ícono de eliminar en la TopAppBar

El ícono de papelera solo aparece cuando `isEditing == true`:
```kotlin
if (viewModel.isEditing) {
    IconButton(onClick = { showDeleteDialog = true }) {
        Icon(
            Icons.Default.Delete,
            tint = MaterialTheme.colorScheme.error
        )
    }
}
```

El click **no llama directamente a `viewModel.delete()`** — primero muestra el dialog. El ViewModel solo se llama cuando el usuario confirma. Esto es intencional: las acciones destructivas requieren una confirmación explícita.

El `tint = MaterialTheme.colorScheme.error` en el ícono señala visualmente la naturaleza destructiva de la acción, siguiendo las guías de Material 3.

### La política de FK: `ON DELETE SET NULL`

La `ForeignKey` entre `ShoppingListItem.productId → Product.id` (que se creará en la historia 2.1) está diseñada con `onDelete = ForeignKey.SET_NULL`.

Esto significa: si borrás el producto "Leche La Serenísima" del catálogo, los ítems de listas que lo referenciaban no se borran — se convierten en ítems de texto libre con `productId = null`. El texto que el usuario ve en la lista seguirá siendo "Leche La Serenísima" (almacenado en `customName`).

El dialog informa al usuario de esta consecuencia: *"Si el producto está en alguna lista de compras, los ítems correspondientes quedarán como texto libre."*

Por ahora (Phase 1), no hay ShoppingListItems, así que el borrado solo afecta al catálogo. Pero la infraestructura de la BD ya está preparada para manejar esto correctamente cuando llegue la Fase 2.

### `check()`: precondición en Kotlin

```kotlin
fun delete() {
    check(isEditing)   // lanza IllegalStateException si isEditing == false
    ...
}
```

`check(condicion)` es la forma idiomática en Kotlin de verificar precondiciones en el estado interno del objeto. Si se viola, lanza `IllegalStateException` con un mensaje descriptivo.

Es diferente de `require()` (que verifica argumentos de entrada) y de `assert()` (que solo corre en debug). `check()` es para invariantes del estado interno: "si llegamos aquí con `isEditing == false`, algo en la lógica del llamador está mal".

En este caso, el botón de eliminar solo aparece en la UI cuando `isEditing == true`, así que `check(isEditing)` nunca debería fallar en producción — es una red de seguridad para bugs futuros.

---

## Decisiones tomadas

| Decisión | Alternativa | Motivo |
|---|---|---|
| Ícono de eliminar en TopAppBar con color error | Botón al pie del formulario | Accesible sin scroll, pero visualmente diferenciado del "Guardar" por color y forma (ícono vs texto) |
| Dialog como estado local (`remember`) | Estado en el ViewModel | El dialog es puramente presentacional; no necesita sobrevivir a rotaciones |
| `deleteById` con `@Query` | Reuse de `@Delete` con objeto parcial | Semánticamente más claro; el contrato del DAO es explícito |
| `check(isEditing)` en `delete()` | Sin verificación | Red de seguridad frente a bugs futuros; el costo es cero en producción |
| Texto del dialog menciona la política FK | Solo decir "no se puede deshacer" | El usuario debe entender la consecuencia real para tomar una decisión informada |
