package com.legionforge.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.ui.screens.ArmyBuilderScreen
import com.legionforge.app.ui.screens.FactionPickerScreen
import com.legionforge.app.ui.screens.HomeScreen
import com.legionforge.app.ui.screens.SettingsScreen
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination

object Routes {
    const val HOME = "home"
    const val FACTION_PICKER = "faction_picker/{system}"
    const val ARMY_BUILDER = "army_builder/{listId}"
    const val SETTINGS = "settings"
    fun factionPicker(system: GameSystem) = "faction_picker/${system.name}"
    fun armyBuilder(listId: String) = "army_builder/$listId"
}

@Composable
fun LegionForgeNavHost(navController: NavHostController = rememberNavController()) {
    val vm: ArmyBuilderViewModel = viewModel()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            androidx.compose.runtime.LaunchedEffect(Unit) { vm.loadAllCatalog(GameSystem.LEGION_V2) }
            HomeScreen(
                onNewList = { system -> navController.navigate(Routes.factionPicker(system)) },
                onOpenList = { listId -> vm.openList(listId); navController.navigate(Routes.armyBuilder(listId)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                vm = vm
            )
        }
        composable(Routes.FACTION_PICKER, arguments = listOf(navArgument("system") { type = NavType.StringType })) { entry ->
            val system = runCatching { GameSystem.valueOf(entry.arguments?.getString("system") ?: "LEGION_V2") }.getOrDefault(GameSystem.LEGION_V2)
            FactionPickerScreen(system, onFactionSelected = { factionId ->
                vm.createList(if (system == GameSystem.LEGION_V2) "Nouvelle armée" else "Nouvelle flotte", system, factionId, if (system == GameSystem.LEGION_V2) 1000 else 400) { id ->
                    navController.navigate(Routes.armyBuilder(id))
                }
            }, vm = vm)
        }
        composable(Routes.ARMY_BUILDER, arguments = listOf(navArgument("listId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("listId") ?: return@composable
            ArmyBuilderScreen(listId = id, viewModel = vm, onBack = {
                navController.navigate(Routes.HOME) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                    launchSingleTop = true
                }
            })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
