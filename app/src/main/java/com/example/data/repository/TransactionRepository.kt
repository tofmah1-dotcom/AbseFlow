package com.example.data.repository

import com.example.data.db.TransactionDao
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.random.Random

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsForMonth(month: Int): Flow<List<Transaction>> {
        return transactionDao.getTransactionsForMonth(month)
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun adjustValuesGlobally(percentage: Double) {
        // Multiplier: e.g., 5% increase -> multiplier = 1.05. -10% decrease -> multiplier = 0.90
        val multiplier = 1.0 + (percentage / 100.0)
        transactionDao.adjustValuesGlobally(multiplier)
    }

    suspend fun seedDatabaseIfEmpty() {
        val currentList = transactionDao.getAllTransactions().first()
        if (currentList.isEmpty()) {
            val categories = listOf("Home", "Car", "Shop", "Extra")
            val familyMembers = listOf("Dad", "Mom", "Son", "Daughter")
            
            // Seed data for all 12 months to make time series beautiful!
            for (month in 1..12) {
                // Incomes
                transactionDao.insertTransaction(
                    Transaction(
                        amount = 4500.0 + Random.nextInt(-200, 300),
                        type = "INCOME",
                        category = "Extra",
                        month = month,
                        day = 1,
                        description = "Monthly Primary Salary",
                        familyMember = "Dad"
                    )
                )

                transactionDao.insertTransaction(
                    Transaction(
                        amount = 1800.0 + Random.nextInt(-100, 400),
                        type = "INCOME",
                        category = "Shop",
                        month = month,
                        day = 10,
                        description = "Freelance Consulting",
                        familyMember = "Mom"
                    )
                )

                // Expenses: Home
                transactionDao.insertTransaction(
                    Transaction(
                        amount = 1200.0 + Random.nextInt(-50, 50),
                        type = "EXPENSE",
                        category = "Home",
                        month = month,
                        day = 2,
                        description = "Rent & Utility Bills",
                        familyMember = "Mom"
                    )
                )
                
                // Expenses: Car
                transactionDao.insertTransaction(
                    Transaction(
                        amount = 350.0 + Random.nextInt(-30, 80),
                        type = "EXPENSE",
                        category = "Car",
                        month = month,
                        day = 15,
                        description = "Vehicle Loan & Fuel",
                        familyMember = "Dad"
                    )
                )

                // Expenses: Shop
                transactionDao.insertTransaction(
                    Transaction(
                        amount = 500.0 + Random.nextInt(-80, 150),
                        type = "EXPENSE",
                        category = "Shop",
                        month = month,
                        day = 8,
                        description = "Weekly Family Groceries",
                        familyMember = "Mom"
                    )
                )
                transactionDao.insertTransaction(
                    Transaction(
                        amount = 125.0 + Random.nextInt(-20, 60),
                        type = "EXPENSE",
                        category = "Shop",
                        month = month,
                        day = 24,
                        description = "Apparel Shopping",
                        familyMember = "Daughter"
                    )
                )

                // Expenses: Extra
                transactionDao.insertTransaction(
                    Transaction(
                        amount = 180.0 + Random.nextInt(-40, 120),
                        type = "EXPENSE",
                        category = "Extra",
                        month = month,
                        day = 20,
                        description = "Family Dinner & Movies",
                        familyMember = "Son"
                    )
                )
                
                if (month % 2 == 0) {
                    transactionDao.insertTransaction(
                        Transaction(
                            amount = 90.0 + Random.nextInt(-20, 50),
                            type = "EXPENSE",
                            category = "Extra",
                            month = month,
                            day = 18,
                            description = "Online Courses & Books",
                            familyMember = "Son"
                        )
                    )
                }
            }
        }
    }
}
