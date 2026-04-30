package com.martinjm.buynote.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinjm.buynote.domain.repository.CategoryRepository
import com.martinjm.buynote.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class CatalogUiState(
    val products: List<ProductUiModel> = emptyList(),
    val isLoading: Boolean = true
)

data class ProductUiModel(
    val id: Long,
    val name: String,
    val brand: String?,
    val categoryName: String?
)

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onQueryChange(query: String) = _searchQuery.update { query }

    val uiState: StateFlow<CatalogUiState> = combine(
        _searchQuery
            .debounce { query -> if (query.isEmpty()) 0L else 250L }
            .distinctUntilChanged()
            .flatMapLatest { query -> productRepository.search(query) },
        categoryRepository.getAll()
    ) { products, categories ->
        val categoryById = categories.associateBy { it.id }
        CatalogUiState(
            products = products.map { p ->
                ProductUiModel(
                    id = p.id,
                    name = p.name,
                    brand = p.brand,
                    categoryName = p.categoryId?.let { categoryById[it]?.name }
                )
            },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogUiState()
    )
}
