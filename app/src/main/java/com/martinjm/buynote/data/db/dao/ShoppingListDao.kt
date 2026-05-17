package com.martinjm.buynote.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.martinjm.buynote.data.db.entity.ShoppingListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_lists WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getActive(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompleted(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE id = :id")
    suspend fun getById(id: Long): ShoppingListEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(list: ShoppingListEntity): Long

    @Update
    suspend fun update(list: ShoppingListEntity): Int

    @Delete
    suspend fun delete(list: ShoppingListEntity): Int

    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM shopping_lists WHERE status = 'COMPLETED'")
    suspend fun deleteAllCompleted()
}
