package com.martinjm.buynote.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.martinjm.buynote.data.db.AppDatabase
import com.martinjm.buynote.data.db.dao.CategoryDao
import com.martinjm.buynote.data.db.dao.ProductDao
import com.martinjm.buynote.data.db.entity.CategoryEntity
import com.martinjm.buynote.data.db.entity.ProductEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var productDao: ProductDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        categoryDao = db.categoryDao()
        productDao = db.productDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // --- CategoryDao ---

    @Test
    fun insertCategory_andGetAll_returnsIt() = runTest {
        val id = categoryDao.insert(CategoryEntity(name = "Lácteos"))
        val all = categoryDao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("Lácteos", all[0].name)
        assertEquals(id, all[0].id)
    }

    @Test
    fun updateCategory_changesName() = runTest {
        val id = categoryDao.insert(CategoryEntity(name = "Bebidas"))
        val updated = CategoryEntity(id = id, name = "Bebidas sin alcohol")
        categoryDao.update(updated)
        val found = categoryDao.getById(id)
        assertEquals("Bebidas sin alcohol", found?.name)
    }

    @Test
    fun deleteCategory_removesIt() = runTest {
        val id = categoryDao.insert(CategoryEntity(name = "Limpieza"))
        val entity = categoryDao.getById(id)!!
        categoryDao.delete(entity)
        val all = categoryDao.getAll().first()
        assertTrue(all.isEmpty())
    }

    // --- ProductDao ---

    @Test
    fun insertProduct_andGetAll_returnsIt() = runTest {
        val product = ProductEntity(name = "Leche", brand = "La Serenísima")
        val id = productDao.insert(product)
        val all = productDao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("Leche", all[0].name)
        assertEquals(id, all[0].id)
    }

    @Test
    fun searchProduct_byName_returnsMatch() = runTest {
        productDao.insert(ProductEntity(name = "Yerba Playadito", brand = "Playadito"))
        productDao.insert(ProductEntity(name = "Leche entera", brand = "La Serenísima"))
        val results = productDao.search("yerba").first()
        assertEquals(1, results.size)
        assertEquals("Yerba Playadito", results[0].name)
    }

    @Test
    fun searchProduct_byBrand_returnsMatch() = runTest {
        productDao.insert(ProductEntity(name = "Manteca", brand = "La Serenísima"))
        productDao.insert(ProductEntity(name = "Atún", brand = "Cormoran"))
        val results = productDao.search("Serenísima").first()
        assertEquals(1, results.size)
        assertEquals("Manteca", results[0].name)
    }

    @Test
    fun searchProduct_emptyQuery_returnsAll() = runTest {
        productDao.insert(ProductEntity(name = "Arroz"))
        productDao.insert(ProductEntity(name = "Fideos"))
        val results = productDao.search("").first()
        assertEquals(2, results.size)
    }

    @Test
    fun findByBarcode_returnsCorrectProduct() = runTest {
        productDao.insert(ProductEntity(name = "Coca-Cola 1.5L", barcode = "7790895000119"))
        val found = productDao.findByBarcode("7790895000119")
        assertNotNull(found)
        assertEquals("Coca-Cola 1.5L", found?.name)
    }

    @Test
    fun findByBarcode_unknown_returnsNull() = runTest {
        val found = productDao.findByBarcode("9999999999999")
        assertNull(found)
    }

    @Test
    fun deleteProduct_setsNullFkOnCategory() = runTest {
        // La FK en ShoppingListItem usa ON DELETE SET NULL; aquí testeamos
        // que borrar una categoría no rompe los productos (FK categoryId → SET_NULL)
        val catId = categoryDao.insert(CategoryEntity(name = "Almacén"))
        val prodId = productDao.insert(ProductEntity(name = "Arroz", categoryId = catId))
        val cat = categoryDao.getById(catId)!!
        categoryDao.delete(cat)
        val prod = productDao.getById(prodId)
        assertNotNull(prod)
        assertNull(prod?.categoryId)
    }
}
