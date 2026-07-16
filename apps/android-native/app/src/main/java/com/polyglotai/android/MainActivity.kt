package com.polyglotai.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.polyglotai.android.ui.PolyglotApp
import com.polyglotai.android.ui.theme.Pack
import com.polyglotai.android.ui.theme.PolyglotTheme

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = AppContainer(this)
        enableEdgeToEdge()
        setContent {
            // Appearance (light/dark/classic) and the active language's pack world are hoisted here
            // so a change in either recolors the whole app immediately — the desktop app's
            // data-theme × data-pack, in Compose.
            var appTheme by remember { mutableStateOf(container.settings.appTheme) }
            var pack by remember { mutableStateOf(Pack.DEFAULT) }
            PolyglotTheme(theme = appTheme, pack = pack) {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    PolyglotApp(
                        container,
                        Modifier.padding(padding),
                        appTheme = appTheme,
                        onThemeChange = {
                            container.settings.appTheme = it
                            appTheme = it
                        },
                        onPackChange = { pack = it },
                    )
                }
            }
        }
    }
}
