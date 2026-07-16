package com.polyglotai.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import com.polyglotai.android.ui.PolyglotApp
import com.polyglotai.android.ui.theme.PolyglotTheme

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = AppContainer(this)
        enableEdgeToEdge()
        setContent {
            PolyglotTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    PolyglotApp(container, Modifier.padding(padding))
                }
            }
        }
    }
}
