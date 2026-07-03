package com.folio.app

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.folio.app.update.UpdateManager
import com.folio.feature.bookshelf.BookDetailScreen
import com.folio.feature.bookshelf.BookshelfScreen
import com.folio.feature.reader.ReaderScreen
import com.folio.feature.settings.SettingsScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable object BookshelfRoute
@Serializable data class BookDetailRoute(val bookId: Long)
@Serializable data class ReaderRoute(val bookId: Long)
@Serializable object SettingsRoute

@Composable
fun FolioNavGraph(
    navController: NavHostController = rememberNavController(),
    updateManager: UpdateManager? = null
) {
    val scope = rememberCoroutineScope()
    var updateStatus by remember { mutableStateOf<String?>(null) }
    val currentVersion = updateManager?.currentVersion ?: "1.0.0"

    NavHost(
        navController = navController,
        startDestination = BookshelfRoute
    ) {
        composable<BookshelfRoute>(
            enterTransition = { fadeIn(animationSpec = spring(dampingRatio = 0.8f)) + scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f)) },
            exitTransition = { fadeOut(animationSpec = spring(dampingRatio = 0.8f)) + scaleOut(targetScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f)) }
        ) {
            BookshelfScreen(
                onBookClick = { bookId ->
                    navController.navigate(BookDetailRoute(bookId))
                },
                onSettingsClick = {
                    navController.navigate(SettingsRoute)
                }
            )
        }
        composable<BookDetailRoute>(
            enterTransition = { fadeIn(animationSpec = spring(dampingRatio = 0.8f)) + scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f)) },
            exitTransition = { fadeOut(animationSpec = spring(dampingRatio = 0.8f)) + scaleOut(targetScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f)) },
            popEnterTransition = { fadeIn(animationSpec = spring(dampingRatio = 0.8f)) + scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f)) },
            popExitTransition = { fadeOut(animationSpec = spring(dampingRatio = 0.8f)) + scaleOut(targetScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f)) }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<BookDetailRoute>()
            BookDetailScreen(
                bookId = route.bookId,
                onBackClick = { navController.popBackStack() },
                onStartReading = { bookId ->
                    navController.navigate(ReaderRoute(bookId))
                }
            )
        }
        composable<ReaderRoute>(
            enterTransition = { fadeIn(animationSpec = spring(dampingRatio = 0.8f)) + scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f)) },
            exitTransition = { fadeOut(animationSpec = spring(dampingRatio = 0.8f)) + scaleOut(targetScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f)) },
            popEnterTransition = { fadeIn(animationSpec = spring(dampingRatio = 0.8f)) + scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f)) },
            popExitTransition = { fadeOut(animationSpec = spring(dampingRatio = 0.8f)) + scaleOut(targetScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f)) }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<ReaderRoute>()
            ReaderScreen(
                bookId = route.bookId,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<SettingsRoute>(
            enterTransition = { fadeIn(animationSpec = spring(dampingRatio = 0.8f)) + scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f)) },
            exitTransition = { fadeOut(animationSpec = spring(dampingRatio = 0.8f)) + scaleOut(targetScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f)) }
        ) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onCheckForUpdates = {
                    updateManager?.let { mgr ->
                        scope.launch {
                            updateStatus = "Checking..."
                            val result = mgr.checkForUpdate()
                            if (result.errorMessage != null) {
                                updateStatus = result.errorMessage
                                return@launch
                            }
                            if (!result.isAvailable || result.updateInfo == null) {
                                updateStatus = "You're up to date (v$currentVersion)"
                                return@launch
                            }
                            val info = result.updateInfo
                            updateStatus = "Downloading update..."
                            val file = mgr.downloadUpdate(info) { progress ->
                                updateStatus = "Downloading... ${(progress * 100).toInt()}%"
                            }
                            if (file == null) {
                                updateStatus = "Download failed"
                                return@launch
                            }
                            updateStatus = "Installing..."
                            if (mgr.installApk()) {
                                updateStatus = null
                            } else {
                                updateStatus = "Install failed"
                            }
                        }
                    }
                },
                currentVersion = currentVersion,
                updateStatus = updateStatus
            )
        }
    }
}
