package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.ScholariaApp
import com.example.ui.theme.ScholariaTheme
import com.example.ui.viewmodel.ScholariaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScholariaTheme {
                val vm: ScholariaViewModel = viewModel()
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScholariaApp(viewModel = vm)
                }
            }
        }
    }
}
