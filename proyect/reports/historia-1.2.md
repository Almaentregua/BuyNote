# Historia 1.2 — Pantalla "Catálogo" (listado read-only)

## Qué se implementó

La pantalla de catálogo con su ViewModel. Muestra todos los productos del repositorio en una lista, con estado vacío y botón de acción para agregar.

---

## Archivos creados / modificados

```
app/src/main/java/com/martinjm/buynote/ui/screens/catalog/
├── CatalogViewModel.kt    ← NUEVO: ViewModel + modelos de UI state
└── CatalogScreen.kt       ← MODIFICADO: reemplaza el placeholder con la UI real
```

---

## Conceptos clave

### StateFlow y el ciclo de vida de la UI

El ViewModel expone un `StateFlow<CatalogUiState>`. La UI se suscribe y se redibuja cada vez que el estado cambia.

```
Room emite Flow<List<Product>>
         ↓
ViewModel combina y transforma → StateFlow<CatalogUiState>
         ↓
Composable observa con collectAsStateWithLifecycle()
         ↓
Compose redibuja solo las partes que cambiaron
```

### `combine`: mezclar dos Flows en uno

El catálogo necesita mostrar el *nombre* de la categoría, pero `Product` solo tiene `categoryId`. Hay que combinar dos fuentes:

```kotlin
combine(
    productRepository.getAll(),   // Flow<List<Product>>
    categoryRepository.getAll()   // Flow<List<Category>>
) { products, categories ->
    // este bloque corre cada vez que cualquiera de los dos flows emite
    val categoryById = categories.associateBy { it.id }
    CatalogUiState(
        products = products.map { p ->
            ProductUiModel(
                categoryName = p.categoryId?.let { categoryById[it]?.name }
            )
        }
    )
}
```

`combine` emite un nuevo valor cada vez que *cualquiera* de sus fuentes emite. Room hace que eso sea automático: si el usuario agrega una categoría nueva, el catálogo se actualiza solo.

### `stateIn`: convertir un Flow frío a un StateFlow caliente

Un `Flow` normal es **frío** — solo corre cuando alguien lo está colectando. Eso es problemático para la UI: si la pantalla rota, el Composable se recrea y el Flow vuelve a empezar desde cero.

`stateIn` lo convierte en un **StateFlow caliente** que:
1. Guarda el último valor emitido (`value`)
2. Puede seguir corriendo aunque nadie lo esté mirando (por un tiempo)

```kotlin
.stateIn(
    scope = viewModelScope,              // vive mientras el ViewModel vive
    started = SharingStarted.WhileSubscribed(5_000),  // sigue activo 5s sin suscriptores
    initialValue = CatalogUiState()      // valor mientras llegan los primeros datos
)
```

**¿Por qué 5 segundos?** Es el tiempo que tarda una rotación de pantalla. El usuario rota → el Composable muere → el Flow sigue activo 5s → el Composable renace y recibe el último valor *sin tener que volver a hacer la query*.

### `collectAsStateWithLifecycle`: suscripción lifecycle-aware

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

En lugar de `collectAsState()` (que siempre corre), esta versión pausa la colección cuando la app va al background. Ahorra batería y evita updates innecesarios.

### UI State: modelo separado de dominio

El ViewModel **no expone** los modelos de dominio directamente a la UI. En cambio, crea modelos específicos para la pantalla:

| Modelo | Dónde vive | Para qué |
|---|---|---|
| `Product` | `domain.model` | Lógica de negocio, repositorios |
| `ProductUiModel` | `ui.screens.catalog` | Solo lo que la pantalla necesita ver |

`ProductUiModel` tiene `categoryName: String?` en lugar de `categoryId: Long?`. La pantalla no necesita saber cómo se llama la tabla en la BD — solo necesita el texto a mostrar.

### Manejo de estados en la UI: `when` sobre el estado

La pantalla tiene tres estados posibles:

```kotlin
when {
    uiState.isLoading -> CircularProgressIndicator(...)
    uiState.products.isEmpty() -> EmptyCatalog(...)
    else -> ProductList(...)
}
```

`isLoading = true` en el estado inicial evita que se muestre brevemente el estado vacío mientras Room hace la primera query (que tarda milisegundos pero alcanza para un flash visual).

### Composables privados como componentes

La pantalla se divide en funciones `private`:

```kotlin
@Composable private fun EmptyCatalog(...)
@Composable private fun ProductList(...)
@Composable private fun ProductItem(...)
```

Son `private` porque solo existen para esta pantalla. Esto mejora la legibilidad de `CatalogScreen` (que queda en ~20 líneas de estructura) y hace cada pieza testeable en aislamiento.

### `ListItem`: componente de Material 3 para listas

`ListItem` implementa las guías de Material 3 para ítems de lista:

```kotlin
ListItem(
    headlineContent = { Text(product.name) },         // texto principal
    supportingContent = product.brand?.let { { Text(it) } },   // texto secundario (opcional)
    trailingContent = product.categoryName?.let { ... }        // elemento a la derecha (opcional)
)
```

Los lambdas opcionales (`?.let { { ... } }`) son la forma de Kotlin de decir "si el valor no es null, mostrar este Composable; si no, no mostrar nada".

### `hiltViewModel()`: inyectar el ViewModel en un Composable

```kotlin
fun CatalogScreen(
    navController: NavHostController,
    viewModel: CatalogViewModel = hiltViewModel()   // Hilt lo crea y lo provee
)
```

`hiltViewModel()` es la función de `hilt-navigation-compose` que conecta Hilt con el sistema de ViewModels de Compose. Automáticamente usa el scope correcto (la entry de navegación) y reutiliza el ViewModel si la pantalla se recrea.

---

## Decisiones tomadas

| Decisión | Alternativa descartada | Motivo |
|---|---|---|
| `combine` con dos repositorios | Query JOIN en el DAO | Mantiene la capa de datos simple; la combinación es responsabilidad del ViewModel |
| `isLoading = true` en el estado inicial | Mostrar vacío desde el inicio | Evita el flash del estado vacío mientras llega el primer dato de Room |
| Modelos `ProductUiModel` separados | Exponer `Product` de dominio | La UI no debería saber cómo está estructurado el dominio; el modelo de UI tiene exactamente los campos que necesita |
| Items no clickeables en 1.2 | Agregar onClick preparando 1.3 | La historia es read-only; el click se agrega en 1.3 donde hay pantalla destino |
| `ExtendedFloatingActionButton` | Button dentro del contenido | FAB siempre visible sin importar si la lista está vacía; evitar duplicar el CTA |
| Categoría como `Surface` pill, no `Chip` | `FilterChip` / `SuggestionChip` | Los chips de Material 3 tienen estado de click implícito; un `Surface` decorativo es más semánticamente correcto para info no interactiva |
