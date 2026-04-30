package com.martinjm.buynote.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinjm.buynote.domain.repository.CategoryRepository
import com.martinjm.buynote.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    val uiState: StateFlow<CatalogUiState> = combine(
        productRepository.getAll(),
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
