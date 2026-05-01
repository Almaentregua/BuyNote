package com.martinjm.buynote.ui.screens.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinjm.buynote.domain.model.ListStatus
import com.martinjm.buynote.domain.model.ShoppingList
import com.martinjm.buynote.domain.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ActiveListsUiState(
    val lists: List<ShoppingListUiModel> = emptyList(),
    val isLoading: Boolean = true
)

data class ShoppingListUiModel(
    val id: Long,
    val name: String,
    val createdAt: String,
    val totalItems: Int,
    val checkedItems: Int
)

@HiltViewModel
class ActiveListsViewModel @Inject constructor(
    private val repository: ShoppingListRepository
) : ViewModel() {

    val uiState: StateFlow<ActiveListsUiState> = combine(
        repository.getActive(),
        repository.getItemCountsPerList()
    ) { lists, counts ->
        ActiveListsUiState(
            lists = lists.map { list ->
                val (total, checked) = counts[list.id] ?: (0 to 0)
                ShoppingListUiModel(
                    id = list.id,
                    name = list.name,
                    createdAt = formatDate(list.createdAt),
                    totalItems = total,
                    checkedItems = checked
                )
            },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ActiveListsUiState()
    )

    private val _navigateToDetail = MutableSharedFlow<Long>()
    val navigateToDetail: SharedFlow<Long> = _navigateToDetail.asSharedFlow()

    fun createList(name: String) {
        viewModelScope.launch {
            val id = repository.insert(
                ShoppingList(
                    name = name.trim(),
                    status = ListStatus.ACTIVE,
                    createdAt = System.currentTimeMillis()
                )
            )
            _navigateToDetail.emit(id)
        }
    }

    private fun formatDate(epochMillis: Long): String {
        val sdf = SimpleDateFormat("d 'de' MMM", Locale.forLanguageTag("es"))
        return sdf.format(Date(epochMillis))
    }
}
