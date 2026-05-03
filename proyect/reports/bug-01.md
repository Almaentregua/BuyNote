# Reporte — BUG-01: Ítem de lista pierde el nombre al eliminar el producto del catálogo

## Causa raíz

`ProductFormViewModel.delete()` llamaba a `productRepository.deleteById(productId)` directamente. Room aplicaba `ON DELETE SET NULL` sobre `productId` en los `ShoppingListItem` vinculados, pero `customName` también quedaba `null`. El display name resolvía `product?.name ?: item.customName ?: ""` → cadena vacía → checkbox sin texto.

## Fix

Se agregó un paso previo a la eliminación: copiar `product.name` a `customName` de todos los ítems que referencian ese `productId` y no tienen ya un `customName` propio.

### Archivos modificados

**`ShoppingListItemDao`** — nuevo query de UPDATE directo:
```sql
UPDATE shopping_list_items
SET customName = :name
WHERE productId = :productId AND customName IS NULL
```

**`ShoppingListRepository`** — nuevo método `detachProduct(productId, productName)`.

**`ShoppingListRepositoryImpl`** — implementación delegando al DAO.

**`ProductFormViewModel`**:
- Inyección de `ShoppingListRepository`.
- `delete()` ahora hace `getById` → `detachProduct` → `deleteById` en ese orden.

## Secuencia de operaciones garantizada

```
getById(productId)           → obtiene nombre del producto
detachProduct(id, name)      → UPDATE en shopping_list_items (single query)
deleteById(productId)        → DELETE del producto (Room aplica ON DELETE SET NULL)
```

El `ON DELETE SET NULL` de Room ya no importa para el display porque `customName` fue poblado antes.

## Verificado

- `assembleDebug` exitoso sin errores.
