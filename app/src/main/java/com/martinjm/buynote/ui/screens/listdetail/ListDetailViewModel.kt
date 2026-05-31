package com.martinjm.buynote.ui.screens.listdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinjm.buynote.domain.model.ListStatus
import com.martinjm.buynote.domain.model.QuantityUnit
import com.martinjm.buynote.domain.model.ShoppingListItem
import com.martinjm.buynote.domain.repository.CategoryRepository
import com.martinjm.buynote.domain.repository.ProductRepository
import com.martinjm.buynote.domain.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListDetailUiState(
    val listName: String = "",
    val totalItems: Int = 0,
    val checkedItems: Int = 0,
    val items: List<ShoppingListItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isCompleted: Boolean = false
)

enum class SortMode { INSERTION, BY_CATEGORY }

data class ShoppingListItemUiModel(
    val id: Long,
    val displayName: String,
    val brandName: String?,
    val quantityDisplay: String,
    val isChecked: Boolean,
    val quantity: Double,
    val unit: QuantityUnit,
    val customName: String?,
    val categoryName: String?
) {
    val isAdHoc: Boolean get() = customName != null
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShoppingListRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val listId: Long = checkNotNull(savedStateHandle["listId"])

    val uiState: StateFlow<ListDetailUiState> = combine(
        flow { emit(repository.getById(listId)) },
        repository.getItemsByListId(listId),
        productRepository.getAll(),
        categoryRepository.getAll()
    ) { list, items, products, categories ->
        val productById = products.associateBy { it.id }
        val categoryById = categories.associateBy { it.id }
        ListDetailUiState(
            listName = list?.name ?: "",
            items = items.map { item ->
                val product = item.productId?.let { productById[it] }
                ShoppingListItemUiModel(
                    id = item.id,
                    displayName = product?.name ?: item.customName ?: "",
                    brandName = product?.brand,
                    quantityDisplay = formatQuantity(item.quantity, item.unit),
                    isChecked = item.isChecked,
                    quantity = item.quantity,
                    unit = item.unit,
                    customName = item.customName,
                    categoryName = product?.categoryId?.let { categoryById[it]?.name }
                )
            },
            totalItems = items.size,
            checkedItems = items.count { it.isChecked },
            isLoading = false,
            isCompleted = list?.status == ListStatus.COMPLETED
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListDetailUiState()
    )

    private val _sortMode = MutableStateFlow(SortMode.INSERTION)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    fun toggleSortMode() = _sortMode.update {
        if (it == SortMode.INSERTION) SortMode.BY_CATEGORY else SortMode.INSERTION
    }

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

    fun completeList() {
        viewModelScope.launch {
            val list = repository.getById(listId) ?: return@launch
            repository.update(list.copy(
                status = ListStatus.COMPLETED,
                completedAt = System.currentTimeMillis()
            ))
            _navigateBack.emit(Unit)
        }
    }

    fun deleteList() {
        viewModelScope.launch {
            repository.deleteById(listId)
            _navigateBack.emit(Unit)
        }
    }

    // --- Picker de catálogo ---

    private val _pickerQuery = MutableStateFlow("")
    val pickerQuery: StateFlow<String> = _pickerQuery.asStateFlow()

    val pickerResults = _pickerQuery
        .debounce { if (it.isEmpty()) 0L else 250L }
        .distinctUntilChanged()
        .flatMapLatest { query -> productRepository.search(query) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun onPickerQueryChange(query: String) = _pickerQuery.update { query }

    fun resetPickerQuery() = _pickerQuery.update { "" }

    fun addItemFromCatalog(productId: Long, quantity: Double, unit: QuantityUnit) {
        viewModelScope.launch {
            repository.insertItem(
                ShoppingListItem(
                    listId = listId,
                    productId = productId,
                    quantity = quantity,
                    unit = unit
                )
            )
        }
    }

    fun addAdHocItem(name: String, quantity: Double, unit: QuantityUnit) {
        viewModelScope.launch {
            repository.insertItem(
                ShoppingListItem(
                    listId = listId,
                    customName = name.trim(),
                    quantity = quantity,
                    unit = unit
                )
            )
        }
    }

    fun updateItem(id: Long, customName: String?, quantity: Double, unit: QuantityUnit) {
        viewModelScope.launch {
            val item = repository.getItemById(id) ?: return@launch
            repository.updateItem(item.copy(
                customName = customName?.trim(),
                quantity = quantity,
                unit = unit
            ))
        }
    }

    fun toggleItem(id: Long, isChecked: Boolean) {
        viewModelScope.launch {
            val item = repository.getItemById(id) ?: return@launch
            repository.updateItem(item.copy(isChecked = isChecked))
        }
    }

    private var lastDeletedItem: ShoppingListItem? = null

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            val item = repository.getItemById(id) ?: return@launch
            lastDeletedItem = item
            repository.deleteItemById(id)
        }
    }

    fun undoDeleteItem() {
        val item = lastDeletedItem ?: return
        lastDeletedItem = null
        viewModelScope.launch {
            repository.insertItem(item.copy(id = 0L))
        }
    }

    // --- Scanner ---

    private val _barcodeResult = MutableSharedFlow<BarcodeResult>()
    val barcodeResult: SharedFlow<BarcodeResult> = _barcodeResult.asSharedFlow()

    fun handleScannedBarcode(barcode: String) {
        viewModelScope.launch {
            val product = productRepository.findByBarcode(barcode)
            if (product != null) {
                _barcodeResult.emit(BarcodeResult.Found(product))
            } else {
                _barcodeResult.emit(BarcodeResult.NotFound(barcode))
            }
        }
    }

    fun addProductFromScanner(productId: Long) {
        viewModelScope.launch {
            val product = productRepository.getById(productId) ?: return@launch
            _barcodeResult.emit(BarcodeResult.Found(product))
        }
    }
}

sealed class BarcodeResult {
    data class Found(val product: com.martinjm.buynote.domain.model.Product) : BarcodeResult()
    data class NotFound(val barcode: String) : BarcodeResult()
}

fun QuantityUnit.displayLabel() = when (this) {
    QuantityUnit.UNIT -> "unid."
    QuantityUnit.KG -> "kg"
    QuantityUnit.G -> "g"
    QuantityUnit.L -> "L"
    QuantityUnit.ML -> "mL"
}

private fun formatQuantity(quantity: Double, unit: QuantityUnit): String {
    val qStr = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
    return "$qStr ${unit.displayLabel()}"
}
