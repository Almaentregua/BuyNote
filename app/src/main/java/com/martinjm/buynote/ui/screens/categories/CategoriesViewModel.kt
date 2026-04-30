package com.martinjm.buynote.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinjm.buynote.domain.model.Category
import com.martinjm.buynote.domain.repository.CategoryRepository
import com.martinjm.buynote.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val categoryDialog: CategoryDialog? = null,
    val deletionRequest: DeletionRequest? = null
)

data class CategoryDialog(
    val id: Long? = null,
    val name: String = "",
    val nameError: String? = null
)

data class DeletionRequest(
    val category: Category,
    val hasProducts: Boolean
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState

    init {
        viewModelScope.launch {
            categoryRepository.getAll().collect { categories ->
                _uiState.update { it.copy(categories = categories, isLoading = false) }
            }
        }
    }

    fun openCreateDialog() {
        _uiState.update { it.copy(categoryDialog = CategoryDialog()) }
    }

    fun openEditDialog(category: Category) {
        _uiState.update { it.copy(categoryDialog = CategoryDialog(id = category.id, name = category.name)) }
    }

    fun onDialogNameChange(name: String) {
        _uiState.update { state ->
            state.copy(categoryDialog = state.categoryDialog?.copy(name = name, nameError = null))
        }
    }

    fun saveCategory() {
        val dialog = _uiState.value.categoryDialog ?: return
        val trimmedName = dialog.name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(categoryDialog = dialog.copy(nameError = "El nombre no puede estar vacío")) }
            return
        }
        viewModelScope.launch {
            if (dialog.id == null) {
                categoryRepository.insert(Category(name = trimmedName))
            } else {
                categoryRepository.update(Category(id = dialog.id, name = trimmedName))
            }
            _uiState.update { it.copy(categoryDialog = null) }
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(categoryDialog = null) }
    }

    fun requestDelete(category: Category) {
        viewModelScope.launch {
            val products = productRepository.getAll().first()
            val hasProducts = products.any { it.categoryId == category.id }
            _uiState.update { it.copy(deletionRequest = DeletionRequest(category, hasProducts)) }
        }
    }

    fun confirmDelete() {
        val request = _uiState.value.deletionRequest ?: return
        viewModelScope.launch {
            categoryRepository.delete(request.category)
            _uiState.update { it.copy(deletionRequest = null) }
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(deletionRequest = null) }
    }
}
