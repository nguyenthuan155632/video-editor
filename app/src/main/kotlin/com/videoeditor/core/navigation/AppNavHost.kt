package com.videoeditor.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.videoeditor.feature.compress.CompressScreen
import com.videoeditor.feature.home.HomeScreen

@Composable
fun AppNavHost(nav: NavHostController) {
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onOpenFeature = { route -> nav.navigate(route) })
        }
        composable(Routes.COMPRESS) {
            CompressScreen(onBack = { nav.popBackStack() })
        }
    }
}