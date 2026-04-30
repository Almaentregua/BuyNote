package com.martinjm.buynote.data.repository

import com.martinjm.buynote.data.db.dao.ProductDao
import com.martinjm.buynote.data.db.entity.ProductEntity
import com.martinjm.buynote.domain.model.Product
import com.martinjm.buynote.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val dao: ProductDao
) : ProductRepository {

    override fun getAll(): Flow<List<Product>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun search(query: String): Flow<List<Product>> =
        dao.search(query).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Product? =
        dao.getById(id)?.toDomain()

    override suspend fun findByBarcode(barcode: String): Product? =
        dao.findByBarcode(barcode)?.toDomain()

    override suspend fun insert(product: Product): Long =
        dao.insert(product.toEntity())

    override suspend fun update(product: Product): Int =
        dao.update(product.toEntity())

    override suspend fun delete(product: Product): Int =
        dao.delete(product.toEntity())

    override suspend fun deleteById(id: Long): Int =
        dao.deleteById(id)
}

private fun ProductEntity.toDomain() = Product(
    id = id,
    name = name,
    brand = brand,
    barcode = barcode,
    categoryId = categoryId,
    notes = notes
)

private fun Product.toEntity() = ProductEntity(
    id = id,
    name = name,
    brand = brand,
    barcode = barcode,
    categoryId = categoryId,
    notes = notes
)
