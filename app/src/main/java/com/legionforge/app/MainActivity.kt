package com.legionforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.legionforge.app.ui.nav.LegionForgeNavHost
import com.legionforge.app.ui.theme.LegionForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LegionForgeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LegionForgeNavHost()
                }
            }
        }
    }
}
