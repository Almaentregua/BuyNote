package com.martinjm.buynote.domain.model

data class ShoppingList(
    val id: Long = 0,
    val name: String,
    val status: ListStatus,
    val createdAt: Long,
    val completedAt: Long? = null
)
