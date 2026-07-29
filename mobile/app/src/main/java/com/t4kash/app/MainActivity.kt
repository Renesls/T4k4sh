package com.t4kash.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.t4kash.app.ui.navigation.NavGraph
import com.t4kash.app.ui.session.UserSession
import com.t4kash.app.ui.theme.T4KASHTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        enableEdgeToEdge()
        UserSession.initialize(applicationContext)
        setContent {
            T4KASHTheme {
                NavGraph()
            }
        }
    }
}
