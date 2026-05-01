package com.martinjm.buynote.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.martinjm.buynote.data.db.AppDatabase
import com.martinjm.buynote.data.db.dao.ProductDao
import com.martinjm.buynote.data.db.dao.ShoppingListDao
import com.martinjm.buynote.data.db.dao.ShoppingListItemDao
import com.martinjm.buynote.data.db.entity.ProductEntity
import com.martinjm.buynote.data.db.entity.ShoppingListEntity
import com.martinjm.buynote.data.db.entity.ShoppingListItemEntity
import com.martinjm.buynote.domain.model.ListStatus
import com.martinjm.buynote.domain.model.QuantityUnit
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
class ShoppingListDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var listDao: ShoppingListDao
    private lateinit var itemDao: ShoppingListItemDao
    private lateinit var productDao: ProductDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        listDao = db.shoppingListDao()
        itemDao = db.shoppingListItemDao()
        productDao = db.productDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun activeList(name: String) = ShoppingListEntity(
        name = name,
        status = ListStatus.ACTIVE,
        createdAt = System.currentTimeMillis()
    )

    private fun item(listId: Long, name: String, productId: Long? = null) = ShoppingListItemEntity(
        listId = listId,
        customName = name,
        productId = productId,
        quantity = 1.0,
        unit = QuantityUnit.UNIT
    )

    // --- ShoppingListDao ---

    @Test
    fun insertList_andGetActive_returnsIt() = runTest {
        listDao.insert(activeList("Supermercado"))
        val active = listDao.getActive().first()
        assertEquals(1, active.size)
        assertEquals("Supermercado", active[0].name)
        assertEquals(ListStatus.ACTIVE, active[0].status)
    }

    @Test
    fun completedList_notInActive_appearsInCompleted() = runTest {
        val id = listDao.insert(activeList("Ferretería"))
        val list = listDao.getById(id)!!
        listDao.update(list.copy(status = ListStatus.COMPLETED, completedAt = System.currentTimeMillis()))

        val active = listDao.getActive().first()
        val completed = listDao.getCompleted().first()

        assertTrue(active.isEmpty())
        assertEquals(1, completed.size)
        assertEquals("Ferretería", completed[0].name)
    }

    @Test
    fun getById_returnsCorrectList() = runTest {
        val id = listDao.insert(activeList("Verdulería"))
        val found = listDao.getById(id)
        assertNotNull(found)
        assertEquals("Verdulería", found?.name)
    }

    @Test
    fun getById_unknownId_returnsNull() = runTest {
        assertNull(listDao.getById(999L))
    }

    @Test
    fun deleteById_removesFromActive() = runTest {
        val id = listDao.insert(activeList("Farmacia"))
        listDao.deleteById(id)
        assertTrue(listDao.getActive().first().isEmpty())
    }

    // --- ShoppingListItemDao ---

    @Test
    fun insertItem_andGetByListId_returnsIt() = runTest {
        val listId = listDao.insert(activeList("Super"))
        itemDao.insert(item(listId, "Leche"))
        val items = itemDao.getByListId(listId).first()
        assertEquals(1, items.size)
        assertEquals("Leche", items[0].customName)
        assertEquals(QuantityUnit.UNIT, items[0].unit)
    }

    @Test
    fun updateItem_changesFields() = runTest {
        val listId = listDao.insert(activeList("Super"))
        val itemId = itemDao.insert(item(listId, "Arroz"))
        val saved = itemDao.getById(itemId)!!
        itemDao.update(saved.copy(quantity = 2.5, unit = QuantityUnit.KG, isChecked = true))
        val updated = itemDao.getById(itemId)!!
        assertEquals(2.5, updated.quantity, 0.001)
        assertEquals(QuantityUnit.KG, updated.unit)
        assertTrue(updated.isChecked)
    }

    @Test
    fun deleteList_cascadesToItems() = runTest {
        val listId = listDao.insert(activeList("Super"))
        itemDao.insert(item(listId, "Pan"))
        itemDao.insert(item(listId, "Manteca"))
        listDao.deleteById(listId)
        assertTrue(itemDao.getByListId(listId).first().isEmpty())
    }

    @Test
    fun deleteProduct_setsNullOnItemProductId() = runTest {
        val productId = productDao.insert(ProductEntity(name = "Yerba"))
        val listId = listDao.insert(activeList("Super"))
        val itemId = itemDao.insert(item(listId, "Yerba", productId = productId))

        val product = productDao.getById(productId)!!
        productDao.delete(product)

        val savedItem = itemDao.getById(itemId)!!
        assertNull(savedItem.productId)
        assertEquals("Yerba", savedItem.customName)
    }

    @Test
    fun multipleUnits_storedAndRestoredCorrectly() = runTest {
        val listId = listDao.insert(activeList("Super"))
        QuantityUnit.entries.forEachIndexed { i, unit ->
            itemDao.insert(ShoppingListItemEntity(
                listId = listId,
                customName = "item$i",
                quantity = 1.0,
                unit = unit
            ))
        }
        val items = itemDao.getByListId(listId).first()
        assertEquals(QuantityUnit.entries.size, items.size)
        items.forEachIndexed { i, it ->
            assertEquals(QuantityUnit.entries[i], it.unit)
        }
    }
}
