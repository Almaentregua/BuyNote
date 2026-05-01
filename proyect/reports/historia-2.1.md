# Historia 2.1 — Entidades ShoppingList y ShoppingListItem + DAO + Repository

**Estado**: Completada  
**Fecha**: 2026-05-01  
**Commit**: pendiente

---

## Criterios de aceptación — verificación

| Criterio | Estado |
|---|---|
| Entities con FKs correspondientes | ✅ |
| Enums `ListStatus` y `QuantityUnit` mapeados con `TypeConverter` | ✅ |
| DAO con CRUD + queries por estado (activas / completadas) | ✅ |
| `ShoppingListRepository` inyectable vía Hilt | ✅ |

---

## Archivos creados

### Dominio
- `domain/model/ListStatus.kt` — enum `ACTIVE | COMPLETED`
- `domain/model/QuantityUnit.kt` — enum `UNIT | KG | G | L | ML`
- `domain/model/ShoppingList.kt` — data class de dominio
- `domain/model/ShoppingListItem.kt` — data class de dominio
- `domain/repository/ShoppingListRepository.kt` — interfaz con operaciones sobre listas e ítems

### Capa de datos
- `data/db/converter/Converters.kt` — `TypeConverter` para `ListStatus` y `QuantityUnit` (almacenados como String)
- `data/db/entity/ShoppingListEntity.kt` — tabla `shopping_lists`
- `data/db/entity/ShoppingListItemEntity.kt` — tabla `shopping_list_items` con FK CASCADE a listas y SET_NULL a productos
- `data/db/dao/ShoppingListDao.kt` — CRUD + `getActive()` / `getCompleted()` como Flow
- `data/db/dao/ShoppingListItemDao.kt` — CRUD + `getByListId()` como Flow
- `data/repository/ShoppingListRepositoryImpl.kt` — implementación con mapeo entity ↔ domain

### DI
- `di/DatabaseModule.kt` — proveedores de `ShoppingListDao` y `ShoppingListItemDao`; migración 1→2 registrada
- `di/RepositoryModule.kt` — binding de `ShoppingListRepository`

### Tests
- `androidTest/.../db/ShoppingListDaoTest.kt` — 8 tests

### Modificados
- `data/db/AppDatabase.kt` — versión 1→2, `@TypeConverters(Converters::class)`, nuevas entidades, nuevos DAOs, `MIGRATION_1_2`

---

## Decisiones de diseño

**Un único repositorio para lista e ítems**: `ShoppingListRepository` expone operaciones tanto sobre `ShoppingList` como sobre `ShoppingListItem`. Los ítems no tienen sentido fuera del contexto de una lista, por lo que un único repositorio simplifica el grafo de dependencias.

**FK `listId` con `ON DELETE CASCADE`**: borrar una lista elimina automáticamente todos sus ítems. Comportamiento esperado y correcto.

**FK `productId` con `ON DELETE SET NULL`**: borrar un producto del catálogo no elimina el ítem de la lista; el ítem queda como ad-hoc con el `customName` que tenía. Consistente con la política definida en la historia 1.4.

**Migración explícita 1→2**: se agrega `MIGRATION_1_2` en lugar de `fallbackToDestructiveMigration` para no perder datos de usuarios que ya tengan la versión 1 instalada.

**Enums almacenados como String**: más legible en SQLite que como enteros; el `TypeConverter` maneja la conversión transparentemente.

---

## Tests implementados

| Test | Qué verifica |
|---|---|
| `insertList_andGetActive_returnsIt` | Insert básico y query por estado ACTIVE |
| `completedList_notInActive_appearsInCompleted` | Cambio de estado ACTIVE → COMPLETED |
| `getById_returnsCorrectList` | Lookup por ID |
| `getById_unknownId_returnsNull` | ID inexistente devuelve null |
| `deleteById_removesFromActive` | Eliminación de lista |
| `insertItem_andGetByListId_returnsIt` | Insert de ítem y recuperación por listId |
| `updateItem_changesFields` | Edición de cantidad, unidad e isChecked |
| `deleteList_cascadesToItems` | Cascade: borrar lista elimina sus ítems |
| `deleteProduct_setsNullOnItemProductId` | SET_NULL: borrar producto no rompe el ítem |
| `multipleUnits_storedAndRestoredCorrectly` | Round-trip de todos los valores de QuantityUnit |

---

## Build

```
BUILD SUCCESSFUL in ~1m
```
Warning menor: nombre del parámetro `db` en la lambda de migración (cosmético, no afecta funcionalidad).
