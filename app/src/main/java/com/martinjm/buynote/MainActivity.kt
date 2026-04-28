package com.martinjm.buynote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.martinjm.buynote.ui.navigation.AppNavigation
import com.martinjm.buynote.ui.theme.BuyNoteTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuyNoteTheme {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}
