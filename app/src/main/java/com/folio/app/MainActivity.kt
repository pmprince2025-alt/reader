package com.folio.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.folio.core.datastore.SettingsDataStore
import com.folio.core.ui.theme.FolioTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handlePdfIntent(intent)
        setContent {
            val appTheme by settingsDataStore.appTheme.collectAsState(
                initial = SettingsDataStore.AppTheme.SYSTEM
            )
            val isDarkOverride = when (appTheme) {
                SettingsDataStore.AppTheme.LIGHT -> false
                SettingsDataStore.AppTheme.DARK -> true
                SettingsDataStore.AppTheme.SYSTEM -> null
            }
            FolioTheme(darkTheme = isDarkOverride) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FolioNavGraph()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePdfIntent(intent)
    }

    private fun handlePdfIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.type == "application/pdf") {
            intent.data?.let { uri ->
                PendingImport.uri = uri
            }
        }
    }

    companion object {
        object PendingImport {
            var uri: Uri? = null
        }
    }
}
