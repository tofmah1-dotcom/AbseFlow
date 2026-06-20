package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.data.model.Transaction
import com.example.data.model.CardItem
import com.example.data.repository.TransactionRepository
import com.example.data.repository.FirebaseManager
import com.example.data.repository.UserProfile
import com.example.data.repository.FamilyMemberProfile
import com.example.data.repository.NestedFinanceCategory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(private val repository: TransactionRepository) : ViewModel() {

    // Auth state: Gated onboarding/login
    val isLoggedIn = MutableStateFlow(false)
    val userName = MutableStateFlow("")
    val userEmail = MutableStateFlow("")
    val isGoogleUser = MutableStateFlow(false)
    val isAdminMode = MutableStateFlow(false)

    // Admin Guard state
    val showAdminUnlockDialog = MutableStateFlow(false)
    val adminPasswordError = MutableStateFlow<String?>(null)

    // Firebase flow state integration
    val isFirebaseLoading = MutableStateFlow(false)
    val firebaseError = MutableStateFlow<String?>(null)
    val firestoreCards: StateFlow<List<CardItem>> = FirebaseManager.firestoreCards
    val currentUserState: StateFlow<UserProfile?> = FirebaseManager.currentUserState
    val firestoreFamilyMembers: StateFlow<List<FamilyMemberProfile>> = FirebaseManager.firestoreFamilyMembers
    val firestoreCategories: StateFlow<List<NestedFinanceCategory>> = FirebaseManager.firestoreCategories
    val isRealFirebase = FirebaseManager.isRealFirebaseActive

    // Selection filters
    val selectedMonth = MutableStateFlow(6) // Default to June (from current date metadata: June 2026)
    val selectedFamilyMember = MutableStateFlow("All") // Switcher filter: "All", "Dad", "Mom", "Son", "Daughter"

    // Toast/Feedback events
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
        viewModelScope.launch {
            FirebaseManager.currentUserState.collect { profile ->
                if (profile != null) {
                    userName.value = profile.displayName
                    userEmail.value = profile.email
                    isLoggedIn.value = true
                    isAdminMode.value = FirebaseManager.customClaimsAdmin.value
                } else {
                    userName.value = ""
                    userEmail.value = ""
                    isLoggedIn.value = false
                    isAdminMode.value = false
                }
            }
        }
        viewModelScope.launch {
            FirebaseManager.customClaimsAdmin.collect { isAdmin ->
                if (FirebaseManager.currentUserState.value != null) {
                    isAdminMode.value = isAdmin
                }
            }
        }
    }

    // All transaction data from Repository
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive State Model for Dashboard Analytics
    val dashboardState: StateFlow<DashboardUiState> = combine(
        allTransactions,
        selectedMonth,
        selectedFamilyMember
    ) { transactions, month, member ->
        
        // 1. Filtered by criteria
        val filtered = transactions.filter { tx ->
            val monthMatch = tx.month == month
            val memberMatch = member == "All" || tx.familyMember == member
            monthMatch && memberMatch
        }

        // 2. Calculations
        var totalIncome = 0.0
        var totalExpense = 0.0
        
        var homeExpense = 0.0
        var carExpense = 0.0
        var shopExpense = 0.0
        var extraExpense = 0.0

        var homeIncome = 0.0
        var carIncome = 0.0
        var shopIncome = 0.0
        var extraIncome = 0.0

        for (tx in filtered) {
            val amt = tx.amount
            if (tx.type == "INCOME") {
                totalIncome += amt
                when (tx.category) {
                    "Home" -> homeIncome += amt
                    "Car" -> carIncome += amt
                    "Shop" -> shopIncome += amt
                    "Extra" -> extraIncome += amt
                }
            } else {
                totalExpense += amt
                when (tx.category) {
                    "Home" -> homeExpense += amt
                    "Car" -> carExpense += amt
                    "Shop" -> shopExpense += amt
                    "Extra" -> extraExpense += amt
                }
            }
        }

        // 3. Time Series: Accumulate Income and Expenses monthly for charts
        val monthlyIncomes = DoubleArray(12) { 0.0 }
        val monthlyExpenses = DoubleArray(12) { 0.0 }
        
        for (tx in transactions) {
            // Apply family member filter globally to the timeseries if a member is active
            if (member == "All" || tx.familyMember == member) {
                val index = tx.month - 1
                if (index in 0..11) {
                    if (tx.type == "INCOME") {
                        monthlyIncomes[index] += tx.amount
                    } else {
                        monthlyExpenses[index] += tx.amount
                    }
                }
            }
        }

        DashboardUiState(
            month = month,
            familyMember = member,
            transactions = filtered,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netSavings = totalIncome - totalExpense,
            homeExpense = homeExpense,
            carExpense = carExpense,
            shopExpense = shopExpense,
            extraExpense = extraExpense,
            monthlyIncomes = monthlyIncomes.toList(),
            monthlyExpenses = monthlyExpenses.toList()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    // User Operations
    fun performLogin(name: String, email: String, googleAuth: Boolean) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank()) {
                _uiEvents.emit("Email and name fields are mandatory.")
                return@launch
            }
            userName.value = name
            userEmail.value = email
            isGoogleUser.value = googleAuth
            isLoggedIn.value = true
            _uiEvents.emit("Welcome, $name!")
        }
    }

    fun performLogout() {
        viewModelScope.launch {
            userName.value = ""
            userEmail.value = ""
            isGoogleUser.value = false
            isAdminMode.value = false
            isLoggedIn.value = false
            _uiEvents.emit("Success: Logged out.")
        }
    }

    // --- Core Firebase Auth & Firestore CRUD methods ---
    fun registerWithFirebase(context: Context, email: String, pass: String, name: String) {
        viewModelScope.launch {
            isFirebaseLoading.value = true
            firebaseError.value = null
            val result = FirebaseManager.signUp(context, email, pass, name)
            isFirebaseLoading.value = false
            if (result.isSuccess) {
                val profile = result.getOrThrow()
                userName.value = profile.displayName
                userEmail.value = profile.email
                isLoggedIn.value = true
                isAdminMode.value = (profile.role == "admin")
                _uiEvents.emit("Sign-up successful! Saved on Firestore `/users/${profile.uid}`")
            } else {
                firebaseError.value = result.exceptionOrNull()?.message ?: "Sign-up failed."
                _uiEvents.emit("Error: ${firebaseError.value}")
            }
        }
    }

    fun loginWithFirebase(context: Context, email: String, pass: String) {
        viewModelScope.launch {
            isFirebaseLoading.value = true
            firebaseError.value = null
            val result = FirebaseManager.signIn(context, email, pass)
            isFirebaseLoading.value = false
            if (result.isSuccess) {
                val profile = result.getOrThrow()
                userName.value = profile.displayName
                userEmail.value = profile.email
                isLoggedIn.value = true
                isAdminMode.value = FirebaseManager.customClaimsAdmin.value
                _uiEvents.emit("Access Granted! Logged in as ${profile.displayName} (${profile.role})")
            } else {
                firebaseError.value = result.exceptionOrNull()?.message ?: "Login failed."
                _uiEvents.emit("Error: ${firebaseError.value}")
            }
        }
    }

    fun logoutFromFirebase() {
        FirebaseManager.logout()
        userName.value = ""
        userEmail.value = ""
        isLoggedIn.value = false
        isAdminMode.value = false
        viewModelScope.launch {
            _uiEvents.emit("Success: Logged out securely.")
        }
    }

    fun toggleCustomRoleClaimOnState(context: Context, targetUid: String, makeAdmin: Boolean) {
        viewModelScope.launch {
            isFirebaseLoading.value = true
            val result = FirebaseManager.toggleAdminClaimSimulated(context, targetUid, makeAdmin)
            isFirebaseLoading.value = false
            if (result.isSuccess) {
                val profile = result.getOrThrow()
                val current = FirebaseManager.currentUserState.value
                if (current != null && current.uid == targetUid) {
                    isAdminMode.value = makeAdmin
                }
                _uiEvents.emit("Claims updated! Custom claim { admin: ${makeAdmin} } is now active.")
            } else {
                _uiEvents.emit("Claims update failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun createFirestoreCard(context: Context, title: String, desc: String, bal: Double) {
        viewModelScope.launch {
            isFirebaseLoading.value = true
            val result = FirebaseManager.createCard(context, title, desc, bal)
            isFirebaseLoading.value = false
            if (result.isSuccess) {
                _uiEvents.emit("Card successfully inserted in `/cards` collection.")
            } else {
                _uiEvents.emit("Access Denied: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun updateFirestoreCard(context: Context, card: CardItem) {
        viewModelScope.launch {
            isFirebaseLoading.value = true
            val result = FirebaseManager.updateCard(context, card)
            isFirebaseLoading.value = false
            if (result.isSuccess) {
                _uiEvents.emit("Card document successfully updated.")
            } else {
                _uiEvents.emit("Access Denied: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun deleteFirestoreCard(context: Context, cardId: String) {
        viewModelScope.launch {
            isFirebaseLoading.value = true
            val result = FirebaseManager.deleteCard(context, cardId)
            isFirebaseLoading.value = false
            if (result.isSuccess) {
                _uiEvents.emit("Card document deleted.")
            } else {
                _uiEvents.emit("Access Denied: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun handleAdminUnlock(pass: String) {
        viewModelScope.launch {
            if (pass == "admin123" || pass == "8888") {
                isAdminMode.value = true
                showAdminUnlockDialog.value = false
                adminPasswordError.value = null
                _uiEvents.emit("Admin Privileges Unlocked.")
            } else {
                adminPasswordError.value = "Invalid Authorization Passcode."
            }
        }
    }

    fun revokeAdminMode() {
        viewModelScope.launch {
            isAdminMode.value = false
            _uiEvents.emit("Admin Mode Deactivated.")
        }
    }

    // Transactions API
    fun addTransaction(
        amount: Double,
        type: String,
        category: String,
        day: Int,
        description: String,
        familyMember: String
    ) {
        viewModelScope.launch {
            if (amount <= 0) {
                _uiEvents.emit("Amount must be greater than zero.")
                return@launch
            }
            val tx = Transaction(
                amount = amount,
                type = type,
                category = category,
                month = selectedMonth.value,
                day = day.coerceIn(1, 31),
                description = description.ifBlank { "$category Transaction" },
                familyMember = familyMember
            )
            val generatedId = repository.insertTransaction(tx)
            val finalTx = tx.copy(id = generatedId)
            if (FirebaseManager.isRealFirebaseActive) {
                FirebaseManager.saveTransactionToFirestore(finalTx)
            }
            _uiEvents.emit("Transaction recorded successfully!")
        }
    }

    fun addTransactionManual(tx: Transaction) {
        viewModelScope.launch {
            if (tx.amount <= 0) {
                _uiEvents.emit("Amount must be greater than zero.")
                return@launch
            }
            val generatedId = repository.insertTransaction(tx)
            val finalTx = tx.copy(id = generatedId)
            if (FirebaseManager.isRealFirebaseActive) {
                FirebaseManager.saveTransactionToFirestore(finalTx)
            }
            _uiEvents.emit("Transaction posted.")
        }
    }

    fun updateTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(tx)
            if (FirebaseManager.isRealFirebaseActive) {
                FirebaseManager.saveTransactionToFirestore(tx)
            }
            _uiEvents.emit("Transaction modified.")
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            if (FirebaseManager.isRealFirebaseActive) {
                FirebaseManager.deleteTransactionFromFirestore(tx.id)
            }
            _uiEvents.emit("Transaction deleted.")
        }
    }

    fun adjustFinancialValuesGlobally(percentage: Double) {
        viewModelScope.launch {
            repository.adjustValuesGlobally(percentage)
            _uiEvents.emit("Global scale adjusted by ${if (percentage > 0) "+" else ""}$percentage%")
        }
    }

    fun triggerForwardSummaryReport(month: Int): String {
        val state = dashboardState.value
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val mName = monthNames.getOrNull(month - 1) ?: "Month $month"
        
        val summary = """
            === ABSEFLOW FINANCIAL REPORT ===
            Period: $mName 2026
            Group Filter: Family Switcher Status (${state.familyMember})
            
            Financial Cashflows Overview:
            - Total Income: $${"%.2f".format(state.totalIncome)}
            - Total Family Expenses: $${"%.2f".format(state.totalExpense)}
            - Savings Net Balance: $${"%.2f".format(state.netSavings)}
            
            Expense Allocation By Category:
            - Home & Utilities: $${"%.2f".format(state.homeExpense)}
            - Automotive & Fuel: $${"%.2f".format(state.carExpense)}
            - Shopping & Retail: $${"%.2f".format(state.shopExpense)}
            - Entertainment & Extras: $${"%.2f".format(state.extraExpense)}
            
            Generated securely by AbseFlow Family App.
        """.trimIndent()
        
        return summary
    }
}

// UI State Model representing calculations for Dashboard view
data class DashboardUiState(
    val month: Int = 6,
    val familyMember: String = "All",
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val homeExpense: Double = 0.0,
    val carExpense: Double = 0.0,
    val shopExpense: Double = 0.0,
    val extraExpense: Double = 0.0,
    val monthlyIncomes: List<Double> = List(12) { 0.0 },
    val monthlyExpenses: List<Double> = List(12) { 0.0 }
)
