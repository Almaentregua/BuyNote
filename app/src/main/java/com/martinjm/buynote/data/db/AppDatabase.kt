package com.martinjm.buynote.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.martinjm.buynote.data.db.dao.CategoryDao
import com.martinjm.buynote.data.db.dao.ProductDao
import com.martinjm.buynote.data.db.entity.CategoryEntity
import com.martinjm.buynote.data.db.entity.ProductEntity

@Database(
    entities = [CategoryEntity::class, ProductEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
}
