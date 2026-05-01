package com.martinjm.buynote.ui.screens.listdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    navController: NavHostController,
    onAddFromCatalog: () -> Unit = {},
    onAddAdHoc: () -> Unit = {},
    viewModel: ListDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!uiState.isLoading && uiState.totalItems == 0) {
                EmptyList(
                    onAddFromCatalog = { showAddSheet = true },
                    onAddAdHoc = { showAddSheet = true },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState
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
                leadingContent = {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    showAddSheet = false
                    onAddFromCatalog()
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
                        onAddAdHoc()
                    }
                    .padding(bottom = 8.dp)
            )
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
                leadingContent = {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null)
                },
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
