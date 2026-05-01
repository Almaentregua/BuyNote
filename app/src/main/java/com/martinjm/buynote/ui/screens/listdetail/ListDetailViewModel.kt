package com.martinjm.buynote.ui.screens.listdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinjm.buynote.domain.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ListDetailUiState(
    val listName: String = "",
    val totalItems: Int = 0,
    val checkedItems: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShoppingListRepository
) : ViewModel() {

    private val listId: Long = checkNotNull(savedStateHandle["listId"])

    val uiState: StateFlow<ListDetailUiState> = combine(
        flow { emit(repository.getById(listId)) },
        repository.getItemsByListId(listId)
    ) { list, items ->
        ListDetailUiState(
            listName = list?.name ?: "",
            totalItems = items.size,
            checkedItems = items.count { it.isChecked },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListDetailUiState()
    )
}
