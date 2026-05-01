package com.martinjm.buynote.di

import android.content.Context
import androidx.room.Room
import com.martinjm.buynote.data.db.AppDatabase
import com.martinjm.buynote.data.db.dao.CategoryDao
import com.martinjm.buynote.data.db.dao.ProductDao
import com.martinjm.buynote.data.db.dao.ShoppingListDao
import com.martinjm.buynote.data.db.dao.ShoppingListItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "buynote.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideShoppingListDao(db: AppDatabase): ShoppingListDao = db.shoppingListDao()

    @Provides
    fun provideShoppingListItemDao(db: AppDatabase): ShoppingListItemDao = db.shoppingListItemDao()
}
