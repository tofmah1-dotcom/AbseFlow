package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.data.db.AppDatabase
import com.example.data.repository.TransactionRepository
import com.example.ui.screens.AbseFlowApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.FinanceViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize standard single-instance Room dependencies natively
    val database = AppDatabase.getDatabase(this)
    val repository = TransactionRepository(database.transactionDao())
    val factory = FinanceViewModelFactory(repository)
    
    // Instantiate ViewModel with lazy delegate
    val viewModel: FinanceViewModel by viewModels { factory }

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        AbseFlowApp(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}

