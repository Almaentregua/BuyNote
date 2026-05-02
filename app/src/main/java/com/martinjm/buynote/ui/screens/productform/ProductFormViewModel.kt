package com.martinjm.buynote.ui.screens.productform

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinjm.buynote.domain.model.Product
import com.martinjm.buynote.domain.repository.CategoryRepository
import com.martinjm.buynote.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductFormUiState(
    val name: String = "",
    val brand: String = "",
    val barcode: String = "",
    val notes: String = "",
    val selectedCategoryId: Long? = null,
    val categories: List<CategoryOption> = emptyList(),
    val nameError: String? = null,
    val barcodeError: String? = null,
    val isLoading: Boolean = true
)

data class CategoryOption(val id: Long, val name: String)

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productId: Long = savedStateHandle["productId"] ?: -1L
    private val initialName: String? = savedStateHandle["name"]
    private val initialBarcode: String? = savedStateHandle["barcode"]
    val isEditing: Boolean = productId != -1L
    val fromScanner: Boolean = !isEditing && initialBarcode != null

    private val _uiState = MutableStateFlow(ProductFormUiState())
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Long?>()
    val navigateBack: SharedFlow<Long?> = _navigateBack.asSharedFlow()

    init {
        viewModelScope.launch {
            categoryRepository.getAll().collect { categories ->
                _uiState.update { state ->
                    state.copy(categories = categories.map { CategoryOption(it.id, it.name) })
                }
            }
        }
        viewModelScope.launch {
            if (isEditing) {
                productRepository.getById(productId)?.let { p ->
                    _uiState.update {
                        it.copy(
                            name = p.name,
                            brand = p.brand ?: "",
                            barcode = p.barcode ?: "",
                            notes = p.notes ?: "",
                            selectedCategoryId = p.categoryId
                        )
                    }
                }
            } else {
                if (initialName != null) _uiState.update { it.copy(name = initialName) }
                if (initialBarcode != null) _uiState.update { it.copy(barcode = initialBarcode) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = null) }
    fun onBrandChange(value: String) = _uiState.update { it.copy(brand = value) }
    fun onBarcodeChange(value: String) = _uiState.update { it.copy(barcode = value, barcodeError = null) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }
    fun onCategorySelected(categoryId: Long?) = _uiState.update { it.copy(selectedCategoryId = categoryId) }

    fun delete() {
        check(isEditing)
        viewModelScope.launch {
            productRepository.deleteById(productId)
            _navigateBack.emit(null)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "El nombre es obligatorio") }
            return
        }
        viewModelScope.launch {
            try {
                val product = Product(
                    id = if (isEditing) productId else 0L,
                    name = state.name.trim(),
                    brand = state.brand.trim().ifBlank { null },
                    barcode = state.barcode.trim().ifBlank { null },
                    categoryId = state.selectedCategoryId,
                    notes = state.notes.trim().ifBlank { null }
                )
                if (isEditing) {
                    productRepository.update(product)
                    _navigateBack.emit(null)
                } else {
                    val newId = productRepository.insert(product)
                    _navigateBack.emit(newId)
                }
            } catch (e: SQLiteConstraintException) {
                _uiState.update { it.copy(barcodeError = "Este código ya existe en el catálogo") }
            }
        }
    }
}
