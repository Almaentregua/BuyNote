package com.martinjm.buynote.domain.repository

import com.martinjm.buynote.domain.model.ShoppingList
import com.martinjm.buynote.domain.model.ShoppingListItem
import kotlinx.coroutines.flow.Flow

interface ShoppingListRepository {
    fun getActive(): Flow<List<ShoppingList>>
    fun getCompleted(): Flow<List<ShoppingList>>
    suspend fun getById(id: Long): ShoppingList?
    suspend fun insert(list: ShoppingList): Long
    suspend fun update(list: ShoppingList): Int
    suspend fun delete(list: ShoppingList): Int
    suspend fun deleteById(id: Long): Int
    suspend fun deleteAllCompleted()

    fun getItemsByListId(listId: Long): Flow<List<ShoppingListItem>>
    fun getItemCountsPerList(): Flow<Map<Long, Pair<Int, Int>>>
    suspend fun getItemById(id: Long): ShoppingListItem?
    suspend fun insertItem(item: ShoppingListItem): Long
    suspend fun updateItem(item: ShoppingListItem): Int
    suspend fun deleteItem(item: ShoppingListItem): Int
    suspend fun deleteItemById(id: Long): Int
    suspend fun detachProduct(productId: Long, productName: String)
}
