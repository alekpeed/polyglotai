package com.polyglotai.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.polyglotai.android.data.ThemeMode
import com.polyglotai.android.ui.PolyglotApp
import com.polyglotai.android.ui.theme.PolyglotTheme

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = AppContainer(this)
        enableEdgeToEdge()
        setContent {
            // Theme choice lives in settings but is hoisted here so a change in the Settings screen
            // recolors the whole app immediately.
            var themeMode by remember { mutableStateOf(container.settings.themeMode) }
            val useDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            PolyglotTheme(useDark = useDark) {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    PolyglotApp(
                        container,
                        Modifier.padding(padding),
                        themeMode = themeMode,
                        onThemeChange = {
                            container.settings.themeMode = it
                            themeMode = it
                        },
                    )
                }
            }
        }
    }
}
