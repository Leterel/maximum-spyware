package com.maximumspyware.parody

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.maximumspyware.parody.ui.MaximumSpywareApp
import com.maximumspyware.parody.ui.theme.SpyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MaximumSpywareApp()
                }
            }
        }
    }
}
