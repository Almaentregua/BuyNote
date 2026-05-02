# Historia 3.2 — Separación visual y contador de progreso

**Estado**: Completada  
**Fecha**: 2026-05-01

---

## Criterios de aceptación — verificación

| Criterio | Estado |
|---|---|
| Items pendientes arriba, marcados abajo con sección diferenciada | ✅ |
| Contador "X de Y listos" en la top bar | ✅ (ya existía desde 2.4) |
| Barra de progreso | ✅ |

---

## Archivos modificados

- `ui/screens/listdetail/ListDetailScreen.kt` — body reemplazado de `Box` a `Column` con `LinearProgressIndicator` en la parte superior; `ItemsList` particionado en pendientes/listos con header "Listos" entre secciones; `ItemRow` extraído como composable privado; alpha 0.5f en items tildados

---

## Decisiones de diseño

**`remember(items) { items.partition { !it.isChecked } }`**: la partición se hace en el composable, no en el ViewModel, porque es un concern de presentación. El ViewModel no necesita saber cómo se agrupa visualmente.

**Header "Listos" solo cuando hay items en ambas secciones**: si todos los items están tildados (o ninguno), no aparece el separador. La condición `pending.isNotEmpty() && checked.isNotEmpty()` lo maneja.

**Alpha solo en el `ListItem`, no en el `SwipeToDismissBox`**: el fondo rojo del swipe queda a opacidad completa; solo el contenido del item (texto, checkbox) se atenúa.

**`LinearProgressIndicator` encima de la lista**: la barra de progreso solo aparece cuando `totalItems > 0`, evitando mostrarla en la pantalla vacía.

**`ItemRow` extraído**: evita duplicar el bloque de `SwipeToDismissBox` + `LaunchedEffect` + `ListItem` que se usaba en ambas secciones (pendientes y listos).

---

## Build

```
BUILD SUCCESSFUL in 16s — sin warnings
```
