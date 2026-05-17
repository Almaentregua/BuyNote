package com.martinjm.buynote.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinjm.buynote.domain.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HistoryListUiModel(
    val id: Long,
    val name: String,
    val createdAt: String,
    val completedAt: String,
    val totalItems: Int,
    val checkedItems: Int
)

data class HistoryUiState(
    val lists: List<HistoryListUiModel> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ShoppingListRepository
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("d 'de' MMM yyyy", Locale.forLanguageTag("es"))

    fun deleteList(id: Long) {
        viewModelScope.launch { repository.deleteById(id) }
    }

    fun deleteAll() {
        viewModelScope.launch { repository.deleteAllCompleted() }
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getCompleted(),
        repository.getItemCountsPerList()
    ) { lists, counts ->
        HistoryUiState(
            lists = lists
                .sortedByDescending { it.completedAt }
                .map { list ->
                    val (total, checked) = counts[list.id] ?: (0 to 0)
                    HistoryListUiModel(
                        id = list.id,
                        name = list.name,
                        createdAt = dateFormat.format(Date(list.createdAt)),
                        completedAt = list.completedAt?.let { dateFormat.format(Date(it)) } ?: "",
                        totalItems = total,
                        checkedItems = checked
                    )
                },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )
}
