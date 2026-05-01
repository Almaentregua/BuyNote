# Historia 2.6 — Agregar item ad-hoc

**Estado**: Completada  
**Fecha**: 2026-05-01

---

## Criterios de aceptación — verificación

| Criterio | Estado |
|---|---|
| Form con: nombre*, cantidad, unidad | ✅ |
| Al guardar, se ofrece checkbox "Guardar también al catálogo" | ✅ |
| Si se marca, abre el form de producto con el nombre precargado | ✅ |

---

## Archivos modificados

- `ui/navigation/AppNavigation.kt` — ruta `product_form` ampliada con arg `name` (nullable String)
- `ui/screens/productform/ProductFormViewModel.kt` — lee `initialName` de `SavedStateHandle`, precarga el campo nombre en modo creación
- `ui/screens/listdetail/ListDetailViewModel.kt` — nuevo `addAdHocItem(name, quantity, unit)`
- `ui/screens/listdetail/ListDetailScreen.kt` — `AdHocFormSheet` (ModalBottomSheet), `onAddAdHoc` ya no es parámetro externo
- `ui/screens/productform/ProductFormScreen.kt` — fix deprecation `MenuAnchorType` → `ExposedDropdownMenuAnchorType` (aprovechado durante la edición)

---

## Decisiones de diseño

**Nombre precargado via query param en la ruta**: la forma más directa de pasar el nombre al `ProductFormScreen` es agregar `name` como argumento opcional en la ruta `product_form?productId={productId}&name={name}`. `Uri.encode(name)` asegura que espacios y caracteres especiales no rompan la URL. El ViewModel lo lee de `SavedStateHandle["name"]` y pre-popula el campo solo en modo creación (no edición).

**`AdHocFormContent` como ModalBottomSheet**: consistente con el picker de catálogo. El formulario vive en el mismo archivo que `ListDetailScreen` ya que es una responsabilidad de esa pantalla. No se creó un ViewModel propio — el estado del form (name, quantity, unit, saveToCatalog) es efímero y local al Composable.

**Checkbox "Guardar al catálogo" en el form, no en un diálogo posterior**: más simple que abrir un diálogo de confirmación después de guardar. El usuario decide antes de confirmar. Si lo marca, al presionar "Guardar" se hace el insert del item y se navega al form de producto con el nombre precargado.

**`addAdHocItem()` en `ListDetailViewModel`**: inserta con `customName = name.trim()` y `productId = null`. La lista se actualiza reactivamente via el Flow de Room.

**`onAddAdHoc` removido como parámetro externo**: el ad-hoc form es un concern interno de `ListDetailScreen`, igual que el picker de catálogo. `AppNavigation` no necesita cambios para este flujo.

---

## Build

```
BUILD SUCCESSFUL in 33s — sin warnings
```
