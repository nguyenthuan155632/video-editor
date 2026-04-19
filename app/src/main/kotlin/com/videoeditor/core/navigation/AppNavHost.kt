package com.videoeditor.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.videoeditor.core.theme.AuroraMotion
import com.videoeditor.feature.compress.CompressScreen
import com.videoeditor.feature.home.HomeScreen

@Composable
fun AppNavHost(nav: NavHostController) {
    val durationMs = AuroraMotion.DURATION_MEDIUM_MS
    val easing = AuroraMotion.auroraEaseOut

    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        enterTransition = {
            slideInHorizontally(
                animationSpec = tween(durationMs, easing = easing),
                initialOffsetX = { it / 8 },
            ) + fadeIn(animationSpec = tween(durationMs))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(durationMs / 2))
        },
        popEnterTransition = {
            slideInHorizontally(
                animationSpec = tween(durationMs, easing = easing),
                initialOffsetX = { -it / 8 },
            ) + fadeIn(animationSpec = tween(durationMs))
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = tween(durationMs, easing = easing),
                targetOffsetX = { it / 8 },
            ) + fadeOut(animationSpec = tween(durationMs))
        },
    ) {
        composable(Routes.HOME) {
            HomeScreen(onOpenFeature = { route -> nav.navigate(route) })
        }
        composable(Routes.COMPRESS) {
            CompressScreen(onBack = { nav.popBackStack() })
        }
    }
}
