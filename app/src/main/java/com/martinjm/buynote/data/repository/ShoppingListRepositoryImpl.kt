package com.martinjm.buynote.data.repository

import com.martinjm.buynote.data.db.dao.ShoppingListDao
import com.martinjm.buynote.data.db.dao.ShoppingListItemDao
import com.martinjm.buynote.data.db.entity.ShoppingListEntity
import com.martinjm.buynote.data.db.entity.ShoppingListItemEntity
import com.martinjm.buynote.domain.model.ShoppingList
import com.martinjm.buynote.domain.model.ShoppingListItem
import com.martinjm.buynote.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingListRepositoryImpl @Inject constructor(
    private val listDao: ShoppingListDao,
    private val itemDao: ShoppingListItemDao
) : ShoppingListRepository {

    override fun getActive(): Flow<List<ShoppingList>> =
        listDao.getActive().map { it.map { e -> e.toDomain() } }

    override fun getCompleted(): Flow<List<ShoppingList>> =
        listDao.getCompleted().map { it.map { e -> e.toDomain() } }

    override suspend fun getById(id: Long): ShoppingList? =
        listDao.getById(id)?.toDomain()

    override suspend fun insert(list: ShoppingList): Long =
        listDao.insert(list.toEntity())

    override suspend fun update(list: ShoppingList): Int =
        listDao.update(list.toEntity())

    override suspend fun delete(list: ShoppingList): Int =
        listDao.delete(list.toEntity())

    override suspend fun deleteById(id: Long): Int =
        listDao.deleteById(id)

    override suspend fun deleteAllCompleted() =
        listDao.deleteAllCompleted()

    override fun getItemsByListId(listId: Long): Flow<List<ShoppingListItem>> =
        itemDao.getByListId(listId).map { it.map { e -> e.toDomain() } }

    override fun getItemCountsPerList(): Flow<Map<Long, Pair<Int, Int>>> =
        itemDao.getCountsPerList().map { list ->
            list.associate { it.listId to (it.total to it.checked) }
        }

    override suspend fun getItemById(id: Long): ShoppingListItem? =
        itemDao.getById(id)?.toDomain()

    override suspend fun insertItem(item: ShoppingListItem): Long =
        itemDao.insert(item.toEntity())

    override suspend fun updateItem(item: ShoppingListItem): Int =
        itemDao.update(item.toEntity())

    override suspend fun deleteItem(item: ShoppingListItem): Int =
        itemDao.delete(item.toEntity())

    override suspend fun deleteItemById(id: Long): Int =
        itemDao.deleteById(id)

    override suspend fun detachProduct(productId: Long, productName: String) =
        itemDao.inheritProductName(productId, productName)
}

private fun ShoppingListEntity.toDomain() = ShoppingList(
    id = id,
    name = name,
    status = status,
    createdAt = createdAt,
    completedAt = completedAt
)

private fun ShoppingList.toEntity() = ShoppingListEntity(
    id = id,
    name = name,
    status = status,
    createdAt = createdAt,
    completedAt = completedAt
)

private fun ShoppingListItemEntity.toDomain() = ShoppingListItem(
    id = id,
    listId = listId,
    productId = productId,
    customName = customName,
    quantity = quantity,
    unit = unit,
    isChecked = isChecked
)

private fun ShoppingListItem.toEntity() = ShoppingListItemEntity(
    id = id,
    listId = listId,
    productId = productId,
    customName = customName,
    quantity = quantity,
    unit = unit,
    isChecked = isChecked
)
