# Guía de Android para BuyNote

Una guía práctica para entender cómo funciona este proyecto. Pensada para alguien que conoce programación pero es nuevo en Android.

---

## 1. La gran diferencia: no hay backend separado

En una app web tenés **frontend** (browser) y **backend** (servidor). En una app Android nativa como esta, **todo corre en el dispositivo**:

```
App web:          Browser  ←→  Servidor  ←→  Base de datos
App Android:      [UI + lógica + base de datos] todo en el celular
```

No hay servidor. Los datos viven en la base de datos local del dispositivo (Room/SQLite). La arquitectura MVVM divide responsabilidades **dentro** de la app, pero es todo una sola pieza.

Si en el futuro agregamos sincronización en la nube (Firebase), *ahí* aparecería un backend remoto — pero la app puede funcionar 100% offline sin él. Por eso diseñamos el Repository Pattern desde el principio.

---

## 2. Estructura de archivos del proyecto

```
BuyNote/
├── app/                          ← el módulo principal (hay proyectos con varios módulos)
│   ├── build.gradle.kts          ← dependencias y config de build del módulo
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   ← "registro" de la app: actividades, permisos, etc.
│       │   ├── java/com/martinjm/buynote/
│       │   │   ├── BuyNoteApplication.kt    ← arranque de la app
│       │   │   ├── MainActivity.kt          ← pantalla raíz
│       │   │   ├── data/                    ← capa de datos
│       │   │   │   └── db/AppDatabase.kt
│       │   │   ├── di/                      ← inyección de dependencias
│       │   │   │   └── DatabaseModule.kt
│       │   │   ├── domain/                  ← interfaces de repositorio (se llena en Fase 1)
│       │   │   └── ui/
│       │   │       ├── navigation/AppNavigation.kt
│       │   │       ├── screens/             ← una carpeta por pantalla
│       │   │       └── theme/               ← colores, tipografía, tema
│       │   └── res/                         ← recursos: imágenes, strings, etc.
│       ├── test/                 ← tests unitarios (corren en la PC)
│       └── androidTest/          ← tests instrumentados (corren en el dispositivo/emulador)
│
├── gradle/
│   ├── libs.versions.toml        ← catálogo central de versiones y dependencias
│   └── wrapper/gradle-wrapper.properties   ← versión de Gradle a usar
├── build.gradle.kts              ← config del proyecto raíz (plugins globales)
├── settings.gradle.kts           ← define qué módulos existen
└── gradle.properties             ← flags y configuración de Gradle
```

---

## 3. Gradle: el sistema de build

Gradle es el equivalente a `npm`/`Maven`/`pip` de Android — maneja dependencias y compila la app.

### `gradle/libs.versions.toml` — catálogo de versiones

Centralizamos todas las versiones y dependencias acá. En lugar de poner números por todos lados:

```toml
[versions]
hilt = "2.59.2"           # la versión se define una sola vez

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }

[plugins]
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

Y en `app/build.gradle.kts` lo referenciás como `libs.hilt.android` — sin repetir el número de versión.

### `app/build.gradle.kts` — el build del módulo

Tres secciones clave:

```kotlin
plugins {
    // qué herramientas se aplican al módulo
    alias(libs.plugins.android.application)  // es una app Android
    alias(libs.plugins.hilt.android)         // procesador de Hilt
    alias(libs.plugins.ksp)                  // procesador de código en tiempo de compilación
}

android {
    // config de la app: SDK mínimo, versión, etc.
    minSdk = 24    // Android 7.0, el mínimo que soportamos
    targetSdk = 36
}

dependencies {
    implementation(...)     // dependencias que van en la app final
    ksp(...)                // procesadores que solo se usan en compile-time (generan código)
    testImplementation(...) // solo para tests
}
```

### KSP (Kotlin Symbol Processing)

Es un procesador que corre **durante la compilación** y genera código automáticamente. Room y Hilt lo usan:
- Room ve `@Entity` y genera el SQL.
- Hilt ve `@HiltAndroidApp` y genera el grafo de dependencias.

No vas a ver ese código generado en tu proyecto — vive en `app/build/generated/ksp/`.

---

## 4. El punto de entrada: Application y Activity

### `BuyNoteApplication.kt`

```kotlin
@HiltAndroidApp
class BuyNoteApplication : Application()
```

Se ejecuta **antes que cualquier pantalla**. Android crea esta clase cuando lanza el proceso de la app. La anotación `@HiltAndroidApp` le dice a Hilt que arranque su sistema de inyección de dependencias acá.

### `MainActivity.kt`

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(...) {
        setContent {
            BuyNoteTheme {
                AppNavigation(navController)
            }
        }
    }
}
```

Es la pantalla raíz. `setContent { }` es el puente entre el mundo Android y Jetpack Compose — todo lo que ponés adentro es UI de Compose. Solo tenemos una Activity en este proyecto; la navegación entre "pantallas" la maneja Navigation Compose sin cambiar de Activity.

### `AndroidManifest.xml`

Es el "registro" que Android lee antes de instalar la app. Declara:
- Qué clase es la `Application` → `android:name=".BuyNoteApplication"`
- Qué Activities existen y cuál es el punto de entrada (la que tiene el `LAUNCHER` intent)
- Permisos que necesita la app (en Fase 4 agregaremos el de cámara acá)

---

## 5. Jetpack Compose: el sistema de UI

Compose reemplaza al antiguo sistema de XML layouts. Definís la UI con funciones de Kotlin anotadas con `@Composable`:

```kotlin
@Composable
fun ActiveListsScreen(navController: NavHostController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Listas activas") }) }
    ) { padding ->
        // contenido de la pantalla
    }
}
```

### Conceptos clave de Compose

| Concepto | Qué hace |
|---|---|
| `@Composable` | Marca una función como UI de Compose |
| `Scaffold` | Layout base con topBar, bottomBar, FAB |
| `LazyColumn` | Lista con scroll (equivalente a RecyclerView) |
| `State` / `StateFlow` | Cuando el estado cambia, la UI se redibuja automáticamente |
| `remember` | Guarda un valor durante la vida del Composable |
| `Modifier` | Encadena estilos y comportamiento (tamaño, padding, click, etc.) |

### Material 3

Es el sistema de diseño de Google. Nos provee componentes listos (`TopAppBar`, `Button`, `Card`, `TextField`) con los colores y formas del tema automáticamente aplicados.

El tema vive en `ui/theme/`:
- `Color.kt` — paleta de colores (fallback para Android < 12)
- `Theme.kt` — configura claro/oscuro + colores dinámicos (Android 12+)
- `Type.kt` — tipografías

---

## 6. MVVM: la arquitectura

```
UI (Composable) ← observa → ViewModel ← usa → Repository ← accede → Room/Network
```

| Capa | Archivo | Responsabilidad |
|---|---|---|
| **UI** | `ui/screens/XScreen.kt` | Mostrar datos, capturar eventos del usuario |
| **ViewModel** | `ui/screens/XViewModel.kt` | Lógica de presentación, expone StateFlow |
| **Repository** | `domain/repository/` (interface) + `data/repository/` (impl) | Abstrae de dónde vienen los datos |
| **Room** | `data/db/` | Base de datos local SQLite |

### ¿Por qué ViewModel?

Sobrevive a los cambios de configuración (rotar el teléfono destruye y recrea el Composable, pero no el ViewModel). Mantiene el estado de la pantalla.

### ¿Por qué Repository?

La UI no sabe si los datos vienen de Room o de Firebase. Cuando en el futuro agreguemos sync en la nube, solo cambiamos la implementación del Repository — la UI no se toca.

---

## 7. Hilt: inyección de dependencias

Hilt conecta los objetos automáticamente. En vez de hacer `val db = Room.databaseBuilder(...)` en cada lugar, declarás que lo necesitás y Hilt lo provee:

```kotlin
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val productRepository: ProductRepository  // Hilt inyecta esto
) : ViewModel()
```

### Cómo funciona

1. `@Module` define cómo crear los objetos (`DatabaseModule`)
2. `@Provides` le dice a Hilt cómo construir uno específico (ej: `AppDatabase`)
3. `@Inject` marca dónde Hilt debe inyectar el objeto
4. `@Singleton` garantiza que solo existe una instancia (ej: la DB)

---

## 8. Room: la base de datos

Room es un ORM (Object-Relational Mapper) sobre SQLite. Convertís clases Kotlin en tablas automáticamente.

```kotlin
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String?
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE name LIKE :query OR brand LIKE :query")
    fun search(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity)
}

@Database(entities = [ProductEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}
```

KSP lee estas anotaciones y genera el código SQL real durante la compilación.

### `Flow` vs `suspend`

- `suspend fun` → operación que espera un resultado (insert, delete)
- `Flow<T>` → stream de datos que se actualiza automáticamente (queries de lectura)

Cuando un nuevo producto se inserta, Room emite el nuevo resultado del `Flow` y la UI se actualiza sola.

---

## 9. Navigation Compose

Reemplaza al sistema de Fragments. Define un grafo de rutas y navegás entre ellas:

```kotlin
// Definición (AppNavigation.kt)
NavHost(startDestination = Routes.LISTS) {
    composable(Routes.LISTS) { ActiveListsScreen(navController) }
    composable(Routes.CATALOG) { CatalogScreen(navController) }
}

// Uso (desde cualquier Composable)
navController.navigate(Routes.CATALOG)
navController.popBackStack()  // equivalente al botón "back"
```

Las rutas con argumentos se definen así:

```kotlin
// En Routes
fun listDetail(listId: Long) = "list_detail/$listId"

// Navegación
navController.navigate(Routes.listDetail(lista.id))

// Recepción
val listId = backStackEntry.arguments?.getLong("listId")
```

---

## 10. Dependencias más importantes del proyecto

| Dependencia | Para qué sirve |
|---|---|
| **Jetpack Compose + Material 3** | Todo el sistema de UI |
| **Hilt** | Inyección de dependencias |
| **Room** | Base de datos local (SQLite) |
| **Navigation Compose** | Navegación entre pantallas |
| **Lifecycle ViewModel** | ViewModels con soporte a Compose |
| **CameraX** | Acceso a la cámara (Fase 4) |
| **ML Kit Barcode** | Leer códigos de barras (Fase 4) |
| **KSP** | Procesador de código (genera código para Room y Hilt) |

---

## 11. Cómo correr la app

### Opción A: Emulador (recomendada para desarrollo)
1. En Android Studio: `Tools → Device Manager → Create Device`
2. Elegí un Pixel con API 35+
3. Presioná el triángulo verde ▶ (Run)

### Opción B: Dispositivo físico
1. En el celular: `Ajustes → Acerca del teléfono → Número de compilación` (tocarlo 7 veces activa las opciones de desarrollador)
2. `Ajustes → Opciones de desarrollador → Depuración USB` → activar
3. Conectar por USB → aparece en Android Studio → presionar ▶

### Qué pasa cuando presionás Run
1. Gradle compila el código Kotlin → bytecode
2. KSP genera código para Room y Hilt
3. Se empaqueta todo en un `.apk`
4. El `.apk` se instala en el dispositivo/emulador
5. Se lanza `MainActivity`
6. Logcat muestra los logs en tiempo real

---

## 12. El flujo de datos en BuyNote (cómo se conecta todo)

```
Usuario toca "Agregar producto"
         ↓
Composable llama a viewModel.saveProduct(data)
         ↓
ViewModel llama a productRepository.insert(product)
         ↓
ProductRepositoryImpl llama a productDao.insert(entity)
         ↓
Room convierte la entity a SQL y ejecuta INSERT
         ↓
Room emite el nuevo Flow con los datos actualizados
         ↓
ViewModel expone StateFlow con la lista actualizada
         ↓
Composable se redibuja automáticamente con los nuevos datos
```

---

## 13. Shortcuts útiles en Android Studio

| Acción | Shortcut |
|---|---|
| Buscar archivo | `Ctrl + Shift + N` |
| Buscar símbolo (clase, función) | `Ctrl + Shift + Alt + N` |
| Buscar en todo el proyecto | `Ctrl + Shift + F` |
| Ver preview de Compose | `@Preview` + panel "Design" |
| Correr la app | `Shift + F10` |
| Ver Logcat (logs) | `Alt + 6` |
| Sincronizar Gradle | `Ctrl + Shift + O` |
| Ir a la definición | `Ctrl + B` o `Ctrl + Click` |
