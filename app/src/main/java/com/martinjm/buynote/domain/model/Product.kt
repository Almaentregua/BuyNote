package com.martinjm.buynote.domain.model

data class Product(
    val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val categoryId: Long? = null,
    val notes: String? = null
)
