package com.martinjm.buynote.di

import com.martinjm.buynote.data.repository.CategoryRepositoryImpl
import com.martinjm.buynote.data.repository.ProductRepositoryImpl
import com.martinjm.buynote.data.repository.ShoppingListRepositoryImpl
import com.martinjm.buynote.domain.repository.CategoryRepository
import com.martinjm.buynote.domain.repository.ProductRepository
import com.martinjm.buynote.domain.repository.ShoppingListRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    abstract fun bindShoppingListRepository(impl: ShoppingListRepositoryImpl): ShoppingListRepository
}
