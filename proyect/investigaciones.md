# Investigaciones y falsos positivos

Registro de comportamientos sospechosos que se investigaron pero no se confirmaron como bugs. Útil para retomar el análisis si el problema reaparece.

---

## [2026-05-02] Scanner desde lista: producto existente no reconocido

### Lo que se observó
Estando dentro de una lista activa, al intentar agregar un producto existente escaneando su código de barras, en vez de aparecer el diálogo de cantidad/unidad (flujo "encontrado"), se abría el formulario de nuevo producto (flujo "no encontrado"). Al completar el formulario, se podía guardar el producto sin error de constraint, resultando aparentemente en duplicados con el mismo código.

**No se pudo reproducir de forma consistente — podría haber sido un falso positivo.**

### Análisis de causas posibles

#### Causa A — Inconsistencia de formato entre barcode almacenado y ML Kit (más probable)
Un mismo código físico puede representarse en dos formatos:
- **UPC-A**: 12 dígitos (`012345678912`)
- **EAN-13**: 13 dígitos con 0 líder (`0012345678912`)

Si el producto fue cargado al catálogo con el barcode tipado a mano en un formato, y ML Kit lo devuelve en el otro, el `WHERE barcode = :barcode` falla porque es comparación exacta. Esto también explica por qué no salía el error de constraint UNIQUE al guardar: los dos barcodes son distintos como strings aunque refieran al mismo código físico.

**Cómo verificarlo**: comparar el valor exacto almacenado en la BD con el `rawValue` que devuelve ML Kit para el mismo producto físico.

#### Causa B — Race condition entre LaunchedEffects (descartada para el síntoma observado)
Al volver del scanner a ListDetail, dos `LaunchedEffect(Unit)` se relaunzan concurrentemente:
1. El que lee `savedStateHandle["barcode"]` y llama a `handleScannedBarcode`.
2. El que colecta `viewModel.barcodeResult`.

`_barcodeResult` es un `MutableSharedFlow()` con `replay = 0`. Si el emit de `Found` ocurre antes de que el colector del punto 2 esté suscrito, la emisión se pierde silenciosamente. **Pero** esto resultaría en que no pasa nada (ni diálogo ni navegación), no en que aparezca el ProductForm. Por eso esta causa no explica el síntoma observado.

#### Causa C — Producto en catálogo sin barcode almacenado
Si el producto existente fue creado sin completar el campo de código de barras, `findByBarcode` retorna null para cualquier código. El flujo NotFound abre el ProductForm con el barcode pre-llenado; al guardar, se crea un segundo producto con ese barcode. El resultado serían dos productos: uno sin barcode y uno con barcode — el usuario los percibe como duplicados aunque técnicamente no violan la constraint UNIQUE (que permite múltiples NULLs).

### Próximos pasos si reaparece
1. Verificar el valor exacto del campo `barcode` del producto en la BD (via logs o Room Inspector).
2. Logear el `rawValue` que devuelve ML Kit para ese mismo producto físico.
3. Comparar ambos strings carácter a carácter.
4. Si coinciden, activar logs en `handleScannedBarcode` para confirmar que `findByBarcode` realmente retorna null.
