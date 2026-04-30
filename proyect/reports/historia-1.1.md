# Historia 1.1 — Entidades Product y Category + DAO + Repository

## Qué se implementó

La capa de datos completa del catálogo: entidades Room, acceso a datos (DAOs), lógica de dominio (interfaces + implementaciones de repositorio), inyección de dependencias y tests.

---

## Archivos creados / modificados

```
app/src/main/java/com/martinjm/buynote/
│
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt                  ← MODIFICADO: se agregó @Database con las dos entidades
│   │   ├── entity/
│   │   │   ├── CategoryEntity.kt           ← NUEVO
│   │   │   └── ProductEntity.kt            ← NUEVO: FK a Category + índice UNIQUE en barcode
│   │   └── dao/
│   │       ├── CategoryDao.kt              ← NUEVO
│   │       └── ProductDao.kt               ← NUEVO
│   └── repository/
│       ├── CategoryRepositoryImpl.kt       ← NUEVO
│       └── ProductRepositoryImpl.kt        ← NUEVO
│
├── domain/
│   ├── model/
│   │   ├── Category.kt                     ← NUEVO
│   │   └── Product.kt                      ← NUEVO
│   └── repository/
│       ├── CategoryRepository.kt           ← NUEVO
│       └── ProductRepository.kt            ← NUEVO
│
└── di/
    ├── DatabaseModule.kt                   ← MODIFICADO: ahora provee AppDatabase, CategoryDao, ProductDao
    └── RepositoryModule.kt                 ← NUEVO: bindea las implementaciones con las interfaces

app/src/androidTest/java/com/martinjm/buynote/
└── db/
    └── CatalogDaoTest.kt                   ← NUEVO: 9 tests de DAO con Room in-memory

gradle/libs.versions.toml                   ← MODIFICADO: se agregó room-testing y kotlinx-coroutines-test
app/build.gradle.kts                        ← MODIFICADO: se agregaron dependencias de test
```

---

## Conceptos clave

### Entities vs Modelos de dominio

Hay dos tipos de clases que representan los mismos datos:

| Clase | Paquete | Quién la usa |
|---|---|---|
| `CategoryEntity` | `data.db.entity` | Room — mapea directamente a una tabla SQL |
| `Category` | `domain.model` | El resto de la app (ViewModels, UI) |

**¿Por qué tener dos?** Separar concerns. El modelo de dominio no sabe nada de Room (`@Entity`, `@PrimaryKey`, etc.). Si en el futuro cambiamos la base de datos o el esquema, solo tocamos la capa `data`. La UI y la lógica no se enteran.

Los `*RepositoryImpl` convierten entre ambas representaciones usando funciones de extensión privadas (`toDomain()`, `toEntity()`).

### @Entity y el esquema de base de datos

```kotlin
@Entity(
    tableName = "products",
    foreignKeys = [ForeignKey(entity = CategoryEntity::class, ...)],
    indices = [Index("categoryId"), Index("barcode", unique = true)]
)
```

- `foreignKeys`: Room genera la restricción FK en el SQL. `onDelete = SET_NULL` significa que si borrás una categoría, los productos que la usaban quedan con `categoryId = null` en lugar de borrarse.
- `indices`: Room crea un índice de base de datos. Acelera búsquedas por esa columna.
- `unique = true` en barcode: la BD rechaza insertar dos productos con el mismo código. Si intentás hacerlo, Room lanza una `SQLiteConstraintException`.

### DAOs: `Flow` vs `suspend fun`

```kotlin
@Query("SELECT * FROM products")
fun getAll(): Flow<List<ProductEntity>>    // reactivo: emite cada vez que cambia la tabla

@Insert
suspend fun insert(product: ProductEntity): Long   // one-shot: espera a que termine
```

- `Flow<T>`: stream continuo de datos. La UI suscribe una sola vez y recibe actualizaciones automáticamente cada vez que hay un cambio en la tabla. Ideal para listas.
- `suspend fun`: función que suspende el hilo hasta terminar (como `async/await`). Para escrituras (INSERT, UPDATE, DELETE) que son operaciones puntuales.

### Repository Pattern: ¿por qué dos capas?

```
UI / ViewModel
      ↓  usa solo la interfaz
CategoryRepository  (domain — no sabe cómo están guardados los datos)
      ↑  implementada por
CategoryRepositoryImpl  (data — sabe que usa Room)
```

La UI habla con la *interfaz*. Mañana podemos agregar Firebase o una API y solo cambiamos el `Impl`; el ViewModel no se toca.

### Hilt: dos tipos de módulos

**`DatabaseModule`** (object con `@Provides`): para clases que no controlamos (Room, Retrofit, etc.). Le decimos a Hilt cómo construir esas clases.

```kotlin
@Provides @Singleton
fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
    Room.databaseBuilder(...).build()
```

**`RepositoryModule`** (abstract class con `@Binds`): para clases propias que tienen `@Inject constructor`. Solo le decimos a Hilt "cuando alguien pida `CategoryRepository`, dale una `CategoryRepositoryImpl`".

```kotlin
@Binds
abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository
```

`@Binds` es más eficiente que `@Provides` porque Hilt no necesita generar el cuerpo de la función.

### Tests instrumentados con Room in-memory

Los tests de DAO necesitan correr en un dispositivo (o emulador) porque Room usa el runtime de Android. Por eso están en `androidTest/` y llevan `@RunWith(AndroidJUnit4::class)`.

```kotlin
db = Room.inMemoryDatabaseBuilder(
    ApplicationProvider.getApplicationContext(),
    AppDatabase::class.java
).build()
```

`inMemoryDatabaseBuilder` crea una base de datos que vive solo en RAM. Es perfecta para tests: arranca limpia, no persiste nada entre tests y no necesita permisos de disco.

`runTest` (de `kotlinx-coroutines-test`) permite llamar `suspend fun` y colectar `Flow` dentro de un test sin bloquear el hilo:

```kotlin
@Test
fun insertCategory_andGetAll_returnsIt() = runTest {
    val id = categoryDao.insert(CategoryEntity(name = "Lácteos"))
    val all = categoryDao.getAll().first()   // .first() toma la primera emisión del Flow
    assertEquals(1, all.size)
}
```

---

## Decisiones tomadas

| Decisión | Alternativa descartada | Motivo |
|---|---|---|
| `OnConflictStrategy.ABORT` en insert | `REPLACE` | REPLACE borra y reinserta (pierde el id), queremos que el error burbuje al ViewModel para mostrarlo |
| `exportSchema = false` en `@Database` | `exportSchema = true` | No hay equipo que necesite el historial de schema por ahora; evita warning de Gradle |
| FK con `SET_NULL` (Category→Product) | `CASCADE` o sin FK | Borrar una categoría no debe borrar sus productos; quedan como "sin categoría" |
| Tests en `androidTest/` | Robolectric en `test/` | Evita agregar Robolectric como dependencia; Room in-memory funciona bien con instrumentados |
| Mappers privados en cada `*Impl` | Archivo `Mapper.kt` separado | Son detalles de implementación; no necesitan ser públicos fuera del impl |
| `@Update`/`@Delete` retornan `Int` (filas afectadas) | `Unit` | Room 2.6.1 no podía manejar el tipo `void` (`V`) con KSP 2.x; con `Int` el tipo es representable en el bytecode. Igual fue necesario actualizar a Room 2.7.0 (fix oficial) |
| Room `2.7.0` en lugar de `2.6.1` | Mantener 2.6.1 | Room 2.6.1 generaba código Java con `Continuation<Long>` (invariante), incompatible con lo que Kotlin 2.2 espera: `Continuation<? super Long>` (contravariante). Room 2.7.0 fue lanzado específicamente para resolver la compatibilidad con Kotlin 2.0+ |
