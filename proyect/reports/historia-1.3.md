# Historia 1.3 — Pantalla "Crear / editar producto"

## Qué se implementó

Formulario para alta y edición de productos con validación de campos y manejo del error de barcode duplicado. Los ítems del catálogo ahora son clickeables y navegan al formulario de edición.

---

## Archivos creados / modificados

```
app/src/main/java/com/martinjm/buynote/ui/screens/productform/
├── ProductFormViewModel.kt    ← NUEVO
└── ProductFormScreen.kt       ← MODIFICADO: reemplaza placeholder con formulario real

app/src/main/java/com/martinjm/buynote/ui/screens/catalog/
└── CatalogScreen.kt           ← MODIFICADO: ítems clickeables, navega a edición
```

---

## Conceptos clave

### SavedStateHandle: leer argumentos de navegación en el ViewModel

El ViewModel no tiene acceso directo al `NavController`. Para leer el argumento `productId` de la ruta, usa `SavedStateHandle`, que Hilt inyecta automáticamente:

```kotlin
@HiltViewModel
class ProductFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val productId: Long = savedStateHandle["productId"] ?: -1L
}
```

El nombre de la clave (`"productId"`) debe coincidir con el nombre del argumento definido en la ruta de `AppNavigation`:
```kotlin
arguments = listOf(navArgument("productId") { type = NavType.LongType; defaultValue = -1L })
```

`-1L` como valor centinela indica "nuevo producto" (IDs reales siempre son > 0 porque `autoGenerate = true` empieza desde 1).

### MutableStateFlow manual vs `combine`

En la historia 1.2 usamos `combine` para mezclar dos Flows reactivos que el usuario no toca.
En este formulario, el estado cambia por **input del usuario**, no por la base de datos. Por eso usamos un `MutableStateFlow` que el ViewModel modifica directamente:

```kotlin
private val _uiState = MutableStateFlow(ProductFormUiState())
val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = null) }
```

`.update { }` garantiza thread-safety: en lugar de leer y escribir por separado, aplica una transformación atómica. `it.copy(...)` crea una nueva instancia inmutable del estado.

Las categorías sí son reactivas (si el usuario agrega una mientras el formulario está abierto, el dropdown se actualiza), así que sí se colectan desde el repositorio dentro del ViewModel.

### SharedFlow para eventos de navegación (one-shot)

El ViewModel no puede acceder al `NavController`. La solución estándar: emitir un evento que la UI escucha una sola vez.

```kotlin
// ViewModel
private val _navigateBack = MutableSharedFlow<Unit>()
val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

// Después de guardar exitosamente:
_navigateBack.emit(Unit)
```

```kotlin
// Composable
LaunchedEffect(Unit) {
    viewModel.navigateBack.collect { navController.popBackStack() }
}
```

**¿Por qué `SharedFlow` y no `StateFlow`?** `StateFlow` siempre tiene un valor actual. Si usáramos `isSaved: Boolean = true`, el Composable reaccionaría cada vez que se redibuje, no solo una vez. `SharedFlow` sin `replay` no guarda el evento — si nadie está escuchando cuando se emite, el evento se pierde (que es lo que queremos: no queremos navegar dos veces).

**¿Por qué `LaunchedEffect(Unit)`?** `Unit` como key significa "corre una sola vez al aparecer el Composable". El `collect` dentro es una coroutine que vive hasta que el Composable desaparece, escuchando eventos.

### Validación en el ViewModel, no en la UI

La validación del nombre vacío se hace en el ViewModel, no en el Composable:

```kotlin
fun save() {
    if (state.name.isBlank()) {
        _uiState.update { it.copy(nameError = "El nombre es obligatorio") }
        return    // aborta el guardado
    }
    // continúa...
}
```

El error se muestra en el TextField con `isError` y `supportingText`:
```kotlin
OutlinedTextField(
    isError = uiState.nameError != null,
    supportingText = uiState.nameError?.let { { Text(it) } }
)
```

El `?.let { { Text(it) } }` anidado: si `nameError` es null, `supportingText` recibe null (no se muestra nada); si no es null, recibe un lambda `@Composable` que muestra el error.

El error se borra automáticamente cuando el usuario empieza a tipear (`nameError = null` en `onNameChange`).

### Manejo de SQLiteConstraintException

Cuando el barcode viola el índice UNIQUE, Room lanza una `SQLiteConstraintException` durante el insert/update. Se captura y se convierte en un mensaje de error de UI:

```kotlin
try {
    productRepository.insert(product)
    _navigateBack.emit(Unit)
} catch (e: SQLiteConstraintException) {
    _uiState.update { it.copy(barcodeError = "Este código ya existe en el catálogo") }
}
```

Este patrón sigue el flujo: el error técnico de la base de datos se transforma en un mensaje comprensible para el usuario, y la lógica de UI (qué mostrar) vive en el ViewModel.

### ExposedDropdownMenuBox: dropdown de Material 3

El componente oficial para dropdowns en Material 3:

```kotlin
ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
    OutlinedTextField(
        readOnly = true,
        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(...)
    }
}
```

- `menuAnchor(MenuAnchorType.PrimaryNotEditable)`: le dice al `ExposedDropdownMenuBox` que este TextField es el "anchor" (punto de anclaje) del menú. `PrimaryNotEditable` porque es read-only (el usuario no escribe directamente, solo selecciona).
- `readOnly = true`: el TextField actúa como display, no como input de texto.
- El estado `expanded` es local al Composable (`remember { mutableStateOf(false) }`). No necesita estar en el ViewModel porque es solo comportamiento visual.

### KeyboardOptions: UX de teclado en formularios

Cada campo tiene configurado el comportamiento del teclado:

```kotlin
KeyboardOptions(
    capitalization = KeyboardCapitalization.Sentences,  // primera letra mayúscula
    imeAction = ImeAction.Next                          // el botón del teclado va al siguiente campo
)
```

El campo de notas usa `ImeAction.Done` (cierra el teclado) porque es el último campo. El de código de barras usa `KeyboardType.Number` para mostrar el teclado numérico.

### Separación de responsabilidades en la UI

El Composable se divide en funciones privadas por responsabilidad:

| Función | Qué hace |
|---|---|
| `ProductFormScreen` | Ciclo de vida: observa estado, maneja navegación, estructura el Scaffold |
| `ProductForm` | Layout del formulario con todos los campos |
| `CategoryDropdown` | Componente aislado con su propio estado (`expanded`) |

`ProductForm` recibe lambdas para cada cambio de campo, no el ViewModel directamente. Esto es una técnica llamada **"hoisting del estado"** — el Composable no sabe quién le provee los datos, solo los muestra y reporta cambios hacia arriba.

---

## Decisiones tomadas

| Decisión | Alternativa | Motivo |
|---|---|---|
| Botón "Guardar" en la TopAppBar | FAB o botón al pie del formulario | Consistente con el patrón "cancelar/confirmar" de Material 3 para formularios en pantalla completa |
| `SharedFlow` para navegateBack | `isSaved: Boolean` en el estado | Evitar el problema de "doble navegación" si el Composable se redibuja mientras `isSaved` es true |
| Error de barcode como estado en `uiState` | Dialog de error | El error se muestra inline bajo el campo afectado, que es más claro para el usuario |
| `isEditing` como `val` calculado en el ViewModel | Pasar como parámetro a la Screen | El ViewModel ya conoce el `productId`, centraliza la lógica de "estoy editando o creando" |
| Limpiar `nameError`/`barcodeError` al tipear | Mostrar error hasta el próximo save | Feedback inmediato: el usuario ve que el error se resolvió a medida que escribe |
| Click en ítems del catálogo agregado en 1.3 | Esperar a que sea "oficialmente" parte del backlog | La historia 1.3 crea la pantalla destino, así que hacer los ítems clickeables en el mismo momento es lo natural |
