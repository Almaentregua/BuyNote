package com.martinjm.buynote.data.repository

import com.martinjm.buynote.data.db.dao.CategoryDao
import com.martinjm.buynote.data.db.entity.CategoryEntity
import com.martinjm.buynote.domain.model.Category
import com.martinjm.buynote.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {

    override fun getAll(): Flow<List<Category>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Category? =
        dao.getById(id)?.toDomain()

    override suspend fun insert(category: Category): Long =
        dao.insert(category.toEntity())

    override suspend fun update(category: Category): Int =
        dao.update(category.toEntity())

    override suspend fun delete(category: Category): Int =
        dao.delete(category.toEntity())
}

private fun CategoryEntity.toDomain() = Category(id = id, name = name)
private fun Category.toEntity() = CategoryEntity(id = id, name = name)
