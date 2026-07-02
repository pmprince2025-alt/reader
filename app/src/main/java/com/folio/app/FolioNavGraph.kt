package com.folio.app

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.folio.feature.bookshelf.BookDetailScreen
import com.folio.feature.bookshelf.BookshelfScreen
import com.folio.feature.reader.ReaderScreen
import com.folio.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable object BookshelfRoute
@Serializable data class BookDetailRoute(val bookId: Long)
@Serializable data class ReaderRoute(val bookId: Long)
@Serializable object SettingsRoute

@Composable
fun FolioNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val pendingUri = PendingImport.uri
    if (pendingUri != null) {
        PendingImport.uri = null
    }

    NavHost(
        navController = navController,
        startDestination = BookshelfRoute
    ) {
        composable<BookshelfRoute>(
            enterTransition = { fadeIn(animationSpec = spring(dampingRatio = 0.8f)) + scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f)) },
            exitTransition = { fadeOut(animationSpec = spring(dampingRatio = 0.8f)) + scaleOut(targetScale = 0.95f, animationSpec = spring(dampingRatio = 0.8f)) }
        ) {
            BookshelfScreen(
                pendingImportUri = pendingUri,
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
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
