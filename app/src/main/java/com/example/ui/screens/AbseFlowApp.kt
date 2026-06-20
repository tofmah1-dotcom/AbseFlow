package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Transaction
import com.example.data.model.CardItem
import com.example.data.repository.FamilyMemberProfile
import com.example.data.repository.NestedFinanceCategory
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.DashboardUiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun AbseFlowApp(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Monitor ViewModel messages for toast feedback
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = isLoggedIn,
                transitionSpec = {
                    fadeIn(animationSpec = twistSpring()) togetherWith
                            fadeOut(animationSpec = twistSpring())
                },
                label = "onboard_switch"
            ) { logged ->
                if (!logged) {
                    OnboardingScreen(viewModel = viewModel)
                } else {
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}

private fun <T> twistSpring(): SpringSpec<T> = spring(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow
)

// Onboarding & Security Authentication Gate
@Composable
fun OnboardingScreen(
    viewModel: FinanceViewModel
) {
    val context = LocalContext.current
    var isSigningUp by remember { mutableStateOf(false) }
    
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val isLoader = viewModel.isFirebaseLoading.collectAsStateWithLifecycle()
    val errMessage = viewModel.firebaseError.collectAsStateWithLifecycle()
    val isRealClientInstance = viewModel.isRealFirebase
    
    // Form compliance validations
    val isEmailCompliant = remember(email) { email.contains("@") && email.contains(".") }
    val isPasswordCompliant = remember(password) { password.length >= 6 }
    val isNameCompliant = remember(name) { name.isNotBlank() }
    
    val isButtonEnabled = if (isSigningUp) {
        isEmailCompliant && isPasswordCompliant && isNameCompliant
    } else {
        isEmailCompliant && isPasswordCompliant
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBg, Color(0xFF0F152B))
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Connection Gateway metadata status badge
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRealClientInstance) Color(0xFF1B5E20).copy(alpha = 0.4f) else Color(0xFFE65100).copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, if (isRealClientInstance) Color(0xFF4CAF50) else Color(0xFFFF9800)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isRealClientInstance) Color(0xFF4CAF50) else Color(0xFFFF9800))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRealClientInstance) "Firebase Cloud Integration Active" else "Firestore Secured Sandbox Environment Ready",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Dynamic Header Visual Card
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF1E294B)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1326)),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 8.5f)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val imgRes = R.drawable.img_onboarding_hero_1781812433717
                    Image(
                        painter = painterResource(id = imgRes),
                        contentDescription = "AbseFlow Dashboard Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xE60A0E1A))
                                )
                            )
                    )
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "ABSEFLOW",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = PrimaryDark,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Next-Gen Family Financial Intelligence",
                            fontSize = 12.sp,
                            color = Color(0xFF90A4AE),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Segmented Tab Toggle for Auth Mode selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0B0F1E))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { isSigningUp = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isSigningUp) Color(0xFF1A1F38) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = "Access Account",
                        fontSize = 13.sp,
                        color = if (!isSigningUp) PrimaryDark else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = { isSigningUp = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSigningUp) Color(0xFF1A1F38) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = "Create Account",
                        fontSize = 13.sp,
                        color = if (isSigningUp) PrimaryDark else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Error banner panel
            if (errMessage.value != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3E1F21)),
                    border = BorderStroke(1.dp, Color(0xFFE57373)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFEF5350))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errMessage.value ?: "",
                            color = Color(0xFFFFCDD2),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Input Fields Layout
            if (isSigningUp) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Family Account Holder Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryDark) },
                    isError = name.isNotBlank() && !isNameCompliant,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E),
                        focusedLabelColor = PrimaryDark,
                        unfocusedLabelColor = Color(0xFF90A4AE)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboard_name")
                )
                if (name.isNotBlank() && !isNameCompliant) {
                    Text("Name cannot be empty", color = Color(0xFFE57373), fontSize = 11.sp, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Family Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryDark) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = email.isNotBlank() && !isEmailCompliant,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PrimaryDark,
                    unfocusedBorderColor = Color(0xFF2E3B5E),
                    focusedLabelColor = PrimaryDark,
                    unfocusedLabelColor = Color(0xFF90A4AE)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboard_email")
            )
            if (email.isNotBlank() && !isEmailCompliant) {
                Text("Enter a valid email (e.g. member@abseflow.com)", color = Color(0xFFE57373), fontSize = 11.sp, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Access Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryDark) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = password.isNotBlank() && !isPasswordCompliant,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PrimaryDark,
                    unfocusedBorderColor = Color(0xFF2E3B5E),
                    focusedLabelColor = PrimaryDark,
                    unfocusedLabelColor = Color(0xFF90A4AE)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboard_password")
            )
            if (password.isNotBlank() && !isPasswordCompliant) {
                Text("Password must contain at least 6 characters", color = Color(0xFFE57373), fontSize = 11.sp, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Authentication triggering button
            Button(
                onClick = {
                    if (isSigningUp) {
                        viewModel.registerWithFirebase(context, email, password, name)
                    } else {
                        viewModel.loginWithFirebase(context, email, password)
                    }
                },
                enabled = isButtonEnabled && !isLoader.value,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryDark,
                    disabledContainerColor = PrimaryDark.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("login_button")
            ) {
                Icon(
                    imageVector = if (isSigningUp) Icons.Default.AppRegistration else Icons.Default.Login,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF381E72)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSigningUp) "Sign Up Family Registry" else "Authenticate Secure Ledger",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF381E72)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Gated Google Switch Simulation
            OutlinedButton(
                onClick = {
                    viewModel.loginWithFirebase(context, "tofmah1@gmail.com", "google-oauth-preset-pass")
                },
                border = BorderStroke(1.dp, Color(0xFF2E3B5E)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_login_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = SecondaryDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue securely with Google", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Circular Loader progress overlay
        if (isLoader.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryDark, strokeWidth = 4.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Authenticating ledger database...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Main Interactive Dashboard Screen
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel
) {
    val dbState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val uName by viewModel.userName.collectAsStateWithLifecycle()
    val isGoogle by viewModel.isGoogleUser.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedMember by viewModel.selectedFamilyMember.collectAsStateWithLifecycle()
    val showAdminDialog by viewModel.showAdminUnlockDialog.collectAsStateWithLifecycle()
    val isAdminMode by viewModel.isAdminMode.collectAsStateWithLifecycle()
    val cards by viewModel.firestoreCards.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateFlowOf(0) } // 0 = Finance Dashboard, 1 = Admin Console (Gated)
    var showAddDialog by remember { mutableStateFlowOf(false) }
    var selectedTxForEdit by remember { mutableStateFlowOf<Transaction?>(null) }

    val context = LocalContext.current

    if (showAdminDialog) {
        AdminUnlockDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showAdminUnlockDialog.value = false }
        )
    }

    if (showAddDialog) {
        AddTransactionDialog(
            activeMonth = selectedMonth,
            onDismiss = { showAddDialog = false },
            onConfirm = { amount, type, category, day, desc, member ->
                viewModel.addTransaction(amount, type, category, day, desc, member)
                showAddDialog = false
            }
        )
    }

    if (selectedTxForEdit != null) {
        EditTransactionDialog(
            transaction = selectedTxForEdit!!,
            onDismiss = { selectedTxForEdit = null },
            onConfirm = { updated ->
                viewModel.updateTransaction(updated)
                selectedTxForEdit = null
            }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryDark,
                        selectedTextColor = PrimaryDark,
                        unselectedIconColor = Color(0xFF90A4AE),
                        unselectedTextColor = Color(0xFF90A4AE),
                        indicatorColor = Color(0xFF1D243F)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = {
                        if (isAdminMode) {
                            activeTab = 1
                        } else {
                            viewModel.showAdminUnlockDialog.value = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (isAdminMode) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Admin Console",
                            tint = if (isAdminMode) SecondaryDark else Color(0xFF90A4AE)
                        )
                    },
                    label = { Text("Admin") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SecondaryDark,
                        selectedTextColor = SecondaryDark,
                        unselectedIconColor = Color(0xFF90A4AE),
                        unselectedTextColor = Color(0xFF90A4AE),
                        indicatorColor = Color(0xFF1D243F)
                    )
                )
            }
        },
        floatingActionButton = {
            if (activeTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = PrimaryDark,
                    contentColor = Color(0xFF001E22),
                    modifier = Modifier.testTag("add_transaction_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log Cashflow")
                }
            }
        },
        containerColor = DarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Dashboard Header / Profile Switcher panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBg)
                    .padding(vertical = 16.dp, horizontal = 18.dp)
            ) {
                // Branding Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon_logo_1781812448102),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AbseFlow Ledger",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // User Profile chip with dynamic status indicators
                    Card(
                        shape = RoundedCornerShape(40.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, if (isAdminMode) SecondaryDark else DarkBorder),
                        onClick = { viewModel.logoutFromFirebase() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isAdminMode) SecondaryDark else PrimaryDark)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAdminMode) "Admin Mode" else uName.take(12),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = "Logout", tint = ColorExpense, modifier = Modifier.size(12.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time Series Month Selector Row (January - December)
                Text(
                    text = "Calendar Period (Time Series Toggle)",
                    color = Color(0xFF90A4AE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                MonthSliderToggle(
                    selectedMonth = selectedMonth,
                    onMonthSelected = { viewModel.selectedMonth.value = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Family Member Switcher Grid (Drives filtering reactively)
                Text(
                    text = "Family Filter Switcher (Family of 4)",
                    color = Color(0xFF90A4AE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FamilyProfileSwitcherRow(
                    selectedMember = selectedMember,
                    onMemberSelected = { viewModel.selectedFamilyMember.value = it }
                )
            }

            // Centralized Swipeable Tabs Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (activeTab == 0) {
                    DashboardAnalysisContent(
                        dbState = dbState,
                        cards = cards,
                        onEditTx = { selectedTxForEdit = it },
                        onDeleteTx = { viewModel.deleteTransaction(it) },
                        isAdmin = isAdminMode
                    )
                } else {
                    AdminManagementConsole(
                        viewModel = viewModel,
                        dbState = dbState,
                        onDeactivateAdmin = { viewModel.revokeAdminMode() }
                    )
                }
            }
        }
    }
}

// Custom Horizontal Scrolling Month Navigation Bar
@Composable
fun MonthSliderToggle(
    selectedMonth: Int,
    onMonthSelected: (Int) -> Unit
) {
    val months = listOf(
        "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
        "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
    )
    val listState = rememberLazyListState()

    // Automatically align slider focus on the selected month
    LaunchedEffect(selectedMonth) {
        listState.animateScrollToItem(max(0, selectedMonth - 3))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        months.forEachIndexed { index, name ->
            val monthInt = index + 1
            val isActive = monthInt == selectedMonth

            val cardColor by animateColorAsState(
                targetValue = if (isActive) PrimaryDark else DarkSurfaceVariant,
                label = "color"
            )
            val textColor by animateColorAsState(
                targetValue = if (isActive) Color(0xFF381E72) else Color(0xFFE6E1E5).copy(alpha = 0.8f),
                label = "text"
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (isActive) PrimaryDark else DarkBorder),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier
                    .width(62.dp)
                    .clickable { onMonthSelected(monthInt) }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }
    }
}

// Custom Horizontal Family Switching List
@Composable
fun FamilyProfileSwitcherRow(
    selectedMember: String,
    onMemberSelected: (String) -> Unit
) {
    val familyDetails = listOf(
        FamilyMemberDef("All", "Global", Icons.Default.Groups, Color(0xFF1769AA)),
        FamilyMemberDef("Dad", "Robert", Icons.Default.Male, ColorHome),
        FamilyMemberDef("Mom", "Sarah", Icons.Default.Female, ColorCar),
        FamilyMemberDef("Son", "Leo", Icons.Default.Face, ColorShop),
        FamilyMemberDef("Daughter", "Mia", Icons.Default.ChildCare, ColorExtra)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        familyDetails.forEach { item ->
            val isSelected = item.id == selectedMember
            
            val scalingFactor by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "scale"
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (isSelected) item.accentColor else DarkBorder),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) DarkSurfaceVariant else DarkSurface
                ),
                onClick = { onMemberSelected(item.id) },
                modifier = Modifier
                    .width(105.dp)
                    .drawBehind {
                        if (isSelected) {
                            drawCircle(
                                color = item.accentColor,
                                radius = 4.dp.toPx(),
                                center = Offset(size.width / 2f, size.height - 8.dp.toPx())
                            )
                        }
                    }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(item.accentColor.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.name,
                            tint = item.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.id,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = item.name,
                        fontSize = 9.sp,
                        color = Color(0xFF90A4AE)
                    )
                }
            }
        }
    }
}

data class FamilyMemberDef(
    val id: String,
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color
)

// Interactive carousel showing real-time Firestore cards
@Composable
fun FirestoreCardsCarousel(
    cards: List<CardItem>,
    isAdmin: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F1224))
            .border(BorderStroke(1.dp, Color(0xFF1E294B)), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = PrimaryDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Active Firestore Cards",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = if (isAdmin) "Admin Access" else "Read-Only (RBAC)",
                color = if (isAdmin) SecondaryDark else Color(0xFF90A4AE),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isAdmin) SecondaryDark.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No card documents registered on Firestore.\nAdmins can register cards in the Admin Console.",
                    color = Color(0xFF90A4AE),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                cards.forEach { card ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF060913)),
                        border = BorderStroke(1.dp, Color(0xFF1E2640)),
                        modifier = Modifier
                            .width(220.dp)
                            .height(115.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = card.title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = card.description,
                                        color = Color(0xFF90A4AE),
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Wallet,
                                    contentDescription = null,
                                    tint = PrimaryDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Column {
                                Text(
                                    text = "AVAIL BALANCE",
                                    color = Color(0xFF6272A4),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "$${"%.2f".format(card.balance)}",
                                    color = ColorIncome,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Main Flow Analysis Sub-Section (Tab 0)
@Composable
fun DashboardAnalysisContent(
    dbState: DashboardUiState,
    cards: List<CardItem>,
    onEditTx: (Transaction) -> Unit,
    onDeleteTx: (Transaction) -> Unit,
    isAdmin: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            // Consolidated Ledger Summary
            item {
                FamilyPerformanceHUD(
                    income = dbState.totalIncome,
                    expense = dbState.totalExpense,
                    savingSum = dbState.netSavings
                )
            }

            // Interactive card carousel showing real-time Firestore cards
            item {
                FirestoreCardsCarousel(
                    cards = cards,
                    isAdmin = isAdmin
                )
            }

            // High Fidelity Custom Time Series Bar Chart
            item {
                TimeHistoryCanvasChart(
                    selectedMonth = dbState.month,
                    incomes = dbState.monthlyIncomes,
                    expenses = dbState.monthlyExpenses
                )
            }

            // Dynamic Category Breakdown Gauge Meters
            item {
                CategoryBreakdownGauges(dbState = dbState)
            }

            // Interactive Transaction Lists
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Transactions ledger",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${dbState.transactions.size} records",
                        fontSize = 12.sp,
                        color = Color(0xFF90A4AE)
                    )
                }
            }

            if (dbState.transactions.isEmpty()) {
                item {
                    EmptyTransactionState()
                }
            } else {
                items(dbState.transactions, key = { it.id }) { tx ->
                    TransactionRowItem(
                        tx = tx,
                        onEdit = { onEditTx(tx) },
                        onDelete = { onDeleteTx(tx) },
                        isAdmin = isAdmin
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// Family Financial Summary HUD Card
@Composable
fun FamilyPerformanceHUD(
    income: Double,
    expense: Double,
    savingSum: Double
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SecondaryDark),
        border = BorderStroke(1.dp, PrimaryDark.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "FAMILY CASHFLOW STATUS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF21005D).copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$${"%,.2f".format(savingSum)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF21005D)
                )
                
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF21005D).copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = if (savingSum >= 0) "Surplus" else "Deficit",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = "Net Family Savings",
                fontSize = 12.sp,
                color = Color(0xFF21005D).copy(alpha = 0.8f)
            )

            HorizontalDivider(color = Color(0xFF21005D).copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 16.dp))

            // Sub Cashflows detail
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF21005D).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF21005D), modifier = Modifier.size(10.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Total Income", fontSize = 11.sp, color = Color(0xFF21005D).copy(alpha = 0.75f))
                    }
                    Text(
                        text = "$${"%,.2f".format(income)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF21005D).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFF21005D), modifier = Modifier.size(10.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Total Expense", fontSize = 11.sp, color = Color(0xFF21005D).copy(alpha = 0.75f))
                    }
                    Text(
                        text = "$${"%,.2f".format(expense)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// 12-Month Time Series Cashflow Chart (Drawn cleanly with Canvas)
@Composable
fun TimeHistoryCanvasChart(
    selectedMonth: Int,
    incomes: List<Double>,
    expenses: List<Double>
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12182D)),
        border = BorderStroke(1.dp, Color(0xFF2E3B5E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "12-MONTH TIMESERIES ANALYTICS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90A4AE),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Chart Design
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    // Safe max boundary computation
                    val maxVal = max(
                        max(incomes.maxOrNull() ?: 1.0, expenses.maxOrNull() ?: 1.0),
                        7000.0 // Minimum max ceiling spacing
                    )

                    // Draw Horizontal Gridlines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = (height / gridLines) * i
                        drawLine(
                            color = Color(0xFF1E2640),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // 12 Months width partition
                    val monthSpacing = width / 12f
                    val barWidth = monthSpacing * 0.35f

                    for (m in 0..11) {
                        val isCurrent = (m + 1) == selectedMonth
                        val monthStartX = m * monthSpacing + (monthSpacing - (barWidth * 2 + 4)) / 2f

                        val incAmt = incomes.getOrNull(m) ?: 0.0
                        val expAmt = expenses.getOrNull(m) ?: 0.0

                        val incHeight = (incAmt / maxVal * (height - 15.dp.toPx())).toFloat()
                        val expHeight = (expAmt / maxVal * (height - 15.dp.toPx())).toFloat()

                        val incY = height - incHeight
                        val expY = height - expHeight

                        // Highlight background overlay if active selected month
                        if (isCurrent) {
                            drawRoundRect(
                                color = Color(0xFF1D243F),
                                topLeft = Offset(m * monthSpacing, 0f),
                                size = Size(monthSpacing, height),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }

                        // Draw Income block (Teal/Green)
                        drawRoundRect(
                            color = if (isCurrent) ColorIncome else ColorIncome.copy(alpha = 0.6f),
                            topLeft = Offset(monthStartX, incY),
                            size = Size(barWidth, incHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Draw Expense block (Crimson)
                        drawRoundRect(
                            color = if (isCurrent) ColorExpense else ColorExpense.copy(alpha = 0.6f),
                            topLeft = Offset(monthStartX + barWidth + 2f, expY),
                            size = Size(barWidth, expHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Label Markers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Legends indicators
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ColorIncome))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Income", fontSize = 10.sp, color = Color(0xFF90A4AE))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ColorExpense))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Expense", fontSize = 10.sp, color = Color(0xFF90A4AE))
                    }
                }

                Text(
                    text = "Tap upper months to filter details",
                    fontSize = 9.sp,
                    color = Color(0xFF607D8B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Double Gauge HUD Category breakdown (Bento 2x2 Grid)
@Composable
fun CategoryBreakdownGauges(
    dbState: DashboardUiState
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "BENTO BUDGET ALLOCATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryDark.copy(alpha = 0.8f),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Row 1: Home & Car
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoCategoryCard(
                name = "Home",
                amount = dbState.homeExpense,
                icon = Icons.Default.Home,
                isHighlight = false,
                modifier = Modifier.weight(1f)
            )

            BentoCategoryCard(
                name = "Car",
                amount = dbState.carExpense,
                icon = Icons.Default.DirectionsCar,
                isHighlight = false,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Shop & Extra
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoCategoryCard(
                name = "Shop",
                amount = dbState.shopExpense,
                icon = Icons.Default.ShoppingCart,
                isHighlight = false,
                modifier = Modifier.weight(1f)
            )

            BentoCategoryCard(
                name = "Extra",
                amount = dbState.extraExpense,
                icon = Icons.Default.Star,
                isHighlight = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun BentoCategoryCard(
    name: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isHighlight: Boolean,
    modifier: Modifier = Modifier
) {
    // If highlight: bg = TertiaryDark (#F2B8B5), text = Color(0xFF601410), icon container white/40
    // If standard: bg = DarkSurfaceVariant (#49454F), text = Color.White, icon container PrimaryDark (#D0BCFF), icon color = Color(0xFF381E72)
    val cardBg = if (isHighlight) TertiaryDark else DarkSurfaceVariant
    val textColor = if (isHighlight) Color(0xFF601410) else Color.White
    val subTextColor = if (isHighlight) Color(0xFF601410).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
    val iconBg = if (isHighlight) Color.White.copy(alpha = 0.4f) else PrimaryDark
    val iconTint = if (isHighlight) Color(0xFF601410) else Color(0xFF381E72)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = modifier.height(115.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Beautiful rounded Icon container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Bottom Section: Category Text details
            Column {
                Text(
                    text = name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = subTextColor
                )
                Text(
                    text = "$${"%,.0f".format(amount)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

data class CategoryDef(
    val name: String,
    val amount: Double,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// Transaction List item Row
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRowItem(
    tx: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isAdmin: Boolean
) {
    val isExpense = tx.type == "EXPENSE"
    val categoryColor = when (tx.category) {
        "Home" -> ColorHome
        "Car" -> ColorCar
        "Shop" -> ColorShop
        else -> ColorExtra
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isAdmin) onEdit() },
                onLongClick = { if (isAdmin) onEdit() }
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            // Visual circle indicator representing category
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f))
            ) {
                val icon = when (tx.category) {
                    "Home" -> Icons.Default.Home
                    "Car" -> Icons.Default.DirectionsCar
                    "Shop" -> Icons.Default.ShoppingCart
                    else -> Icons.Default.Star
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text detail
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.description,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Day ${tx.day}",
                        fontSize = 10.sp,
                        color = Color(0xFF90A4AE)
                    )
                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0xFF607D8B)))
                    Text(
                        text = tx.familyMember + " (${tx.category})",
                        fontSize = 10.sp,
                        color = categoryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Cash value and Admin CRUD actions if triggered
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "${if (isExpense) "-" else "+"}$${"%,.2f".format(tx.amount)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isExpense) ColorExpense else ColorIncome
                )

                if (isAdmin) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryDark, modifier = Modifier.size(14.dp))
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ColorExpense, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTransactionState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Wallet,
            contentDescription = null,
            tint = Color(0xFF1E2640),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "No financial activity recorded",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF90A4AE)
        )
        Text(
            text = "Log simple dynamic family cashflows via the Floating Action control.",
            fontSize = 11.sp,
            color = Color(0xFF607D8B),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

// Elevated Administrator Gated Workspace Panel (Tab 1)
@Composable
fun AdminManagementConsole(
    viewModel: FinanceViewModel,
    dbState: DashboardUiState,
    onDeactivateAdmin: () -> Unit
) {
    var globalSliderValue by remember { mutableStateOf(0f) } // -50% to +100%
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val cards by viewModel.firestoreCards.collectAsStateWithLifecycle()
    val familyMembers by viewModel.firestoreFamilyMembers.collectAsStateWithLifecycle()
    val categories by viewModel.firestoreCategories.collectAsStateWithLifecycle()
    val isLoader by viewModel.isFirebaseLoading.collectAsStateWithLifecycle()
    
    // Admin Dialog controllers
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<CardItem?>(null) }
    var cardIdToDelete by remember { mutableStateOf<String?>(null) }
    
    // Claims promotion sandbox properties
    var promoUserId by remember { mutableStateOf("") }
    var promoIsAdmin by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Gated administration Active state
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101932)),
                border = BorderStroke(1.dp, SecondaryDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SecondaryDark.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SecondaryDark)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gated Administration active",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Elevated global parameters and modifications unlocked",
                            fontSize = 10.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }
                    Button(
                        onClick = onDeactivateAdmin,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorExpense),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Lock", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // 1. ROLE-BASED ACCESS CONTROL (RBAC) CLAIMS PROVISIONER
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1224)),
                border = BorderStroke(1.dp, Color(0xFF1E294B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = SecondaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RBAC Custom Claims Provisioner",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Manage Firebase claims and Firestore roles. Promoting an account will set custom claims { admin: true } on their auth token and update Firestore '/users/{uid}' document.",
                        fontSize = 10.sp,
                        color = Color(0xFF90A4AE),
                        lineHeight = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Quick promote target input
                    OutlinedTextField(
                        value = promoUserId,
                        onValueChange = { promoUserId = it },
                        label = { Text("Target User ID (user.uid)") },
                        placeholder = { Text("e.g. local_admin_uid or real UUID") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SecondaryDark,
                            unfocusedBorderColor = Color(0xFF2E3B5E)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (promoUserId.isNotBlank()) {
                                    viewModel.toggleCustomRoleClaimOnState(context, promoUserId, true)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Grant Admin Claim", fontSize = 11.sp, color = Color(0xFF1D243F), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (promoUserId.isNotBlank()) {
                                    viewModel.toggleCustomRoleClaimOnState(context, promoUserId, false)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2640)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Revoke Claim (User)", fontSize = 11.sp, color = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Render quick list of registered local simulation users
                    Text(
                        text = "Active Sandbox Profiles:",
                        fontSize = 10.sp,
                        color = Color(0xFF90A4AE),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    
                    com.example.data.repository.FirebaseManager.getAllRegisteredUsers().forEach { user ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0A0C18))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .clickable { promoUserId = user.uid }
                        ) {
                            Column {
                                Text(user.displayName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("uid: ${user.uid} • ${user.email}", color = Color(0xFF90A4AE), fontSize = 9.sp)
                            }
                            Text(
                                text = user.role.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (user.role == "admin") SecondaryDark else Color(0xFF90A4AE),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (user.role == "admin") SecondaryDark.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. FIRESTORE CARDS CRUD CONSOLE (ADMIN)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1224)),
                border = BorderStroke(1.dp, Color(0xFF1E294B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = SecondaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Firestore Cards Collection",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        IconButton(
                            onClick = { showAddCardDialog = true },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(SecondaryDark.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Card", tint = SecondaryDark, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create, edit, and delete real-time Firestore '/cards' documents securely with administrative claims verified.",
                        fontSize = 10.sp,
                        color = Color(0xFF90A4AE),
                        lineHeight = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (cards.isEmpty()) {
                        Text(
                            text = "No cards registered. Tap the '+' button above to add a Firestore card.",
                            color = Color(0xFF90A4AE),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            cards.forEach { card ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0C18)),
                                    border = BorderStroke(1.dp, Color(0xFF1E2640)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = card.title,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = card.description,
                                                color = Color(0xFF90A4AE),
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Balance: $${"%.2f".format(card.balance)}",
                                                color = ColorIncome,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                        
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = { cardToEdit = card },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Card", tint = SecondaryDark, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { cardIdToDelete = card.id },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Card", tint = ColorExpense, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Global Multi-inflation Value Adjuster Engine
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12182D)),
                border = BorderStroke(1.dp, Color(0xFF2E3B5E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = SecondaryDark, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Global Ledger Multiplier Settings",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Modify all existing ledgers in SQLite globally (e.g. inflating or flat adjustments by scale percentages).",
                        fontSize = 11.sp,
                        color = Color(0xFF90A4AE)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Adjustment Factor: ${if (globalSliderValue > 0) "+" else ""}${globalSliderValue.toInt()}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (globalSliderValue >= 0) ColorIncome else ColorExpense,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Slider(
                        value = globalSliderValue,
                        onValueChange = { globalSliderValue = it },
                        valueRange = -50f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = SecondaryDark,
                            activeTrackColor = SecondaryDark,
                            inactiveTrackColor = Color(0xFF1E2640)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Apply actions
                    Button(
                        onClick = {
                            viewModel.adjustFinancialValuesGlobally(globalSliderValue.toDouble())
                            globalSliderValue = 0f
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("apply_multiplier_button")
                    ) {
                        Text("Apply Global Re-scaling", color = Color(0xFF002201), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Forward reports to external stakeholders via intent
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12182D)),
                border = BorderStroke(1.dp, Color(0xFF2E3B5E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = SecondaryDark, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Forward Summarized Ledger Reports",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Extract compilation and trigger native system Android Share Sheets to external auditors or banks.",
                        fontSize = 11.sp,
                        color = Color(0xFF90A4AE)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val rpt = viewModel.triggerForwardSummaryReport(dbState.month)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, rpt)
                            }
                            context.startActivity(Intent.createChooser(intent, "Forward AbseFlow Ledger"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("forward_report_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate & Forward to Stakeholders", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. CLOUD FIRESTORE FAMILY PROFILES MANAGER
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1224)),
                border = BorderStroke(1.dp, Color(0xFF1E294B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = SecondaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Firestore Family Profiles",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        IconButton(
                            onClick = { showAddMemberDialog = true },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(SecondaryDark.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Member", tint = SecondaryDark, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Register, configure, and synchronize dynamic family member profiles fully backed by Cloud Firestore collections.",
                        fontSize = 10.sp,
                        color = Color(0xFF90A4AE),
                        lineHeight = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (familyMembers.isEmpty()) {
                        Text(
                            text = "No family profiles registered. Tap '+' to create a profile.",
                            color = Color(0xFF90A4AE),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            familyMembers.forEach { member ->
                                val colorFromHex = try {
                                    Color(android.graphics.Color.parseColor(member.accentColorHex))
                                } catch (ex: Exception) {
                                    SecondaryDark
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0A0C18))
                                        .border(1.dp, Color(0xFF1E2640), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(colorFromHex)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(member.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("${member.relationship} • Limit: $${"%.2f".format(member.monthlyLimit)}", color = Color(0xFF90A4AE), fontSize = 10.sp)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                com.example.data.repository.FirebaseManager.deleteFamilyMember(context, member.id)
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Profile", tint = ColorExpense, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. CLOUD FIRESTORE NESTED CATEGORIES MANAGER
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1224)),
                border = BorderStroke(1.dp, Color(0xFF1E294B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = SecondaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Firestore Nested Categories",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        IconButton(
                            onClick = { showAddCategoryDialog = true },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(SecondaryDark.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Category", tint = SecondaryDark, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Manage hierarchical/nested budget categories with parent mapping and dynamic allocations synchronized live to Firestore.",
                        fontSize = 10.sp,
                        color = Color(0xFF90A4AE),
                        lineHeight = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (categories.isEmpty()) {
                        Text(
                            text = "No custom categories registered. Tap '+' to create a nested category.",
                            color = Color(0xFF90A4AE),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    } else {
                        // Display hierarchical nested list
                        val parentCategories = categories.filter { it.parentId == null }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            parentCategories.forEach { parent ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0A0C18))
                                        .border(1.dp, Color(0xFF1E2640), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(parent.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                            if (parent.description.isNotBlank()) {
                                                Text(parent.description, color = Color(0xFF90A4AE), fontSize = 10.sp)
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "$${"%.0f".format(parent.allocation)}",
                                                color = SecondaryDark,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            )
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        com.example.data.repository.FirebaseManager.deleteNestedCategory(context, parent.id)
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = ColorExpense, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                    
                                    val children = categories.filter { it.parentId == parent.id }
                                    if (children.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(start = 14.dp)
                                        ) {
                                            children.forEach { sub ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.White.copy(alpha = 0.02f))
                                                        .padding(6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("↳ ${sub.name}", color = Color(0xFFEEEEEE), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        if (sub.description.isNotBlank()) {
                                                            Text(sub.description, color = Color(0xFF90A4AE), fontSize = 9.sp)
                                                        }
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "$${"%.0f".format(sub.allocation)}",
                                                            color = Color(0xFF81C784),
                                                            fontSize = 10.sp,
                                                            modifier = Modifier.padding(horizontal = 4.dp)
                                                        )
                                                        IconButton(
                                                            onClick = {
                                                                scope.launch {
                                                                    com.example.data.repository.FirebaseManager.deleteNestedCategory(context, sub.id)
                                                                }
                                                            },
                                                            modifier = Modifier.size(20.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = null, tint = ColorExpense.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Dialog Managers for Admin CRUD actions
    if (showAddCardDialog) {
        AddCardDialog(
            isLoader = isLoader,
            onDismiss = { showAddCardDialog = false },
            onConfirm = { title, desc, bal ->
                viewModel.createFirestoreCard(context, title, desc, bal)
                showAddCardDialog = false
            }
        )
    }
    
    val activeEditCard = cardToEdit
    if (activeEditCard != null) {
        EditCardDialog(
            card = activeEditCard,
            isLoader = isLoader,
            onDismiss = { cardToEdit = null },
            onConfirm = { updated ->
                viewModel.updateFirestoreCard(context, updated)
                cardToEdit = null
            }
        )
    }
    
    val activeDeleteId = cardIdToDelete
    if (activeDeleteId != null) {
        DeleteConfirmDialog(
            onDismiss = { cardIdToDelete = null },
            onConfirm = {
                viewModel.deleteFirestoreCard(context, activeDeleteId)
                cardIdToDelete = null
            }
        )
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { name, rel, icon, accent, limit ->
                val newId = name.replace(" ", "")
                val profile = FamilyMemberProfile(
                    id = newId,
                    name = name,
                    relationship = rel,
                    iconName = icon,
                    accentColorHex = accent,
                    monthlyLimit = limit
                )
                scope.launch {
                    com.example.data.repository.FirebaseManager.createFamilyMember(context, profile)
                }
                showAddMemberDialog = false
            }
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            categories = categories,
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, desc, parentId, alloc ->
                val newId = name.replace(" ", "")
                val category = NestedFinanceCategory(
                    id = newId,
                    name = name,
                    description = desc,
                    parentId = parentId,
                    allocation = alloc
                )
                scope.launch {
                    com.example.data.repository.FirebaseManager.createNestedCategory(context, category)
                }
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rel by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("Male") }
    var accent by remember { mutableStateOf("#1769AA") }
    var limitStr by remember { mutableStateOf("5000.00") }

    val isFormComplete = name.isNotBlank() && rel.isNotBlank() && limitStr.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12182D),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.People, contentDescription = null, tint = SecondaryDark, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Family Profile", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Post a new dynamic family member profile into Firestore collection.", fontSize = 11.sp, color = Color(0xFF90A4AE))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Alice") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SecondaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rel,
                    onValueChange = { rel = it },
                    label = { Text("Relationship") },
                    placeholder = { Text("e.g. Spouse, Grandparent") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SecondaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("Monthly Budget Limit ($)") },
                    placeholder = { Text("5000.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SecondaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = accent,
                    onValueChange = { accent = it },
                    label = { Text("Accent Color Hex") },
                    placeholder = { Text("#E57373") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SecondaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, rel, icon, accent, limitStr.toDoubleOrNull() ?: 5000.0) },
                enabled = isFormComplete,
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryDark)
            ) {
                Text("Add Profile", color = Color(0xFF12182D), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

@Composable
fun AddCategoryDialog(
    categories: List<NestedFinanceCategory>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<String?>(null) }
    var allocStr by remember { mutableStateOf("1000.00") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val isFormComplete = name.isNotBlank() && allocStr.toDoubleOrNull() != null
    val parentOptions = categories.filter { it.parentId == null }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12182D),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Category, contentDescription = null, tint = SecondaryDark, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Nested Category", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Post a new parent or subcategory with specified allocation into Firestore collection.", fontSize = 11.sp, color = Color(0xFF90A4AE))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Health") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SecondaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Medicine & Care") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SecondaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = allocStr,
                    onValueChange = { allocStr = it },
                    label = { Text("Allocation budget ($)") },
                    placeholder = { Text("1000.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SecondaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Simple Dropdown-style Selector for Parent ID
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedDropdown = true },
                        border = BorderStroke(1.dp, Color(0xFF2E3B5E)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (parentId == null) "No Parent (Root Category)" else "Selected Parent: $parentId",
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.background(Color(0xFF1E2640))
                    ) {
                        DropdownMenuItem(
                            text = { Text("No Parent (Root Category)", color = Color.White) },
                            onClick = {
                                parentId = null
                                expandedDropdown = false
                            }
                        )
                        parentOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.name, color = Color.White) },
                                onClick = {
                                    parentId = opt.id
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, desc, parentId, allocStr.toDoubleOrNull() ?: 1000.0) },
                enabled = isFormComplete,
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryDark)
            ) {
                Text("Add Category", color = Color(0xFF12182D), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

// Dialog Component for adding new Firestore Cards
@Composable
fun AddCardDialog(
    isLoader: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var balStr by remember { mutableStateOf("") }
    
    val isFormComplete = title.isNotBlank() && desc.isNotBlank() && balStr.toDoubleOrNull() != null
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12182D),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = SecondaryDark, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Firestore Card", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Post a new credit, debit, or cash ledger card into Firestore '/cards' collection.", fontSize = 11.sp, color = Color(0xFF90A4AE))
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Card Title") },
                    placeholder = { Text("e.g. Visa Gold Ledger") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SecondaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Short Description") },
                    placeholder = { Text("e.g. Family Savings Reserve") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SecondaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = balStr,
                    onValueChange = { balStr = it },
                    label = { Text("Initial Balance ($)") },
                    placeholder = { Text("e.g. 5000.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SecondaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, desc, balStr.toDoubleOrNull() ?: 0.0) },
                enabled = isFormComplete && !isLoader,
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Create Card", color = Color(0xFF1D243F), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

// Dialog Component for editing existing Firestore Cards
@Composable
fun EditCardDialog(
    card: CardItem,
    isLoader: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (CardItem) -> Unit
) {
    var title by remember { mutableStateOf(card.title) }
    var desc by remember { mutableStateOf(card.description) }
    var balStr by remember { mutableStateOf(card.balance.toString()) }
    
    val isFormComplete = title.isNotBlank() && desc.isNotBlank() && balStr.toDoubleOrNull() != null
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12182D),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = SecondaryDark, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Modify Card Details", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Edit this card document on Firestore. This operation requires admin custom claim token verification.", fontSize = 11.sp, color = Color(0xFF90A4AE))
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SecondaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SecondaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = balStr,
                    onValueChange = { balStr = it },
                    label = { Text("Balance ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SecondaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(card.copy(title = title, description = desc, balance = balStr.toDoubleOrNull() ?: card.balance)) },
                enabled = isFormComplete && !isLoader,
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Changes", color = Color(0xFF1D243F), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

// Confirmation Dialog on deletes
@Composable
fun DeleteConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12182D),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ColorExpense, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm Document Deletion", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text("Are you completely sure you want to delete this card from the Firestore.cards collection? This action is irreversible.", fontSize = 12.sp, color = Color(0xFF90A4AE))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ColorExpense),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Delete permanently", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

private fun Intent.putchar(extraText: String, rpt: String) {
    putExtra(extraText, rpt)
}

// Access PIN dial elevated Dialog
@Composable
fun AdminUnlockDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    var passInput by remember { mutableStateOf("") }
    val error by viewModel.adminPasswordError.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12182D),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SecondaryDark, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Elevated Permission Check", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "Unlock the administrative ledger. Post custom entries, delete audit trails, scale values locally.",
                    fontSize = 12.sp,
                    color = Color(0xFF90A4AE)
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = passInput,
                    onValueChange = {
                        passInput = it
                        if (error != null) viewModel.adminPasswordError.value = null
                    },
                    label = { Text("Passphrase Pin") },
                    placeholder = { Text("admin123 or 8888") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SecondaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E),
                        focusedLabelColor = SecondaryDark,
                        unfocusedLabelColor = Color(0xFF90A4AE)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pass_input")
                )

                if (error != null) {
                    Text(
                        text = error!!,
                        color = ColorExpense,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.handleAdminUnlock(passInput) },
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("admin_unlock_submit")
            ) {
                Text("Verify Gate", color = Color(0xFF002201), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = Color(0xFF90A4AE))
            }
        }
    )
}

// Add Transaction prompt dialogue
@Composable
fun AddTransactionDialog(
    activeMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, Int, String, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") } // INCOME or EXPENSE
    var category by remember { mutableStateOf("Home") } // Home, Car, Shop, Extra
    var familyMember by remember { mutableStateOf("Dad") } // Dad, Mom, Son, Daughter
    var dayInt by remember { mutableStateOf(15) }
    var desc by remember { mutableStateOf("") }

    val categories = listOf("Home", "Car", "Shop", "Extra")
    val members = listOf("Dad", "Mom", "Son", "Daughter")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12182D),
        title = {
            Text("Record Financial Transaction", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                // Type selector tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { type = "INCOME" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "INCOME") ColorIncome else Color(0xFF1E2640)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Income", color = if (type == "INCOME") Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { type = "EXPENSE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "EXPENSE") ColorExpense else Color(0xFF1E2640)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Expense", color = if (type == "EXPENSE") Color.White else Color(0xFF90A4AE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount text input
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_amount_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Ledger Description") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_desc_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Day Selection
                Text("Transaction Day of Month: $dayInt", fontSize = 11.sp, color = Color(0xFF90A4AE), fontWeight = FontWeight.Bold)
                Slider(
                    value = dayInt.toFloat(),
                    onValueChange = { dayInt = it.toInt() },
                    valueRange = 1f..28f,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryDark,
                        activeTrackColor = PrimaryDark
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category select grid
                Text("Allocation Category", fontSize = 11.sp, color = Color(0xFF90A4AE), fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = cat == category
                        val categoryColor = when (cat) {
                            "Home" -> ColorHome
                            "Car" -> ColorCar
                            "Shop" -> ColorShop
                            else -> ColorExtra
                        }
                        
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSel) categoryColor else Color(0xFF1E2640)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) categoryColor.copy(alpha = 0.25f) else Color(0xFF12182D)
                            ),
                            onClick = { category = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (isSel) categoryColor else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Family Switchee select grid
                Text("Assign to Family Member", fontSize = 11.sp, color = Color(0xFF90A4AE), fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    members.forEach { mem ->
                        val isSel = mem == familyMember
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSel) PrimaryDark else Color(0xFF1E2640)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) PrimaryDark.copy(alpha = 0.2f) else Color(0xFF12182D)
                            ),
                            onClick = { familyMember = mem }
                        ) {
                            Text(
                                text = mem,
                                color = if (isSel) PrimaryDark else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(amt, type, category, dayInt, desc, familyMember)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Log Cashflow", color = Color(0xFF001E22), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF90A4AE))
            }
        }
    )
}

// Edit Transaction dialogue
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit
) {
    var amountStr by remember { mutableStateOf(transaction.amount.toString()) }
    var type by remember { mutableStateOf(transaction.type) }
    var category by remember { mutableStateOf(transaction.category) }
    var familyMember by remember { mutableStateOf(transaction.familyMember) }
    var dayInt by remember { mutableStateOf(transaction.day) }
    var desc by remember { mutableStateOf(transaction.description) }

    val categories = listOf("Home", "Car", "Shop", "Extra")
    val members = listOf("Dad", "Mom", "Son", "Daughter")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12182D),
        title = {
            Text("Modify Entry Trail", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { type = "INCOME" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "INCOME") ColorIncome else Color(0xFF1E2640)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Income", color = if (type == "INCOME") Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { type = "EXPENSE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "EXPENSE") ColorExpense else Color(0xFF1E2640)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Expense", color = if (type == "EXPENSE") Color.White else Color(0xFF90A4AE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Ledger Description") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = Color(0xFF2E3B5E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Day Slider
                Text("Transaction Day: $dayInt", fontSize = 11.sp, color = Color(0xFF90A4AE), fontWeight = FontWeight.Bold)
                Slider(
                    value = dayInt.toFloat(),
                    onValueChange = { dayInt = it.toInt() },
                    valueRange = 1f..28f,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryDark,
                        activeTrackColor = PrimaryDark
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category select list
                Text("Allocation Category", fontSize = 11.sp, color = Color(0xFF90A4AE), fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = cat == category
                        val categoryColor = when (cat) {
                            "Home" -> ColorHome
                            "Car" -> ColorCar
                            "Shop" -> ColorShop
                            else -> ColorExtra
                        }
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSel) categoryColor else Color(0xFF1E2640)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) categoryColor.copy(alpha = 0.25f) else Color(0xFF12182D)
                            ),
                            onClick = { category = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (isSel) categoryColor else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Family select list
                Text("Assign to Family Member", fontSize = 11.sp, color = Color(0xFF90A4AE), fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    members.forEach { mem ->
                        val isSel = mem == familyMember
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSel) PrimaryDark else Color(0xFF1E2640)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) PrimaryDark.copy(alpha = 0.2f) else Color(0xFF12182D)
                            ),
                            onClick = { familyMember = mem }
                        ) {
                            Text(
                                text = mem,
                                color = if (isSel) PrimaryDark else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(
                            transaction.copy(
                                amount = amt,
                                type = type,
                                category = category,
                                day = dayInt,
                                description = desc,
                                familyMember = familyMember
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Updates", color = Color(0xFF001E22), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF90A4AE))
            }
        }
    )
}

// Stateful helper functions
private fun <T> mutableStateFlowOf(value: T) = mutableStateOf(value)
