package tw.taipei.veges

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable
import tw.taipei.veges.alerts.SettingsRoute
import tw.taipei.veges.catalog.CatalogRoute
import tw.taipei.veges.designsystem.ContainerTransformDurationMillis
import tw.taipei.veges.designsystem.EmphasizedAccelerateEasing
import tw.taipei.veges.designsystem.EmphasizedDecelerateEasing
import tw.taipei.veges.designsystem.EmphasizedEasing
import tw.taipei.veges.designsystem.LocalNavAnimatedVisibilityScope
import tw.taipei.veges.designsystem.LocalSharedTransitionScope
import tw.taipei.veges.detail.DetailRoute
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.ProduceConcept
import tw.taipei.veges.home.HomeRoute

@Serializable
private data object HomeDestination

@Serializable
private data object VegetableMarketDestination

@Serializable
private data object FruitMarketDestination

@Serializable
private data object SettingsDestination

@Serializable
private data class DetailDestination(
    val conceptId: String,
    val previewName: String = "",
    val previewIllustration: String = "",
)

private data class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
)

private val TopLevelDestinations = listOf(
    TopLevelDestination(HomeDestination, HomeDestination::class, "自選", Icons.Rounded.Star),
    TopLevelDestination(
        VegetableMarketDestination,
        VegetableMarketDestination::class,
        "蔬菜市場",
        Icons.AutoMirrored.Rounded.ShowChart,
    ),
    TopLevelDestination(FruitMarketDestination, FruitMarketDestination::class, "水果市場", Icons.Rounded.Spa),
    TopLevelDestination(SettingsDestination, SettingsDestination::class, "設定", Icons.Rounded.Settings),
)

// Material fade-through for switching between top-level destinations.
private const val FadeThroughOutMillis = 90
private const val FadeThroughInMillis = 210
private val FadeThroughEnter = fadeIn(
    tween(FadeThroughInMillis, delayMillis = FadeThroughOutMillis, easing = EmphasizedDecelerateEasing),
) + scaleIn(
    tween(FadeThroughInMillis, delayMillis = FadeThroughOutMillis, easing = EmphasizedDecelerateEasing),
    initialScale = 0.96f,
)
private val FadeThroughExit = fadeOut(tween(FadeThroughOutMillis, easing = EmphasizedAccelerateEasing))

private fun NavBackStackEntry.isDetail() = destination.hasRoute(DetailDestination::class)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(): EnterTransition =
    if (targetState.isDetail()) {
        // Used only when no matching row is on screen (e.g. opened from a notification).
        fadeIn(tween(ContainerTransformDurationMillis, easing = EmphasizedDecelerateEasing)) +
            scaleIn(tween(ContainerTransformDurationMillis, easing = EmphasizedEasing), initialScale = 0.92f)
    } else {
        FadeThroughEnter
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(): ExitTransition =
    if (targetState.isDetail()) ExitTransition.KeepUntilTransitionsFinished else FadeThroughExit

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    if (initialState.isDetail()) EnterTransition.None else FadeThroughEnter

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition =
    if (initialState.isDetail()) {
        // Used when the originating row is gone, so the page shrinks away instead of morphing back.
        fadeOut(tween(ContainerTransformDurationMillis, easing = EmphasizedAccelerateEasing)) +
            scaleOut(tween(ContainerTransformDurationMillis, easing = EmphasizedEasing), targetScale = 0.9f)
    } else {
        FadeThroughExit
    }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VegesApp(
    onRequestNotificationPermission: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStack.collectAsStateWithLifecycle()
    val selectedTopLevel = backStack.lastOrNull { entry ->
        TopLevelDestinations.any { entry.destination.hasRoute(it.routeClass) }
    }?.let { entry -> TopLevelDestinations.first { entry.destination.hasRoute(it.routeClass) } }

    fun openDetail(conceptId: String, preview: ProduceConcept?) {
        navController.navigate(
            DetailDestination(
                conceptId = conceptId,
                previewName = preview?.householdName.orEmpty(),
                previewIllustration = preview?.illustrationAsset.orEmpty(),
            ),
        )
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        SharedTransitionLayout(modifier.background(MaterialTheme.colorScheme.background)) {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavHost(
                    navController = navController,
                    startDestination = HomeDestination,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { enter() },
                    exitTransition = { exit() },
                    popEnterTransition = { popEnter() },
                    popExitTransition = { popExit() },
                ) {
                    composable<HomeDestination> {
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                            HomeRoute(
                                onBrowseCatalog = { navController.navigateToTopLevel(VegetableMarketDestination) },
                                onConceptSelected = ::openDetail,
                            )
                        }
                    }
                    composable<VegetableMarketDestination> {
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                            CatalogRoute(
                                category = ProduceCategory.VEGETABLE,
                                onConceptSelected = ::openDetail,
                            )
                        }
                    }
                    composable<FruitMarketDestination> {
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                            CatalogRoute(
                                category = ProduceCategory.FRUIT,
                                onConceptSelected = ::openDetail,
                            )
                        }
                    }
                    composable<SettingsDestination> {
                        SettingsRoute()
                    }
                    composable<DetailDestination>(
                        deepLinks = listOf(navDeepLink<DetailDestination>(basePath = "veges://produce")),
                    ) { entry ->
                        val destination = entry.toRoute<DetailDestination>()
                        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                            DetailRoute(
                                conceptId = destination.conceptId,
                                previewName = destination.previewName.takeIf(String::isNotBlank),
                                previewIllustrationAsset = destination.previewIllustration
                                    .takeIf(String::isNotBlank),
                                onBack = { navController.popBackStack() },
                                onAlertSaved = onRequestNotificationPermission,
                            )
                        }
                    }
                }
            }
        }
    }

    fun onTopLevelSelected(destination: TopLevelDestination) {
        if (destination == selectedTopLevel) {
            // Re-selecting the current tab returns to its root, e.g. closes an open detail page.
            navController.popBackStack(destination.route, inclusive = false)
        } else {
            navController.navigateToTopLevel(destination.route)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp
        if (useNavigationRail) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    TopLevelDestinations.forEach { destination ->
                        NavigationRailItem(
                            selected = destination == selectedTopLevel,
                            onClick = { onTopLevelSelected(destination) },
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
                        TopLevelDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = destination == selectedTopLevel,
                                onClick = { onTopLevelSelected(destination) },
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
                Box(Modifier.padding(padding)) {
                    Content(Modifier.fillMaxSize())
                }
            }
        }
    }
}

private fun NavHostController.navigateToTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
