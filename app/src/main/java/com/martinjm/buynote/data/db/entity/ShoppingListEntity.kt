package com.martinjm.buynote.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.martinjm.buynote.domain.model.ListStatus

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val status: ListStatus,
    val createdAt: Long,
    val completedAt: Long? = null
)
