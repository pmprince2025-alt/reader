package com.folio.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.folio.app.update.UpdateManager
import com.folio.core.common.PendingImport
import com.folio.core.datastore.SettingsDataStore
import com.folio.core.ui.theme.FolioTheme
import com.folio.core.ui.theme.PlasmaCyan
import com.folio.core.ui.theme.PlasmaViolet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var updateManager: UpdateManager

    @Inject
    lateinit var pendingImport: PendingImport

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
            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(1500)
                showSplash = false
            }

            FolioTheme(darkTheme = isDarkOverride) {
                if (showSplash) {
                    SplashScreen()
                } else {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        FolioNavGraph(updateManager = updateManager)
                    }
                }
            }
        }
    }

    @Composable
    private fun SplashScreen() {
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            visible = true
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PlasmaViolet.copy(alpha = 0.03f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .scale(if (visible) 1f else 0.8f)
                    .alpha(if (visible) 1f else 0f)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(PlasmaCyan.copy(alpha = 0.6f), PlasmaViolet.copy(alpha = 0.4f))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "F",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Folio",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlasmaViolet
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "PDF Reader",
                    fontSize = 14.sp,
                    color = PlasmaCyan.copy(alpha = 0.7f)
                )
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
                pendingImport.set(uri)
            }
        }
    }
}
