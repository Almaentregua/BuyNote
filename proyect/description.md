# Shopping List App — Contexto del Proyecto

## Descripción general

Aplicación mobile Android desarrollada en Kotlin con Jetpack Compose. El objetivo es gestionar listas de compras: el usuario va cargando artículos durante la semana a medida que se da cuenta que necesita algo, y luego usa la lista como checklist al ir al supermercado o a cualquier local.

## Arquitectura

**MVVM + Repository Pattern**

- **Capa UI**: Jetpack Compose + ViewModel + StateFlow
- **Capa de dominio**: interfaces de Repository (abstrae las fuentes de datos)
- **Capa de datos**: Room (local, fuente única de verdad por ahora)
- **DI**: Hilt

El Repository Pattern es intencional: la app arranca completamente offline, pero la arquitectura debe soportar agregar una fuente de datos remota (Firebase u otro) en el futuro sin tocar la capa UI.

## Stack tecnológico

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Base de datos**: Room (SQLite)
- **DI**: Hilt
- **Navegación**: Navigation Compose
- **Cámara / Código de barras**: CameraX + ML Kit

## Modelo de datos

```kotlin
Category(
    id: Long,
    name: String
)

Product(
    id: Long,
    name: String,
    brand: String?,
    barcode: String?,        // único cuando existe (UNIQUE)
    categoryId: Long?,       // FK → Category
    notes: String?
)

ShoppingList(
    id: Long,
    name: String,
    status: ListStatus,      // ACTIVE | COMPLETED
    createdAt: Long,
    completedAt: Long?       // se setea al finalizar la compra
)

ShoppingListItem(
    id: Long,
    listId: Long,            // FK → ShoppingList
    productId: Long?,        // FK → Product (nullable: ítems ad-hoc)
    customName: String?,     // se usa cuando productId es null
    quantity: Double,        // soporta enteros y decimales (1, 1.5)
    unit: QuantityUnit,      // UNIT | KG | G | L | ML
    isChecked: Boolean
)

enum ListStatus { ACTIVE, COMPLETED }
enum QuantityUnit { UNIT, KG, G, L, ML }   // ampliable
```

**Notas sobre el modelo**:
- `productId` es nullable por diseño: los ítems pueden referenciar el catálogo o ser entradas libres (texto).
- `barcode` tiene constraint UNIQUE: se asume el código de barras de fábrica del producto.
- Las cantidades son flexibles: combinación de número decimal + unidad (3 unidades, 1.5 kg, 500 g, etc.).
- Las categorías son administrables por el usuario (no son una lista fija).

## Features principales (por prioridad)

### 1. Catálogo de productos
El usuario mantiene un catálogo personal de productos que suele comprar (nombre, marca, categoría, código de barras, notas). Búsqueda por nombre y marca (ej. "yerba playadito"). Las categorías son administrables y permiten agrupar productos en el modo compras.

### 2. Listas de compras
Pueden existir múltiples listas activas simultáneamente (ej: "Supermercado", "Ferretería"). Cada lista tiene un nombre, estado (ACTIVE / COMPLETED) y contiene ítems con cantidad y unidad.

### 3. Modo compras (checklist)
Mientras el usuario está en el local, va tildando los ítems a medida que los agarra. Los ítems marcados y pendientes se separan visualmente. La lista muestra el progreso (ej: "5 de 12 listos"). Cuando termina, marca la lista como **completada** y queda en el historial.

### 4. Historial
Las listas completadas quedan accesibles en modo lectura para revisar compras pasadas (qué se compró, cuándo). Una vez completada, una lista no vuelve a estado activo: si se necesita, se crea una nueva.

### 5. Lector de código de barras
Mediante la cámara del dispositivo. Al escanear, busca en el catálogo local: si lo encuentra, agrega el producto directamente a la lista activa; si no, abre el formulario de creación de producto con el código ya precargado.

## Decisiones de diseño tomadas

- Los ítems se pueden agregar a una lista de forma manual (texto libre) o desde el catálogo. Al agregar uno ad-hoc, la app **ofrece** guardarlo al catálogo para que el usuario decida.
- Las listas tienen ciclo de vida: **ACTIVE → COMPLETED**. No hay reset; al finalizar una compra se crea una nueva lista.
- Pantalla de inicio: vista de **listas activas**. Historial y catálogo se acceden por navegación lateral / tabs.
- Las categorías son administrables por el usuario (CRUD).
- El borrado de productos / listas activas es permanente (no hay borrado lógico).
- En v1 no hay cuentas de usuario — todos los datos son locales al dispositivo.
- La sincronización en la nube es una feature futura; la capa Repository no debe bloquear esto.

## Estado del proyecto

Greenfield. No hay nada implementado todavía.
