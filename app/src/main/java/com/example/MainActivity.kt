package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AnalystRepository
import com.example.data.local.AppDatabase
import com.example.ui.AnalystApp
import com.example.ui.AnalystViewModel
import com.example.ui.AnalystViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge to edge for modern borderless display
        enableEdgeToEdge()
        
        // Initialize Database & Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = AnalystRepository(database.datasetDao(), database.chatMessageDao())
        
        // Instantiate ViewModel
        val factory = AnalystViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[AnalystViewModel::class.java]
        
        setContent {
            MyApplicationTheme {
                AnalystApp(viewModel = viewModel)
            }
        }
    }
}
