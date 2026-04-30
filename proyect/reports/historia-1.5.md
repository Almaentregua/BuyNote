# Historia 1.5 — Búsqueda en el catálogo

## Qué se implementó

Filtrado en tiempo real del catálogo por nombre o marca, con debounce de 250ms para evitar queries innecesarias mientras el usuario escribe. La infraestructura SQL ya existía desde la historia 1.1.

---

## Archivos modificados

```
app/src/main/java/com/martinjm/buynote/ui/screens/catalog/
├── CatalogViewModel.kt    ← MODIFICADO: debounce + flatMapLatest + searchQuery expuesto
└── CatalogScreen.kt       ← MODIFICADO: SearchField + estado "sin resultados"
```

---

## Conceptos clave

### `flatMapLatest`: cancelar la búsqueda anterior cuando llega una nueva

```kotlin
_searchQuery
    .debounce { query -> if (query.isEmpty()) 0L else 250L }
    .distinctUntilChanged()
    .flatMapLatest { query -> productRepository.search(query) }
```

`flatMapLatest` es el operador clave para búsqueda en tiempo real. Cada vez que el upstream emite un nuevo valor (la query), **cancela la coroutine del resultado anterior** y lanza una nueva con la query actualizada.

Sin `flatMapLatest`, si el usuario escribe rápido, podrían llegar resultados de queries anteriores y "pisar" al resultado correcto. Con `flatMapLatest`, solo el último resultado importa.

```
query: "l" → flatMapLatest lanza search("l")
query: "le" → flatMapLatest cancela search("l"), lanza search("le")
query: "lec" → flatMapLatest cancela search("le"), lanza search("lec")
                                             ↑
                             search("l") y search("le") nunca llegan a la UI
```

### Debounce con selector de tiempo por valor

```kotlin
.debounce { query -> if (query.isEmpty()) 0L else 250L }
```

La versión de `debounce` que recibe una función tiene una ventaja importante: permite un timeout diferente por valor.

- Query vacía → `0L` ms de espera → la lista completa aparece **inmediatamente** al entrar a la pantalla o al limpiar la búsqueda
- Query no vacía → `250L` ms → espera a que el usuario deje de tipear antes de hacer la query

Sin esto, el estado de carga inicial (`isLoading = true`) duraría 250ms innecesariamente, generando un flash de spinner al abrir la pantalla.

### `distinctUntilChanged`: no repetir la misma búsqueda

```kotlin
.distinctUntilChanged()
```

Después del `debounce`, si el usuario escribe "abc", borra una letra y la vuelve a poner, y espera 250ms, la query resultante sería "abc" dos veces seguidas. `distinctUntilChanged` descarta la segunda emisión si es igual a la anterior, evitando una query duplicada.

### Dos StateFlows: texto del campo vs resultados filtrados

```kotlin
// ViewModel
private val _searchQuery = MutableStateFlow("")
val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()   // inmediato

val uiState: StateFlow<CatalogUiState> = _searchQuery
    .debounce(...)
    .flatMapLatest { productRepository.search(it) }
    ...stateIn(...)                                               // con delay
```

```kotlin
// Screen
val query by viewModel.searchQuery.collectAsStateWithLifecycle()  // actualiza el TextField
val uiState by viewModel.uiState.collectAsStateWithLifecycle()    // actualiza la lista
```

Se usan **dos StateFlows distintos** porque tienen propósitos diferentes:

| `searchQuery` | `uiState.products` |
|---|---|
| Controla el texto que ve el usuario en el campo | Controla la lista de resultados |
| Se actualiza **inmediatamente** con cada tecla | Se actualiza **250ms después** de que el usuario para de tipear |
| El usuario ve su texto reflejado al instante | La lista no "parpadea" en cada letra |

Si usáramos un solo StateFlow con debounce, el texto del campo también se atrasaría 250ms — el usuario escribiría y el campo no respondería. Mala UX.

### Tres estados vacíos, no uno

```kotlin
when {
    uiState.isLoading -> CircularProgressIndicator
    uiState.products.isEmpty() && query.isEmpty() -> EmptyCatalog()    // catálogo vacío
    uiState.products.isEmpty()                    -> EmptySearch(query) // sin resultados
    else                                          -> ProductList(...)
}
```

La misma lista vacía puede significar cosas distintas según el contexto:
- Sin query → el catálogo no tiene productos → mostrar CTA de agregar
- Con query → la búsqueda no encontró nada → mostrar mensaje con la query y sugerencia de intentar otro término

Compartir el mismo estado vacío daría un mensaje confuso: "Tu catálogo está vacío" cuando en realidad hay productos pero ninguno coincide con la búsqueda.

### `SearchField`: pill shape con botón de limpiar

```kotlin
OutlinedTextField(
    shape = RoundedCornerShape(50),  // pill / cápsula
    colors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    ),
    trailingIcon = if (query.isNotEmpty()) {
        { IconButton(onClick = { onQueryChange("") }) { Icon(Clear) } }
    } else null
)
```

- `RoundedCornerShape(50)` con 50% da la forma de píldora, distintiva de un campo de búsqueda vs un campo de formulario.
- El borde con `alpha = 0.4f` hace que en estado inactivo sea más sutil, dando jerarquía visual correcta (el campo de búsqueda no compite con el contenido de la lista).
- El botón de limpiar (✕) solo aparece cuando hay texto — evitar mostrar un ícono no funcional.

### Capa de datos: sin cambios

El DAO ya tenía `search(query)` desde la historia 1.1. La UI se conecta a algo que ya existía. Este es el beneficio de diseñar bien las capas desde el principio: la feature de búsqueda en la UI no requirió tocar ningún archivo del paquete `data/` ni `domain/`.

---

## Decisiones tomadas

| Decisión | Alternativa | Motivo |
|---|---|---|
| `debounce { query -> if (isEmpty) 0L else 250L }` | `debounce(250)` fijo | El tiempo cero para query vacía evita el delay en la carga inicial y al limpiar la búsqueda |
| Dos StateFlows separados (query + uiState) | Query dentro de `CatalogUiState` | El campo de texto necesita actualizarse instantáneamente; los resultados pueden esperar el debounce |
| `distinctUntilChanged()` después del debounce | Sin él | Evita queries duplicadas si el usuario "rodea" el mismo término |
| `EmptySearch` como estado separado de `EmptyCatalog` | Un solo estado vacío | El mensaje y la sugerencia son distintos según si el catálogo está vacío o la búsqueda no tiene resultados |
| `OutlinedTextField` pill para búsqueda | Material 3 `SearchBar` / `DockedSearchBar` | Más simple, consistente con el resto de la app, y sin APIs experimentales adicionales |
