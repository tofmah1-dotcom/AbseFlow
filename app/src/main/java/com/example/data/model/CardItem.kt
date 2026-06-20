package com.example.data.model

import java.util.UUID

data class CardItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val balance: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)
