package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String, // "Home", "Car", "Shop", "Extra"
    val month: Int, // 1 to 12 (Jan is 1, Dec is 12)
    val day: Int, // 1 to 31
    val description: String,
    val familyMember: String, // "Dad", "Mom", "Son", "Daughter"
    val timestamp: Long = System.currentTimeMillis()
)
