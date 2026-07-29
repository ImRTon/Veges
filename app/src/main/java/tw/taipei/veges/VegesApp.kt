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
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import tw.taipei.veges.alerts.AlertsRoute
import tw.taipei.veges.catalog.CatalogRoute
import tw.taipei.veges.detail.DetailRoute
import tw.taipei.veges.home.HomeRoute

@Serializable
private data object HomeDestination

@Serializable
private data object VegetableMarketDestination

@Serializable
private data object FruitMarketDestination

@Serializable
private data class AlertsDestination(
    val conceptId: String = "",
    val basis: String = "",
)

@Serializable
private data class DetailDestination(val conceptId: String)

private data class TopLevelDestination(
    val route: Any,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun VegesApp() {
    val navController = rememberNavController()
    val destinations = listOf(
        TopLevelDestination(HomeDestination, "自選", Icons.Rounded.Star),
        TopLevelDestination(VegetableMarketDestination, "蔬菜市場", Icons.AutoMirrored.Rounded.ShowChart),
        TopLevelDestination(FruitMarketDestination, "水果市場", Icons.Rounded.Spa),
        TopLevelDestination(AlertsDestination(), "提醒", Icons.Rounded.Notifications),
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
                HomeRoute(
                    onBrowseCatalog = { navController.navigate(VegetableMarketDestination) },
                    onConceptSelected = { navController.navigate(DetailDestination(it)) },
                )
            }
            composable<VegetableMarketDestination> {
                CatalogRoute(
                    category = tw.taipei.veges.domain.ProduceCategory.VEGETABLE,
                    onConceptSelected = { navController.navigate(DetailDestination(it)) },
                )
            }
            composable<FruitMarketDestination> {
                CatalogRoute(
                    category = tw.taipei.veges.domain.ProduceCategory.FRUIT,
                    onConceptSelected = { navController.navigate(DetailDestination(it)) },
                )
            }
            composable<AlertsDestination> { entry ->
                val destination = entry.toRoute<AlertsDestination>()
                AlertsRoute(
                    conceptId = destination.conceptId.takeIf { it.isNotBlank() },
                    basis = destination.basis.takeIf { it.isNotBlank() }
                        ?.let(tw.taipei.veges.domain.MarketBasis::valueOf),
                )
            }
            composable<DetailDestination>(
                deepLinks = listOf(navDeepLink<DetailDestination>(basePath = "veges://produce")),
            ) { entry ->
                DetailRoute(
                    conceptId = entry.toRoute<DetailDestination>().conceptId,
                    onSetAlert = { conceptId, basis ->
                        navController.navigate(AlertsDestination(conceptId, basis.name))
                    },
                )
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
                            icon = {
                                Icon(
                                    destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
                            modifier = Modifier.testTag("top-level-${destination.label}"),
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
                                icon = {
                                    Icon(
                                        destination.icon,
                                        contentDescription = destination.label,
                                    )
                                },
                                modifier = Modifier.testTag("top-level-${destination.label}"),
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
