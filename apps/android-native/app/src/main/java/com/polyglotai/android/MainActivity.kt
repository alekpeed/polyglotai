package com.polyglotai.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polyglotai.android.ui.theme.PolyglotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PolyglotTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    App(Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun App(modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp)) {
        Text("PolyglotAI", style = MaterialTheme.typography.headlineMedium)
        Text("Native Android — foundation build", style = MaterialTheme.typography.bodyMedium)
    }
}
