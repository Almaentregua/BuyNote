package com.martinjm.buynote.domain.repository

import com.martinjm.buynote.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAll(): Flow<List<Category>>
    suspend fun getById(id: Long): Category?
    suspend fun insert(category: Category): Long
    suspend fun update(category: Category): Int
    suspend fun delete(category: Category): Int
}
