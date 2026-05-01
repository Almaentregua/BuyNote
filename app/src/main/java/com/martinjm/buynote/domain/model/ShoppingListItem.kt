package com.martinjm.buynote.domain.model

data class ShoppingListItem(
    val id: Long = 0,
    val listId: Long,
    val productId: Long? = null,
    val customName: String? = null,
    val quantity: Double,
    val unit: QuantityUnit,
    val isChecked: Boolean = false
)
