package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.CardItem
import com.example.data.model.Transaction
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "user",
    val createdAt: Long = System.currentTimeMillis()
)

data class FamilyMemberProfile(
    val id: String = "",
    val name: String = "",
    val relationship: String = "",
    val iconName: String = "",
    val accentColorHex: String = "",
    val monthlyLimit: Double = 5000.0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "relationship" to relationship,
            "iconName" to iconName,
            "accentColorHex" to accentColorHex,
            "monthlyLimit" to monthlyLimit,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): FamilyMemberProfile {
            return FamilyMemberProfile(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                relationship = map["relationship"] as? String ?: "",
                iconName = map["iconName"] as? String ?: "",
                accentColorHex = map["accentColorHex"] as? String ?: "",
                monthlyLimit = (map["monthlyLimit"] as? Number)?.toDouble() ?: 5000.0,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class NestedFinanceCategory(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val parentId: String? = null,
    val allocation: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "parentId" to parentId,
            "allocation" to allocation,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): NestedFinanceCategory {
            return NestedFinanceCategory(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                description = map["description"] as? String ?: "",
                parentId = map["parentId"] as? String,
                allocation = (map["allocation"] as? Number)?.toDouble() ?: 0.0,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    
    // Engine states
    var isRealFirebaseActive = false
        private set
        
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    
    // Shared Preferences for local sandbox persistence
    private const val PREFS_NAME = "AbseFlowFirebaseSandbox"
    
    // Simulated Firestore collections
    private val localUsers = mutableMapOf<String, UserProfile>()
    private val localCards = mutableMapOf<String, CardItem>()
    
    // Admin Custom Claims state for the active user
    val customClaimsAdmin = MutableStateFlow(false)
    
    // Current user flow
    private val _currentUserState = MutableStateFlow<UserProfile?>(null)
    val currentUserState: StateFlow<UserProfile?> = _currentUserState
    
    // Cards list flow
    private val _firestoreCards = MutableStateFlow<List<CardItem>>(emptyList())
    val firestoreCards: StateFlow<List<CardItem>> = _firestoreCards

    // Family profiles flow
    private val _firestoreFamilyMembers = MutableStateFlow<List<FamilyMemberProfile>>(emptyList())
    val firestoreFamilyMembers: StateFlow<List<FamilyMemberProfile>> = _firestoreFamilyMembers

    // Nested categories flow
    private val _firestoreCategories = MutableStateFlow<List<NestedFinanceCategory>>(emptyList())
    val firestoreCategories: StateFlow<List<NestedFinanceCategory>> = _firestoreCategories

    fun initialize(context: Context) {
        try {
            // First try default initialization (from google-services.json)
            FirebaseApp.initializeApp(context)
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            isRealFirebaseActive = true
            Log.d(TAG, "Firebase successfully initiated using default google-services configuration!")
        } catch (e: Exception) {
            Log.w(TAG, "Default Firebase initialization failed, trying dynamic fallback options: ${e.message}")
            try {
                // If it fails (e.g. google-services.json missing), try dynamic options with standard values if applicable,
                // or fall back cleanly to Local Sandbox mode on-device for a zero-crash, 100% interactive runtime.
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:864416446824:web:d7a87b01f0cba84fd03edd")
                    .setProjectId("abseflow-d72bb")
                    .setApiKey("AIzaSyAjmNkX2dSne4kkb9wZDihzqPSXA_OUSl8")
                    .setGcmSenderId("864416446824")
                    .setStorageBucket("abseflow-d72bb.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(context, options)
                auth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()
                isRealFirebaseActive = true
                Log.d(TAG, "Firebase successfully initiated using dynamic options fallback!")
            } catch (ex: Exception) {
                Log.e(TAG, "Firebase initialization failed globally. Fallback to Local Persistent Sandbox engine.")
                isRealFirebaseActive = false
            }
        }
        
        loadLocalDatabase(context)
        observeAuthChanges(context)
    }
    
    private fun loadLocalDatabase(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Load simulated Users
        val usersJsonString = prefs.getString("users", "{}") ?: "{}"
        try {
            // Very simple custom map parser to avoid dependency issues in basic configurations
            // Schema: uid|email|displayName|role|createdAt;;;
            if (usersJsonString.isNotBlank() && usersJsonString != "{}") {
                usersJsonString.split(";;;").forEach { item ->
                    val parts = item.split("|")
                    if (parts.size >= 5) {
                        val user = UserProfile(
                            uid = parts[0],
                            email = parts[1],
                            displayName = parts[2],
                            role = parts[3],
                            createdAt = parts[4].toLongOrNull() ?: System.currentTimeMillis()
                        )
                        localUsers[user.uid] = user
                    }
                }
            }
            // Seed a default mock admin and a regular user so players can explore instantly
            if (localUsers.isEmpty()) {
                val demoAdmin = UserProfile("admin_uid", "admin@abseflow.com", "Admin Demo", "admin", System.currentTimeMillis() - 86400000)
                val demoUser = UserProfile("user_uid", "user@abseflow.com", "Regular Family", "user", System.currentTimeMillis() - 86400000 * 2)
                localUsers[demoAdmin.uid] = demoAdmin
                localUsers[demoUser.uid] = demoUser
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing saved users: ${e.message}")
        }
        
        // Load simulated Cards
        val cardsJsonString = prefs.getString("cards", "") ?: ""
        try {
            if (cardsJsonString.isNotBlank()) {
                cardsJsonString.split(";;;").forEach { item ->
                    val parts = item.split("|")
                    if (parts.size >= 4) {
                        val card = CardItem(
                            id = parts[0],
                            title = parts[1],
                            description = parts[2],
                            balance = parts[3].toDoubleOrNull() ?: 0.0,
                            updatedAt = parts[4].toLongOrNull() ?: System.currentTimeMillis()
                        )
                        localCards[card.id] = card
                    }
                }
            }
            
            // Seed a couple Cards if empty
            if (localCards.isEmpty()) {
                val creditCard = CardItem(UUID.randomUUID().toString(), "Family Prime Card", "Primary Platinum Rewards", 4850.50, System.currentTimeMillis())
                val debitCard = CardItem(UUID.randomUUID().toString(), "Emergency Backup", "Savings Reserve Vault", 12450.00, System.currentTimeMillis())
                localCards[creditCard.id] = creditCard
                localCards[debitCard.id] = debitCard
                saveCardsToPrefs(context)
            }
            _firestoreCards.value = localCards.values.toList().sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing saved cards: ${e.message}")
        }

        // Load simulated Family Members
        val membersJsonString = prefs.getString("family_members", "") ?: ""
        try {
            if (membersJsonString.isNotBlank()) {
                val list = membersJsonString.split(";;;").mapNotNull { item ->
                    try {
                        val parts = item.split("|")
                        FamilyMemberProfile(
                            id = parts[0],
                            name = parts[1],
                            relationship = parts[2],
                            iconName = parts[3],
                            accentColorHex = parts[4],
                            monthlyLimit = parts[5].toDoubleOrNull() ?: 5000.0,
                            createdAt = parts[6].toLongOrNull() ?: System.currentTimeMillis()
                        )
                    } catch (ex: Exception) {
                        null
                    }
                }
                _firestoreFamilyMembers.value = list
            } else {
                // Seed
                val defaults = listOf(
                    FamilyMemberProfile("Dad", "Robert", "Father", "Male", "#1769AA"),
                    FamilyMemberProfile("Mom", "Sarah", "Mother", "Female", "#E57373"),
                    FamilyMemberProfile("Son", "Leo", "Child", "Face", "#81C784"),
                    FamilyMemberProfile("Daughter", "Mia", "Child", "ChildCare", "#FFB74D")
                )
                _firestoreFamilyMembers.value = defaults
                saveFamilyMembersToPrefs(context, defaults)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing saved family members: ${e.message}")
        }

        // Load simulated Categories
        val categoriesJsonString = prefs.getString("nested_categories", "") ?: ""
        try {
            if (categoriesJsonString.isNotBlank()) {
                val list = categoriesJsonString.split(";;;").mapNotNull { item ->
                    try {
                        val parts = item.split("|")
                        NestedFinanceCategory(
                            id = parts[0],
                            name = parts[1],
                            description = parts[2],
                            parentId = if (parts[3] != "null") parts[3] else null,
                            allocation = parts[4].toDoubleOrNull() ?: 0.0,
                            createdAt = parts[5].toLongOrNull() ?: System.currentTimeMillis()
                        )
                    } catch (ex: Exception) {
                        null
                    }
                }
                _firestoreCategories.value = list
            } else {
                val defaults = listOf(
                    NestedFinanceCategory("Home", "Home", "Home & Utilities", null, 2500.0),
                    NestedFinanceCategory("Car", "Car", "Automotive & Transport", null, 1000.0),
                    NestedFinanceCategory("Shop", "Shop", "Groceries & Retail", null, 1500.0),
                    NestedFinanceCategory("Extra", "Extra", "Leisure & Entertainment", null, 800.0),
                    NestedFinanceCategory("Rent", "Rent", "Monthly Rent", "Home", 1500.0),
                    NestedFinanceCategory("Utilities", "Utilities", "Utilities", "Home", 500.0),
                    NestedFinanceCategory("Maintenance", "Maintenance", "Home Repairs", "Home", 500.0),
                    NestedFinanceCategory("Fuel", "Fuel", "Fuel & Gas", "Car", 400.0),
                    NestedFinanceCategory("Insurance", "Insurance", "Vehicle Insurance", "Car", 300.0),
                    NestedFinanceCategory("Repair", "Repair", "Vehicle Repairs", "Car", 300.0),
                    NestedFinanceCategory("Groceries", "Groceries", "Weekly Groceries", "Shop", 1000.0),
                    NestedFinanceCategory("Clothing", "Clothing", "Apparel & Shopping", "Shop", 500.0),
                    NestedFinanceCategory("DiningOut", "Dining Out", "Restaurants", "Extra", 300.0),
                    NestedFinanceCategory("Entertainment", "Entertainment", "Movies & Fun", "Extra", 300.0),
                    NestedFinanceCategory("Education", "Education", "Courses & Books", "Extra", 200.0)
                )
                _firestoreCategories.value = defaults
                saveCategoriesToPrefs(context, defaults)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing saved categories: ${e.message}")
        }
    }
    
    private fun saveUsersToPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val data = localUsers.values.joinToString(";;;") { 
            "${it.uid}|${it.email}|${it.displayName}|${it.role}|${it.createdAt}"
        }
        prefs.edit().putString("users", data).apply()
    }
    
    private fun saveCardsToPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val data = localCards.values.joinToString(";;;") { 
            "${it.id}|${it.title}|${it.description}|${it.balance}|${it.updatedAt}"
        }
        prefs.edit().putString("cards", data).apply()
        _firestoreCards.value = localCards.values.toList().sortedByDescending { it.updatedAt }
    }

    private fun saveFamilyMembersToPrefs(context: Context, list: List<FamilyMemberProfile>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val data = list.joinToString(";;;") {
            "${it.id}|${it.name}|${it.relationship}|${it.iconName}|${it.accentColorHex}|${it.monthlyLimit}|${it.createdAt}"
        }
        prefs.edit().putString("family_members", data).apply()
        _firestoreFamilyMembers.value = list
    }

    private fun saveCategoriesToPrefs(context: Context, list: List<NestedFinanceCategory>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val data = list.joinToString(";;;") {
            "${it.id}|${it.name}|${it.description}|${it.parentId ?: "null"}|${it.allocation}|${it.createdAt}"
        }
        prefs.edit().putString("nested_categories", data).apply()
        _firestoreCategories.value = list
    }
    
    private fun observeAuthChanges(context: Context) {
        if (isRealFirebaseActive && auth != null) {
            auth!!.addAuthStateListener { firebaseAuth ->
                val fbUser = firebaseAuth.currentUser
                if (fbUser != null) {
                    // Check and map user
                    val uid = fbUser.uid
                    val email = fbUser.email ?: ""
                    val disp = fbUser.displayName ?: "Family Member"
                    
                    // Fetch profile details from real firestore asynchronously
                    firestore?.collection("users")?.document(uid)?.get()
                        ?.addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                val role = doc.getString("role") ?: "user"
                                val profile = UserProfile(uid, email, disp, role, doc.getLong("createdAt") ?: System.currentTimeMillis())
                                _currentUserState.value = profile
                                customClaimsAdmin.value = (role == "admin")
                            } else {
                                // Create default
                                val profile = UserProfile(uid, email, disp, "user", System.currentTimeMillis())
                                _currentUserState.value = profile
                                customClaimsAdmin.value = false
                                // Write to firestore
                                firestore?.collection("users")?.document(uid)?.set(profile)
                            }
                        }
                        ?.addOnFailureListener {
                            // Offline/Failed fallback
                            val profile = UserProfile(uid, email, disp, "user", System.currentTimeMillis())
                            _currentUserState.value = profile
                            customClaimsAdmin.value = false
                        }
                    
                    // Also query real ID token metadata for custom claims verification
                    fbUser.getIdToken(false).addOnSuccessListener { result ->
                        val adminClaimFetched = result.claims["admin"] as? Boolean ?: false
                        if (adminClaimFetched) {
                            customClaimsAdmin.value = true
                        }
                    }
                    
                    // Fetch real cards, profiles, categories
                    observeRealCards()
                    observeRealFamilyMembers()
                    observeRealCategories()
                } else {
                    _currentUserState.value = null
                    customClaimsAdmin.value = false
                    _firestoreCards.value = emptyList()
                    _firestoreFamilyMembers.value = emptyList()
                    _firestoreCategories.value = emptyList()
                }
            }
        }
    }
    
    private fun observeRealCards() {
        if (isRealFirebaseActive && firestore != null) {
            firestore!!.collection("cards")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "Listen failed for cards.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val cards = snapshot.documents.mapNotNull { doc ->
                            try {
                                CardItem(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    description = doc.getString("description") ?: "",
                                    balance = doc.getDouble("balance") ?: 0.0,
                                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                                )
                            } catch (ex: Exception) {
                                null
                            }
                        }
                        _firestoreCards.value = cards.sortedByDescending { it.updatedAt }
                    }
                }
        }
    }

    private fun observeRealFamilyMembers() {
        if (isRealFirebaseActive && firestore != null) {
            firestore!!.collection("family_members")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "Listen failed for family_members.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        if (snapshot.isEmpty) {
                            seedDefaultFamilyMembersFirestore()
                        } else {
                            val list = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val data = doc.data ?: return@mapNotNull null
                                    FamilyMemberProfile.fromMap(data)
                                } catch (ex: Exception) {
                                    null
                                }
                            }
                            _firestoreFamilyMembers.value = list.sortedBy { it.id }
                        }
                    }
                }
        }
    }

    private fun seedDefaultFamilyMembersFirestore() {
        val firestore = firestore ?: return
        val defaults = listOf(
            FamilyMemberProfile("Dad", "Robert", "Father", "Male", "#1769AA"),
            FamilyMemberProfile("Mom", "Sarah", "Mother", "Female", "#E57373"),
            FamilyMemberProfile("Son", "Leo", "Child", "Face", "#81C784"),
            FamilyMemberProfile("Daughter", "Mia", "Child", "ChildCare", "#FFB74D")
        )
        defaults.forEach { member ->
            firestore.collection("family_members").document(member.id).set(member.toMap())
        }
    }

    private fun observeRealCategories() {
        if (isRealFirebaseActive && firestore != null) {
            firestore!!.collection("nested_categories")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "Listen failed for nested_categories.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        if (snapshot.isEmpty) {
                            seedDefaultCategoriesFirestore()
                        } else {
                            val list = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val data = doc.data ?: return@mapNotNull null
                                    NestedFinanceCategory.fromMap(data)
                                } catch (ex: Exception) {
                                    null
                                }
                            }
                            _firestoreCategories.value = list
                        }
                    }
                }
        }
    }

    private fun seedDefaultCategoriesFirestore() {
        val firestore = firestore ?: return
        val defaults = listOf(
            NestedFinanceCategory("Home", "Home", "Home & Utilities", null, 2500.0),
            NestedFinanceCategory("Car", "Car", "Automotive & Transport", null, 1000.0),
            NestedFinanceCategory("Shop", "Shop", "Groceries & Retail", null, 1500.0),
            NestedFinanceCategory("Extra", "Extra", "Leisure & Entertainment", null, 800.0),
            NestedFinanceCategory("Rent", "Rent", "Monthly Rent", "Home", 1500.0),
            NestedFinanceCategory("Utilities", "Utilities", "Utilities", "Home", 500.0),
            NestedFinanceCategory("Maintenance", "Maintenance", "Home Repairs", "Home", 500.0),
            NestedFinanceCategory("Fuel", "Fuel", "Fuel & Gas", "Car", 400.0),
            NestedFinanceCategory("Insurance", "Insurance", "Vehicle Insurance", "Car", 300.0),
            NestedFinanceCategory("Repair", "Repair", "Vehicle Repairs", "Car", 300.0),
            NestedFinanceCategory("Groceries", "Groceries", "Weekly Groceries", "Shop", 1000.0),
            NestedFinanceCategory("Clothing", "Clothing", "Apparel & Shopping", "Shop", 500.0),
            NestedFinanceCategory("DiningOut", "Dining Out", "Restaurants", "Extra", 300.0),
            NestedFinanceCategory("Entertainment", "Entertainment", "Movies & Fun", "Extra", 300.0),
            NestedFinanceCategory("Education", "Education", "Courses & Books", "Extra", 200.0)
        )
        defaults.forEach { cat ->
            firestore.collection("nested_categories").document(cat.id).set(cat.toMap())
        }
    }

    // --- Dynamic Profile CRUD ---
    suspend fun createFamilyMember(context: Context, member: FamilyMemberProfile): Result<FamilyMemberProfile> {
        if (isRealFirebaseActive && firestore != null) {
            return try {
                firestore!!.collection("family_members").document(member.id).set(member.toMap()).await()
                Result.success(member)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            val current = _firestoreFamilyMembers.value.toMutableList()
            current.removeAll { it.id == member.id }
            current.add(member)
            saveFamilyMembersToPrefs(context, current)
            return Result.success(member)
        }
    }

    suspend fun deleteFamilyMember(context: Context, memberId: String): Result<Boolean> {
        if (isRealFirebaseActive && firestore != null) {
            return try {
                firestore!!.collection("family_members").document(memberId).delete().await()
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            val current = _firestoreFamilyMembers.value.toMutableList()
            current.removeAll { it.id == memberId }
            saveFamilyMembersToPrefs(context, current)
            return Result.success(true)
        }
    }

    // --- Dynamic Category CRUD ---
    suspend fun createNestedCategory(context: Context, category: NestedFinanceCategory): Result<NestedFinanceCategory> {
        if (isRealFirebaseActive && firestore != null) {
            return try {
                firestore!!.collection("nested_categories").document(category.id).set(category.toMap()).await()
                Result.success(category)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            val current = _firestoreCategories.value.toMutableList()
            current.removeAll { it.id == category.id }
            current.add(category)
            saveCategoriesToPrefs(context, current)
            return Result.success(category)
        }
    }

    suspend fun deleteNestedCategory(context: Context, categoryId: String): Result<Boolean> {
        if (isRealFirebaseActive && firestore != null) {
            return try {
                firestore!!.collection("nested_categories").document(categoryId).delete().await()
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            val current = _firestoreCategories.value.toMutableList()
            current.removeAll { it.id == categoryId }
            saveCategoriesToPrefs(context, current)
            return Result.success(true)
        }
    }

    // --- Cloud Transaction Sync ---
    suspend fun saveTransactionToFirestore(tx: Transaction): Result<Boolean> {
        val firestore = firestore ?: return Result.failure(Exception("Firestore not active"))
        return try {
            val txMap = mapOf(
                "id" to tx.id,
                "amount" to tx.amount,
                "type" to tx.type,
                "category" to tx.category,
                "month" to tx.month,
                "day" to tx.day,
                "description" to tx.description,
                "familyMember" to tx.familyMember,
                "timestamp" to tx.timestamp
            )
            firestore.collection("transactions").document(tx.id.toString()).set(txMap).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransactionFromFirestore(txId: Long): Result<Boolean> {
        val firestore = firestore ?: return Result.failure(Exception("Firestore not active"))
        return try {
            firestore.collection("transactions").document(txId.toString()).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(context: Context, email: String, pass: String, displayName: String): Result<UserProfile> {
        if (email.isBlank() || pass.isBlank() || displayName.isBlank()) {
            return Result.failure(Exception("All sign-up fields are required."))
        }
        
        if (isRealFirebaseActive && auth != null && firestore != null) {
            return try {
                val authResult = auth!!.createUserWithEmailAndPassword(email, pass).await()
                val user = authResult.user ?: throw Exception("Auth object creation failed.")
                
                val newProfile = UserProfile(
                    uid = user.uid,
                    email = email,
                    displayName = displayName,
                    role = "user", // Default Role of "user"
                    createdAt = System.currentTimeMillis()
                )
                
                // Save to Firestore
                firestore!!.collection("users").document(user.uid).set(newProfile).await()
                _currentUserState.value = newProfile
                customClaimsAdmin.value = false
                
                Result.success(newProfile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            // Local persistence lookup
            if (localUsers.values.any { it.email.equals(email, ignoreCase = true) }) {
                return Result.failure(Exception("User email already registered! Try logging in."))
            }
            val uid = "local_" + UUID.randomUUID().toString().take(8)
            val newProfile = UserProfile(uid, email, displayName, "user", System.currentTimeMillis())
            localUsers[uid] = newProfile
            saveUsersToPrefs(context)
            _currentUserState.value = newProfile
            customClaimsAdmin.value = false
            return Result.success(newProfile)
        }
    }

    suspend fun signIn(context: Context, email: String, pass: String): Result<UserProfile> {
        if (email.isBlank() || pass.isBlank()) {
            return Result.failure(Exception("Email and Password are required fields."))
        }
        
        if (isRealFirebaseActive && auth != null && firestore != null) {
            return try {
                val authResult = auth!!.signInWithEmailAndPassword(email, pass).await()
                val user = authResult.user ?: throw Exception("Auth signIn failed.")
                
                val doc = firestore!!.collection("users").document(user.uid).get().await()
                val profile = if (doc.exists()) {
                    UserProfile(
                        uid = user.uid,
                        email = email,
                        displayName = doc.getString("displayName") ?: user.displayName ?: "User",
                        role = doc.getString("role") ?: "user",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } else {
                    val fallback = UserProfile(user.uid, email, user.displayName ?: "User", "user", System.currentTimeMillis())
                    firestore!!.collection("users").document(user.uid).set(fallback).await()
                    fallback
                }
                
                _currentUserState.value = profile
                
                // Evaluate custom claims check
                val tokenResult = user.getIdToken(false).await()
                val hasAdminClaim = tokenResult.claims["admin"] as? Boolean ?: false
                customClaimsAdmin.value = hasAdminClaim || (profile.role == "admin")
                
                Result.success(profile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            // Local persistence check
            val existing = localUsers.values.find { it.email.equals(email, ignoreCase = true) }
                ?: return Result.failure(Exception("No account exists with this email address. Please sign up!"))
            
            _currentUserState.value = existing
            customClaimsAdmin.value = (existing.role == "admin")
            return Result.success(existing)
        }
    }

    fun logout() {
        if (isRealFirebaseActive && auth != null) {
            auth!!.signOut()
        }
        _currentUserState.value = null
        customClaimsAdmin.value = false
        _firestoreCards.value = emptyList()
    }
    
    // Simulate Custom Claims / Admin Upgrade via Cloud Function or SDK
    suspend fun toggleAdminClaimSimulated(context: Context, targetUid: String, enable: Boolean): Result<UserProfile> {
        if (isRealFirebaseActive && firestore != null) {
            return try {
                val roleStr = if (enable) "admin" else "user"
                firestore!!.collection("users").document(targetUid).update("role", roleStr).await()
                
                // Get fresh state
                val currentUserLoaded = _currentUserState.value
                if (currentUserLoaded != null && currentUserLoaded.uid == targetUid) {
                    val updatedProfile = currentUserLoaded.copy(role = roleStr)
                    _currentUserState.value = updatedProfile
                    customClaimsAdmin.value = enable
                }
                Result.success(_currentUserState.value ?: UserProfile())
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            val usr = localUsers[targetUid] ?: return Result.failure(Exception("Selected user id not found."))
            val updated = usr.copy(role = if (enable) "admin" else "user")
            localUsers[targetUid] = updated
            saveUsersToPrefs(context)
            
            val cur = _currentUserState.value
            if (cur != null && cur.uid == targetUid) {
                _currentUserState.value = updated
                customClaimsAdmin.value = enable
            }
            return Result.success(updated)
        }
    }
    
    // --- Card CRUD endpoints ---
    suspend fun createCard(context: Context, title: String, description: String, balance: Double): Result<CardItem> {
        // Enforce admin privileges gate
        if (!customClaimsAdmin.value) {
            return Result.failure(SecurityException("SECURITY_ERROR: Access denied. Administrative claims verification failed."))
        }
        
        val newCard = CardItem(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            balance = balance,
            updatedAt = System.currentTimeMillis()
        )
        
        if (isRealFirebaseActive && firestore != null) {
            return try {
                firestore!!.collection("cards").document(newCard.id).set(newCard).await()
                Result.success(newCard)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            localCards[newCard.id] = newCard
            saveCardsToPrefs(context)
            return Result.success(newCard)
        }
    }
    
    suspend fun updateCard(context: Context, card: CardItem): Result<CardItem> {
        // Enforce admin privileges gate
        if (!customClaimsAdmin.value) {
            return Result.failure(SecurityException("SECURITY_ERROR: Access denied. Admin token validation failed."))
        }
        
        val updatedCard = card.copy(updatedAt = System.currentTimeMillis())
        if (isRealFirebaseActive && firestore != null) {
            return try {
                firestore!!.collection("cards").document(updatedCard.id).set(updatedCard).await()
                Result.success(updatedCard)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            localCards[updatedCard.id] = updatedCard
            saveCardsToPrefs(context)
            return Result.success(updatedCard)
        }
    }
    
    suspend fun deleteCard(context: Context, cardId: String): Result<Boolean> {
        // Enforce admin privileges gate
        if (!customClaimsAdmin.value) {
            return Result.failure(SecurityException("SECURITY_ERROR: Access denied. Admin permissions missing."))
        }
        
        if (isRealFirebaseActive && firestore != null) {
            return try {
                firestore!!.collection("cards").document(cardId).delete().await()
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            localCards.remove(cardId)
            saveCardsToPrefs(context)
            return Result.success(true)
        }
    }
    
    // Quick helper to read all user emails registered (for the Claims Panel switcher)
    fun getAllRegisteredUsers(): List<UserProfile> {
        return localUsers.values.toList()
    }
}
