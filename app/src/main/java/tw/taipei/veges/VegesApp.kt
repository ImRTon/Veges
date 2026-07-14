package tw.taipei.veges

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import tw.taipei.veges.alerts.AlertsRoute
import tw.taipei.veges.catalog.CatalogRoute
import tw.taipei.veges.detail.DetailRoute
import tw.taipei.veges.home.HomeRoute

@Serializable
private data object HomeDestination

@Serializable
private data object CatalogDestination

@Serializable
private data object AlertsDestination

@Serializable
private data class DetailDestination(val conceptId: String)

private data class TopLevelDestination(
    val route: Any,
    val label: String,
)

@Composable
fun VegesApp() {
    val navController = rememberNavController()
    val destinations = listOf(
        TopLevelDestination(HomeDestination, "追蹤"),
        TopLevelDestination(CatalogDestination, "蔬果"),
        TopLevelDestination(AlertsDestination, "提醒"),
    )
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        NavHost(
            navController = navController,
            startDestination = HomeDestination,
            modifier = modifier,
        ) {
            composable<HomeDestination> {
                HomeRoute(onBrowseCatalog = { navController.navigate(CatalogDestination) })
            }
            composable<CatalogDestination> {
                CatalogRoute(onConceptSelected = { navController.navigate(DetailDestination(it)) })
            }
            composable<AlertsDestination> { AlertsRoute() }
            composable<DetailDestination> { entry ->
                DetailRoute(conceptId = entry.toRoute<DetailDestination>().conceptId)
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp
        if (useNavigationRail) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    destinations.forEach { destination ->
                        NavigationRailItem(
                            selected = currentRoute == destination.route::class.qualifiedName,
                            onClick = { navController.navigate(destination.route) },
                            icon = { Text(destination.label.take(1)) },
                            label = { Text(destination.label) },
                        )
                    }
                }
                Content(Modifier.weight(1f))
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        destinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route::class.qualifiedName,
                                onClick = { navController.navigate(destination.route) },
                                icon = { Text(destination.label.take(1)) },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                },
            ) { padding ->
                Content(Modifier.padding(padding))
            }
        }
    }
}
