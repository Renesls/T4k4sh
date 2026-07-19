package com.t4kash.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.t4kash.app.ui.navigation.NavGraph
import com.t4kash.app.ui.theme.T4KASHTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            T4KASHTheme {
                NavGraph()
            }
        }
    }
}
