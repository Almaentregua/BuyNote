# Backlog — BuyNote

Cada historia es una unidad de trabajo independiente, pequeña y verificable, con criterios de aceptación claros. La idea es ir tomándolas de a una en cada iteración, terminarla bien (incluyendo prueba manual + tests donde aplique) y recién después pasar a la siguiente.

---

## Estado general

| Fase | Nombre | Estado |
|------|--------|--------|
| 0 | Setup del proyecto | ✓ Completa |
| 1 | Catálogo de productos | ✓ Completa |
| 2 | Listas de compras | ✓ Completa |
| 3 | Modo compras | ✓ Completa |
| 4 | Lector de código de barras | ✓ Completa |
| 5 | Gestión avanzada | Pendiente |

---

## Fase 0 — Setup del proyecto ✓

### 0.1 Configuración base de Gradle y dependencias
**Objetivo**: dejar el proyecto compilando con todas las dependencias necesarias.
**Criterios de aceptación**:
- `build.gradle` con Kotlin, Compose BOM, Material 3, Hilt, Room, Navigation Compose, CameraX, ML Kit barcode.
- Versiones unificadas en `libs.versions.toml`.
- Compila en limpio (`./gradlew assembleDebug`).

### 0.2 Estructura de paquetes y configuración de Hilt
**Objetivo**: layout de paquetes acorde a MVVM + Repository, con Hilt funcionando.
**Criterios de aceptación**:
- Paquetes: `data` (entities, dao, db, repository.impl), `domain` (repository interfaces, model), `ui` (screens, viewmodels), `di`.
- `Application` anotada con `@HiltAndroidApp`.
- Un módulo de prueba con un binding inyectable correctamente.

### 0.3 Setup de Room (base de datos vacía)
**Objetivo**: clase `AppDatabase` configurada, lista para recibir entidades.
**Criterios de aceptación**:
- `AppDatabase` con `@Database` (sin entidades aún o con un placeholder).
- Provista por Hilt como singleton.
- Estrategia de migraciones planteada (aunque vacía por ahora).

### 0.4 Setup de Navigation Compose con pantallas placeholder
**Objetivo**: poder navegar entre las pantallas principales aunque estén vacías.
**Criterios de aceptación**:
- `NavHost` con rutas: `lists` (home), `list_detail/{id}`, `catalog`, `product_form?productId={id?}`, `categories`, `history`, `scanner`.
- Cada pantalla muestra su título y un botón de back (donde aplique).

### 0.5 Tema Material 3
**Objetivo**: tema claro/oscuro consistente.
**Criterios de aceptación**:
- `Theme.kt`, `Color.kt`, `Type.kt` configurados.
- Soporte automático light/dark según el sistema.

---

## Fase 1 — Catálogo de productos ✓

### 1.1 Entidades Product y Category + DAO + Repository
**Objetivo**: capa de datos del catálogo lista para usar.
**Criterios de aceptación**:
- Entities `ProductEntity`, `CategoryEntity` con relación FK (`categoryId`).
- DAOs con CRUD básico + búsqueda por nombre/marca.
- `ProductRepository` y `CategoryRepository` (interfaces en `domain`, impls en `data`) inyectables vía Hilt.
- Constraint UNIQUE en `barcode`.
- Test unitario del DAO con Room in-memory.

### 1.2 Pantalla "Catálogo" — listado read-only
**Objetivo**: ver todos los productos guardados.
**Criterios de aceptación**:
- LazyColumn con cada producto (nombre, marca, categoría).
- Estado vacío con mensaje y CTA "Agregar producto".
- ViewModel expone StateFlow.

### 1.3 Pantalla "Crear / editar producto"
**Objetivo**: alta y edición de productos.
**Criterios de aceptación**:
- Formulario con: nombre*, marca, código de barras, categoría (dropdown), notas.
- Validación: nombre obligatorio; barcode único si se carga.
- Manejo del error de constraint UNIQUE con mensaje claro.
- Guardar y volver al catálogo.

### 1.4 Eliminar producto
**Objetivo**: poder borrar un producto del catálogo.
**Criterios de aceptación**:
- Acción "eliminar" con diálogo de confirmación.
- Definición y aplicación de la política sobre items existentes (FK con `ON DELETE SET NULL`: el item queda como ad-hoc con el `customName` heredado del producto).

### 1.5 Búsqueda en el catálogo
**Objetivo**: filtrar productos por nombre o marca.
**Criterios de aceptación**:
- Campo de búsqueda en el top de la pantalla.
- Filtra en tiempo real (debounce 250ms).
- Búsqueda case-insensitive sobre nombre y marca.

### 1.6 Gestión de categorías (CRUD)
**Objetivo**: administrar categorías propias.
**Criterios de aceptación**:
- Pantalla con lista de categorías.
- Crear, editar, eliminar (con confirmación si la categoría tiene productos asociados).
- Acceso desde el catálogo (botón en la top bar o menú).

---

## Fase 2 — Listas de compras ✓

### 2.1 Entidades ShoppingList y ShoppingListItem + DAO + Repository
**Objetivo**: capa de datos de las listas lista.
**Criterios de aceptación**:
- Entities con FKs correspondientes.
- Enums `ListStatus` y `QuantityUnit` mapeados con `TypeConverter`.
- DAO con CRUD + queries por estado (activas / completadas).
- `ShoppingListRepository` inyectable.

### 2.2 Pantalla "Listas activas" como home
**Objetivo**: el home muestra listas en estado ACTIVE.
**Criterios de aceptación**:
- LazyColumn con nombre, fecha de creación, contador "X items / Y listos".
- FAB para crear nueva lista.
- Acceso a "Historial", "Catálogo" y "Categorías" via menú/drawer.

### 2.3 Crear nueva lista
**Objetivo**: crear lista vacía con un nombre.
**Criterios de aceptación**:
- Diálogo simple con campo "nombre".
- Validación: nombre obligatorio.
- Al crear, navega al detalle de la lista recién creada.

### 2.4 Pantalla "Detalle de lista" (vacía)
**Objetivo**: ver el contenido de una lista.
**Criterios de aceptación**:
- Top bar con nombre de la lista + contador de progreso.
- Lista de items vacía con CTA.
- Dos opciones para sumar item: "desde catálogo" y "ad-hoc".

### 2.5 Agregar item desde catálogo
**Objetivo**: elegir un producto del catálogo y sumarlo a la lista.
**Criterios de aceptación**:
- Picker con búsqueda (reutiliza componente de la 1.5).
- Al elegir, pide cantidad y unidad antes de confirmar.
- El item aparece en el detalle de la lista.

### 2.6 Agregar item ad-hoc
**Objetivo**: agregar un item con texto libre.
**Criterios de aceptación**:
- Form con: nombre*, cantidad, unidad.
- Al guardar, se ofrece checkbox "Guardar también al catálogo".
- Si se marca, abre el form de producto con el nombre precargado.

### 2.7 Editar cantidad / unidad de un item
**Objetivo**: ajustar un item ya agregado.
**Criterios de aceptación**:
- Tap en el item abre edición inline o bottom sheet.
- Solo se editan cantidad, unidad y `customName` (si era ad-hoc).

### 2.8 Eliminar item de la lista
**Objetivo**: sacar un item de la lista.
**Criterios de aceptación**:
- Swipe-to-delete con snackbar de "deshacer" (5s).

---

## Fase 3 — Modo compras ✓

### 3.1 Tildar / destildar items
**Objetivo**: marcar items como agarrados en el local.
**Criterios de aceptación**:
- Checkbox en cada item.
- Estado persiste en BD.

### 3.2 Separación visual y contador de progreso
**Objetivo**: ver fácilmente lo que falta vs lo listo.
**Criterios de aceptación**:
- Items pendientes arriba, marcados abajo (con sección visualmente diferenciada — opacidad / divider).
- Contador "5 de 12 listos" en la top bar + barra de progreso opcional.

### 3.3 Agrupar items por categoría
**Objetivo**: ordenar por sectores del local.
**Criterios de aceptación**:
- Toggle entre "orden de carga" y "agrupado por categoría".
- Items ad-hoc o sin categoría van al final.

### 3.4 Finalizar compra
**Objetivo**: marcar la lista como completada.
**Criterios de aceptación**:
- Botón "Finalizar compra" en el detalle.
- Diálogo de confirmación.
- Setea `status = COMPLETED`, `completedAt = now`.
- La lista deja de aparecer en el home y aparece en historial.

### 3.5 Historial
**Objetivo**: ver listas pasadas en modo lectura.
**Criterios de aceptación**:
- Pantalla con listas completadas, ordenadas por `completedAt` desc.
- Tap muestra detalle read-only (sin edición ni borrado).
- Mostrar fecha de creación y de completado.

---

## Fase 4 — Lector de código de barras ✓

### 4.1 Permisos de cámara
**Objetivo**: solicitar permiso al usuario.
**Criterios de aceptación**:
- Manejo de permission rationale.
- Manejo de "denegado para siempre" con CTA a settings del sistema.

### 4.2 Pantalla scanner con CameraX + ML Kit
**Objetivo**: detectar un código de barras con la cámara.
**Criterios de aceptación**:
- Preview de cámara fullscreen.
- Detección de barcodes EAN-13, EAN-8, UPC-A.
- Vibración + cierre del scanner al detectar.

### 4.3 Búsqueda en catálogo por código → agregar a lista
**Objetivo**: si el código ya existe, sumar el producto a la lista activa.
**Criterios de aceptación**:
- Lookup en `ProductRepository.findByBarcode(...)`.
- Si existe: pide cantidad/unidad y agrega.
- Toast / snackbar de confirmación.

### 4.4 Si no existe → form de producto con código precargado
**Objetivo**: alta rápida desde el scanner.
**Criterios de aceptación**:
- Navega a `product_form` con barcode prellenado.
- Al guardar, vuelve al detalle de la lista y suma el producto recién creado.

---

## Fase 5 — Gestión avanzada

### 5.1 Eliminar lista activa
**Objetivo**: poder borrar una lista que todavía no fue completada.
**Criterios de aceptación**:
- Swipe-to-delete en el home de listas activas con diálogo de confirmación.
- Botón "Eliminar lista" en el detalle de la lista con diálogo de confirmación.
- Al confirmar, elimina la lista y todos sus items en cascada.
- Si se elimina desde el detalle, navega al home.

### 5.2 Eliminar listas del historial
**Objetivo**: poder limpiar listas completadas que ya no son necesarias.
**Criterios de aceptación**:
- Swipe-to-delete sobre una lista en el historial con diálogo de confirmación.
- Botón "Limpiar historial" en la top bar del historial con diálogo de confirmación ("Esta acción no se puede deshacer").
- Ambas acciones eliminan la lista y todos sus items en cascada.

### 5.3 Escanear código desde el formulario de producto
**Objetivo**: rellenar el campo de código de barras escaneando, sin tener que tipear.
**Criterios de aceptación**:
- Ícono de scanner al lado del campo "Código de barras" en el formulario de producto.
- Al tocar, abre el scanner (sin listId).
- Al detectar un código, cierra el scanner y llena el campo automáticamente.
- Si el código ya existe en otro producto, el error de constraint se muestra al intentar guardar (comportamiento ya existente).

---

## Bugs conocidos

### ~~BUG-01~~ — Ítem de lista pierde el nombre al eliminar el producto del catálogo ✓ Resuelto

**Síntoma**: al eliminar un producto del catálogo, los ítems de listas que lo referenciaban aparecen como un checkbox vacío sin texto (ni en listas activas ni en el historial).

**Causa**: `ProductFormViewModel.delete()` llama a `productRepository.deleteById(productId)` sin antes copiar `product.name` a `customName` de los ítems vinculados. Room aplica `ON DELETE SET NULL` sobre `productId` automáticamente, pero `customName` también queda `null`. El display name resuelve `product?.name ?: item.customName ?: ""` y termina en cadena vacía.

**Comportamiento esperado** (definido en historia 1.4): al eliminar un producto, los ítems vinculados deben quedar con `productId = null` y `customName = product.name`, conservando el nombre como texto libre.

**Fix propuesto**: antes de ejecutar el delete, obtener todos los `ShoppingListItem` que referencian ese `productId` y setear `customName = product.name` en los que tengan `customName = null`. Requiere inyectar `ShoppingListRepository` en `ProductFormViewModel` o encapsular la lógica en `ProductRepositoryImpl`.

---

## Ideas / Backlog futuro

Ideas que no están listas para convertirse en historias todavía. Se priorizan y detallan antes de arrancar.

- **Scanner desde el catálogo**: botón en la pantalla de catálogo que escanea un código y abre el producto si ya existe, o el form de alta si no. Similar al flujo de listas pero sin el contexto de una lista activa.

---

## Roadmap de iteraciones sugerido

| Iteración | Contenido |
|-----------|-----------|
| 1 | Fase 0 completa |
| 2 | 1.1 → 1.4 (catálogo CRUD básico) |
| 3 | 1.5 + 1.6 (búsqueda y categorías) |
| 4 | 2.1 → 2.4 (modelos de lista + home + crear + detalle vacío) |
| 5 | 2.5 → 2.8 (agregar / editar / eliminar items) |
| 6 | Fase 3 completa (modo compras + historial) |
| 7 | Fase 4 completa (barcode scanner) |
| 8 | Fase 5 completa (gestión avanzada) |

Al final de cada iteración la app debería compilar, correr y ofrecer valor incremental que se pueda probar.
