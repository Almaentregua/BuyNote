# Historia 1.6 — Gestión de categorías (CRUD)

## Qué se implementó

CRUD completo de categorías: crear, editar, y eliminar con confirmación que varía según si la categoría tiene productos asociados. El formulario es un `AlertDialog` inline con un solo campo (nombre), dado que las categorías no tienen más atributos. Acceso desde el catálogo via ícono en la TopAppBar.

---

## Archivos creados / modificados

```
app/src/main/java/com/martinjm/buynote/
│
└── ui/screens/categories/
    ├── CategoriesViewModel.kt  ← CREADO: ViewModel con CRUD + estados de dialog
    └── CategoriesScreen.kt     ← REEMPLAZADO: implementación completa sobre el placeholder

ui/screens/catalog/
    └── CatalogScreen.kt        ← MODIFICADO: ícono Label en TopAppBar → navega a categorías
```

---

## Conceptos clave

### `MutableStateFlow` con `collect` en `init` vs `stateIn`

En historias anteriores (CatalogViewModel) se usó `stateIn` para convertir un Flow del repositorio en StateFlow:

```kotlin
// Patrón anterior (CatalogViewModel)
val uiState = productRepository.getAll()
    .map { ... }
    .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = ...)
```

En CategoriesViewModel se usa un enfoque diferente: un `MutableStateFlow` que se actualiza desde `init`:

```kotlin
// Patrón nuevo (CategoriesViewModel)
private val _uiState = MutableStateFlow(CategoriesUiState())
val uiState: StateFlow<CategoriesUiState> = _uiState

init {
    viewModelScope.launch {
        categoryRepository.getAll().collect { categories ->
            _uiState.update { it.copy(categories = categories, isLoading = false) }
        }
    }
}
```

**¿Por qué este patrón en vez de `stateIn`?**

El UiState tiene dos fuentes de datos con naturalezas distintas:
- `categories`: reactiva (viene de Room, Flow del repositorio)
- `categoryDialog` / `deletionRequest`: local (controlada por el usuario, no viene de ningún Flow)

Con `stateIn` solo puedo convertir *un* Flow en StateFlow. Para mezclar datos reactivos con estado local, necesitaría hacer `combine` de varios Flows — incluyendo un Flow artificial para el estado del dialog. Eso agrega complejidad innecesaria.

La alternativa más simple: un `MutableStateFlow` que contiene *todo* el estado, y que recibe actualizaciones de ambas fuentes: del `collect` del repositorio y de las funciones de UI (`openCreateDialog`, `dismissDialog`, etc.).

**La diferencia importante con `stateIn(SharingStarted.WhileSubscribed(5_000))`**:

Con `stateIn` y `WhileSubscribed`, la suscripción al repositorio Room se detiene 5 segundos después de que ningún collector esté activo (por ejemplo, cuando el usuario sale de la pantalla). Con el patrón de `collect` en `init`, la colección corre durante *toda la vida del ViewModel*, incluso cuando la pantalla no está visible.

Para este caso el overhead es mínimo (Room no hace I/O si los datos no cambian), pero es una diferencia arquitectónica a tener en cuenta. Si el ViewModel fuera más costoso, habría que preferir `stateIn`.

### Dialogs dentro del UiState

El estado de los dialogs (crear/editar y confirmación de eliminación) vive en el UiState del ViewModel, no en el Composable con `remember`:

```kotlin
data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val categoryDialog: CategoryDialog? = null,    // null = cerrado, non-null = abierto
    val deletionRequest: DeletionRequest? = null   // null = no hay confirmación pendiente
)
```

Contraste con la historia 1.4 (dialog de eliminación de producto), donde el dialog era estado local del Composable:

```kotlin
// Historia 1.4: estado local
var showDeleteDialog by remember { mutableStateOf(false) }
```

**¿Por qué el cambio?**

En la historia 1.4, el dialog era puramente presentacional: solo "¿está visible o no?". El ViewModel no necesitaba saber nada sobre el dialog para ejecutar el borrado.

En la historia 1.6, el dialog tiene **datos propios**:
- `CategoryDialog` tiene el nombre que el usuario está tipando y un posible error de validación
- `DeletionRequest` tiene la categoría a eliminar y si tiene productos (calculado asincrónicamente)

Si `nameError` viviera en el Composable con `remember`, la validación tendría que estar en la UI. Si `hasProducts` viviera en el Composable, la coroutine de consulta de productos también tendría que estar en la UI. Eso viola la separación de responsabilidades: la lógica de negocio pertenece al ViewModel.

**Regla más precisa**:
- Estado puramente visual que no involucra lógica → `remember` en el Composable
- Estado que tiene datos propios, validación, o requiere operaciones asíncronas → en el ViewModel

### Null como señal de "cerrado"

```kotlin
val categoryDialog: CategoryDialog? = null
```

Usar un tipo nullable como indicador de visibilidad es un patrón idiomático en Compose + ViewModel. En vez de tener `showDialog: Boolean` + `dialogData: CategoryDialog`, se combina en `dialog: CategoryDialog?`:

- `null` → el dialog no está visible
- `CategoryDialog(...)` → el dialog está visible con esos datos

En la pantalla esto se expresa limpiamente:

```kotlin
uiState.categoryDialog?.let { dialog ->
    CategoryFormDialog(dialog = dialog, ...)
}
```

Si `categoryDialog` es null, el bloque `let` no se ejecuta y el dialog no se compone. No hay `if (showDialog)` separado.

### Consulta única (`first()`) para verificar productos antes de eliminar

Cuando el usuario toca el ícono de eliminar en una categoría, el ViewModel necesita saber si esa categoría tiene productos asociados para mostrar el mensaje correcto en el dialog.

```kotlin
fun requestDelete(category: Category) {
    viewModelScope.launch {
        val products = productRepository.getAll().first()
        val hasProducts = products.any { it.categoryId == category.id }
        _uiState.update { it.copy(deletionRequest = DeletionRequest(category, hasProducts)) }
    }
}
```

`productRepository.getAll()` retorna `Flow<List<Product>>` — un stream reactivo. Pero acá no necesito el stream completo: solo quiero el valor actual, una vez. `first()` es el operador que consume el primer elemento emitido por el Flow y cancela la suscripción.

Alternativas consideradas:
- **Agregar `countByCategory(id)` al DAO**: requeriría cambios en el DAO, repositorio e interfaz — demasiado aparato para una consulta ocasional
- **Mantener los productos en el UiState de categorías**: sobredimensionado; ese estado pertenece a CatalogViewModel
- **`first()`**: directo, sin schema changes, correcto para una verificación puntual

### El dialog de confirmación cambia según el contexto

```kotlin
data class DeletionRequest(
    val category: Category,
    val hasProducts: Boolean
)
```

```kotlin
// En la pantalla:
DeleteCategoryDialog(
    request = request,
    onConfirm = viewModel::confirmDelete,
    onDismiss = viewModel::cancelDelete
)

// En el composable del dialog:
Text(
    if (request.hasProducts)
        "Esta categoría tiene productos asociados. Al eliminarla, esos productos quedarán sin categoría."
    else
        "Esta acción no se puede deshacer."
)
```

Si la categoría tiene productos, el mensaje informa de la consecuencia concreta (los productos quedan sin categoría, gracias al `ON DELETE SET NULL` de la FK). Si no tiene productos, el mensaje es simple. El usuario toma una decisión informada en ambos casos.

### Formulario en `AlertDialog` (vs pantalla dedicada)

Los formularios de crear/editar podrían haberse resuelto con una pantalla de navegación (`Routes.CATEGORY_FORM`). Se eligió un `AlertDialog` inline porque:

1. Categoría tiene un solo campo (nombre). Una pantalla completa sería excesiva.
2. El flujo es más rápido: el usuario toca "Editar", modifica el nombre en el mismo dialog, guarda. Sin navegación.
3. El stack de navegación no crece innecesariamente.

La regla de oro: si el formulario tiene 3+ campos o lógica compleja (dropdowns, validación cruzada), pantalla dedicada. Si es un dato simple, dialog.

### `AutoMirrored` para íconos direccionales

```kotlin
// Correcto:
Icons.AutoMirrored.Outlined.Label
// Deprecated:
Icons.Outlined.Label
```

Material Icons marca algunos íconos como "AutoMirrored" — íconos que deberían reflejarse horizontalmente en locales de derecha a izquierda (árabe, hebreo). El ícono `Label` (etiqueta) tiene dirección, así que Material lo incluye en esta categoría.

Usar la versión `AutoMirrored` no cambia nada visualmente en español, pero es la API correcta y evita los warnings del compilador.

---

## Decisiones tomadas

| Decisión | Alternativa | Motivo |
|---|---|---|
| `MutableStateFlow` + `collect` en `init` | `stateIn(WhileSubscribed)` | El UiState mezcla datos reactivos (categorías) con estado local (dialogs); un solo MutableStateFlow es más simple que un `combine` con Flows artificiales |
| Dialog/DeletionRequest en el ViewModel | `remember` en el Composable | Los dialogs tienen datos propios y lógica asíncrona (`first()` para contar productos) — deben vivir en el ViewModel |
| `nullable` como indicador de visibilidad | `showDialog: Boolean` + objeto separado | Más idiomático, evita estado inconsistente (dialog visible pero sin datos) |
| `productRepository.getAll().first()` para verificar productos | DAO `countByCategory` | Sin cambios de schema; la consulta es ocasional (no reactiva); `first()` es suficiente |
| `AlertDialog` para crear/editar | Pantalla de navegación dedicada | Un solo campo no justifica una pantalla; el dialog es más fluido |
| Mensaje de confirmación diferente según `hasProducts` | Un solo mensaje genérico | El usuario necesita entender la consecuencia real (FK SET_NULL) para decidir con información |
| Ícono Label en TopAppBar del catálogo | Menú overflow o FAB secundario | El acceso a categorías es frecuente (para organizarse); un ícono dedicado es más directo que un menú |
