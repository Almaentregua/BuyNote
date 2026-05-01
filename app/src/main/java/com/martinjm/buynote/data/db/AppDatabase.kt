package com.martinjm.buynote.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.martinjm.buynote.data.db.converter.Converters
import com.martinjm.buynote.data.db.dao.CategoryDao
import com.martinjm.buynote.data.db.dao.ProductDao
import com.martinjm.buynote.data.db.dao.ShoppingListDao
import com.martinjm.buynote.data.db.dao.ShoppingListItemDao
import com.martinjm.buynote.data.db.entity.CategoryEntity
import com.martinjm.buynote.data.db.entity.ProductEntity
import com.martinjm.buynote.data.db.entity.ShoppingListEntity
import com.martinjm.buynote.data.db.entity.ShoppingListItemEntity

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        ShoppingListEntity::class,
        ShoppingListItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingListItemDao(): ShoppingListItemDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_lists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_list_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        listId INTEGER NOT NULL,
                        productId INTEGER,
                        customName TEXT,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        isChecked INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (listId) REFERENCES shopping_lists(id) ON DELETE CASCADE,
                        FOREIGN KEY (productId) REFERENCES products(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_listId ON shopping_list_items(listId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_productId ON shopping_list_items(productId)")
            }
        }
    }
}
