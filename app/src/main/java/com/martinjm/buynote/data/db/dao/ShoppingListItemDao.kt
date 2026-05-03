package com.martinjm.buynote.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.martinjm.buynote.data.db.entity.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow

data class ListItemCounts(val listId: Long, val total: Int, val checked: Int)

@Dao
interface ShoppingListItemDao {
    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId ORDER BY id ASC")
    fun getByListId(listId: Long): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items WHERE id = :id")
    suspend fun getById(id: Long): ShoppingListItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ShoppingListItemEntity): Long

    @Update
    suspend fun update(item: ShoppingListItemEntity): Int

    @Delete
    suspend fun delete(item: ShoppingListItemEntity): Int

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("UPDATE shopping_list_items SET customName = :name WHERE productId = :productId AND customName IS NULL")
    suspend fun inheritProductName(productId: Long, name: String)

    @Query("SELECT listId, COUNT(*) as total, SUM(isChecked) as checked FROM shopping_list_items GROUP BY listId")
    fun getCountsPerList(): Flow<List<ListItemCounts>>
}
