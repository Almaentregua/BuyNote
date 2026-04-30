package com.martinjm.buynote.domain.repository

import com.martinjm.buynote.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAll(): Flow<List<Product>>
    fun search(query: String): Flow<List<Product>>
    suspend fun getById(id: Long): Product?
    suspend fun findByBarcode(barcode: String): Product?
    suspend fun insert(product: Product): Long
    suspend fun update(product: Product): Int
    suspend fun delete(product: Product): Int
}
