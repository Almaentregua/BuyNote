package com.martinjm.buynote.ui.screens.listdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.martinjm.buynote.domain.model.Product
import com.martinjm.buynote.domain.model.QuantityUnit
import com.martinjm.buynote.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    navController: NavHostController,
    viewModel: ListDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pickerQuery by viewModel.pickerQuery.collectAsStateWithLifecycle()
    val pickerResults by viewModel.pickerResults.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var showCatalogPicker by remember { mutableStateOf(false) }
    var showAdHocSheet by remember { mutableStateOf(false) }
    var pendingProduct by remember { mutableStateOf<Product?>(null) }
    var editingItem by remember { mutableStateOf<ShoppingListItemUiModel?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val onItemDelete: (ShoppingListItemUiModel) -> Unit = { item ->
        viewModel.deleteItem(item.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "\"${item.displayName}\" eliminado",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeleteItem()
            }
        }
    }

    val addSheetState = rememberModalBottomSheetState()
    val pickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.listName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.totalItems > 0) {
                            Text(
                                text = "${uiState.checkedItems} de ${uiState.totalItems} listos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Agregar") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> Unit
                uiState.items.isEmpty() -> EmptyList(
                    onAddFromCatalog = { showCatalogPicker = true },
                    onAddAdHoc = { showAdHocSheet = true },
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> ItemsList(
                    items = uiState.items,
                    onItemClick = { editingItem = it },
                    onItemToggle = { id, checked -> viewModel.toggleItem(id, checked) },
                    onItemDelete = onItemDelete,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // --- Bottom sheet: elegir tipo de item ---
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = addSheetState
        ) {
            Text(
                text = "Agregar item",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Desde catálogo") },
                supportingContent = { Text("Elegí un producto que ya tenés guardado") },
                leadingContent = { Icon(Icons.Outlined.Inventory2, contentDescription = null) },
                modifier = Modifier.clickable {
                    showAddSheet = false
                    showCatalogPicker = true
                }
            )
            ListItem(
                headlineContent = { Text("Item ad-hoc") },
                supportingContent = { Text("Escribí el nombre del item libremente") },
                leadingContent = {
                    Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, contentDescription = null)
                },
                modifier = Modifier
                    .clickable {
                        showAddSheet = false
                        showAdHocSheet = true
                    }
                    .padding(bottom = 8.dp)
            )
        }
    }

    // --- Bottom sheet: form ad-hoc ---
    if (showAdHocSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAdHocSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AdHocFormContent(
                onConfirm = { name, quantity, unit, saveToCatalog ->
                    viewModel.addAdHocItem(name, quantity, unit)
                    showAdHocSheet = false
                    if (saveToCatalog) {
                        navController.navigate(Routes.productForm(name = name))
                    }
                },
                onDismiss = { showAdHocSheet = false }
            )
        }
    }

    // --- Bottom sheet: picker de catálogo ---
    if (showCatalogPicker) {
        ModalBottomSheet(
            onDismissRequest = {
                showCatalogPicker = false
                viewModel.resetPickerQuery()
            },
            sheetState = pickerSheetState
        ) {
            CatalogPickerContent(
                query = pickerQuery,
                results = pickerResults,
                onQueryChange = viewModel::onPickerQueryChange,
                onProductSelected = { product ->
                    pendingProduct = product
                    showCatalogPicker = false
                    viewModel.resetPickerQuery()
                }
            )
        }
    }

    // --- Diálogo: cantidad y unidad ---
    pendingProduct?.let { product ->
        QuantityPickerDialog(
            productName = product.name,
            onConfirm = { quantity, unit ->
                viewModel.addItemFromCatalog(product.id, quantity, unit)
                pendingProduct = null
            },
            onDismiss = { pendingProduct = null }
        )
    }

    // --- Bottom sheet: editar item ---
    editingItem?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { editingItem = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            EditItemContent(
                item = item,
                onConfirm = { customName, quantity, unit ->
                    viewModel.updateItem(item.id, customName, quantity, unit)
                    editingItem = null
                },
                onDismiss = { editingItem = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemsList(
    items: List<ShoppingListItemUiModel>,
    onItemClick: (ShoppingListItemUiModel) -> Unit,
    onItemToggle: (id: Long, isChecked: Boolean) -> Unit,
    onItemDelete: (ShoppingListItemUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            val dismissState = rememberSwipeToDismissBoxState()
            LaunchedEffect(dismissState.currentValue) {
                if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                    onItemDelete(item)
                }
            }
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(end = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            ) {
                ListItem(
                    headlineContent = { Text(item.displayName) },
                    supportingContent = { Text(item.quantityDisplay) },
                    leadingContent = {
                        Checkbox(
                            checked = item.isChecked,
                            onCheckedChange = { onItemToggle(item.id, it) }
                        )
                    },
                    modifier = Modifier.clickable { onItemClick(item) }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogPickerContent(
    query: String,
    results: List<Product>,
    onQueryChange: (String) -> Unit,
    onProductSelected: (Product) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Elegí un producto",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Buscar por nombre o marca...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Clear, null) } }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focusRequester)
        )
        HorizontalDivider()
        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (query.isEmpty()) "El catálogo está vacío" else "Sin resultados para \"$query\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                items(results, key = { it.id }) { product ->
                    ListItem(
                        headlineContent = { Text(product.name) },
                        supportingContent = product.brand?.let { { Text(it) } },
                        modifier = Modifier.clickable { onProductSelected(product) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuantityPickerDialog(
    productName: String,
    onConfirm: (Double, QuantityUnit) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityText by remember { mutableStateOf("1") }
    var selectedUnit by remember { mutableStateOf(QuantityUnit.UNIT) }
    var unitExpanded by remember { mutableStateOf(false) }

    val quantity = quantityText.toDoubleOrNull()
    val isValid = quantity != null && quantity > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(productName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = !isValid,
                    modifier = Modifier.weight(1f)
                )
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedUnit.displayLabel(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unidad") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                        },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        QuantityUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.displayLabel()) },
                                onClick = {
                                    selectedUnit = unit
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(quantity!!, selectedUnit) },
                enabled = isValid
            ) { Text("Agregar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdHocFormContent(
    onConfirm: (name: String, quantity: Double, unit: QuantityUnit, saveToCatalog: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var selectedUnit by remember { mutableStateOf(QuantityUnit.UNIT) }
    var unitExpanded by remember { mutableStateOf(false) }
    var saveToCatalog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val quantity = quantityText.toDoubleOrNull()
    val isValid = name.isNotBlank() && quantity != null && quantity > 0

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Agregar item",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                placeholder = { Text("Ej: Detergente") },
                singleLine = true,
                isError = name.isBlank() && name.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = quantity == null || quantity <= 0,
                    modifier = Modifier.weight(1f)
                )
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedUnit.displayLabel(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unidad") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                        },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        QuantityUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.displayLabel()) },
                                onClick = { selectedUnit = unit; unitExpanded = false }
                            )
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { saveToCatalog = !saveToCatalog }
            ) {
                Checkbox(
                    checked = saveToCatalog,
                    onCheckedChange = { saveToCatalog = it }
                )
                Text(
                    text = "Guardar también al catálogo",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                TextButton(
                    onClick = { onConfirm(name, quantity!!, selectedUnit, saveToCatalog) },
                    enabled = isValid
                ) { Text("Guardar") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditItemContent(
    item: ShoppingListItemUiModel,
    onConfirm: (customName: String?, quantity: Double, unit: QuantityUnit) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(item.customName ?: "") }
    var quantityText by remember { mutableStateOf(
        if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
    ) }
    var selectedUnit by remember { mutableStateOf(item.unit) }
    var unitExpanded by remember { mutableStateOf(false) }

    val quantity = quantityText.toDoubleOrNull()
    val isValid = quantity != null && quantity > 0 && (!item.isAdHoc || name.isNotBlank())

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Editar item",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (item.isAdHoc) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    isError = name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = quantity == null || quantity <= 0,
                    modifier = Modifier.weight(1f)
                )
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedUnit.displayLabel(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unidad") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                        },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        QuantityUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.displayLabel()) },
                                onClick = { selectedUnit = unit; unitExpanded = false }
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                TextButton(
                    onClick = {
                        onConfirm(if (item.isAdHoc) name else null, quantity!!, selectedUnit)
                    },
                    enabled = isValid
                ) { Text("Guardar") }
            }
        }
    }
}

@Composable
private fun EmptyList(
    onAddFromCatalog: () -> Unit,
    onAddAdHoc: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.ShoppingBasket,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Text(
            text = "La lista está vacía",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Agregá items desde tu catálogo o escribí uno directamente",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ListItem(
                headlineContent = { Text("Desde catálogo") },
                leadingContent = { Icon(Icons.Outlined.Inventory2, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onAddFromCatalog)
            )
            ListItem(
                headlineContent = { Text("Item ad-hoc") },
                leadingContent = {
                    Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = onAddAdHoc)
            )
        }
    }
}
