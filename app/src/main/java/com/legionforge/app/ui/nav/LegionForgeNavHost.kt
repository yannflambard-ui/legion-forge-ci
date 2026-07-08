package com.legionforge.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.legionforge.app.ui.screens.ArmyBuilderScreen
import com.legionforge.app.ui.screens.FactionPickerScreen
import com.legionforge.app.ui.screens.HomeScreen

object Routes {
    const val HOME = "home"
    const val FACTION_PICKER = "faction_picker"
    const val ARMY_BUILDER = "army_builder/{factionId}"

    fun armyBuilder(factionId: String) = "army_builder/$factionId"
}

@Composable
fun LegionForgeNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onNewList = { navController.navigate(Routes.FACTION_PICKER) })
        }
        composable(Routes.FACTION_PICKER) {
            FactionPickerScreen(
                onFactionSelected = { factionId ->
                    navController.navigate(Routes.armyBuilder(factionId))
                }
            )
        }
        composable(Routes.ARMY_BUILDER) { backStackEntry ->
            val factionId = backStackEntry.arguments?.getString("factionId") ?: return@composable
            ArmyBuilderScreen(factionId = factionId, listName = "Nouvelle liste")
        }
    }
}
