package com.example.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.AddEditMemberRoute
import com.example.ui.navigation.EventsRoute
import com.example.ui.navigation.GalleryRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.MemberDetailRoute
import com.example.ui.navigation.MembersRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.navigation.TreeRoute
import com.example.ui.screens.AddEditMemberScreen
import com.example.ui.screens.EventsScreen
import com.example.ui.screens.FamilyTreeScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemberDetailScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.SettingsScreen
import kotlin.reflect.KClass

private data class TopLevelDestination(
    val label: String,
    val route: Any,
    val routeClass: KClass<*>,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        label = "Home",
        route = HomeRoute,
        routeClass = HomeRoute::class,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    ),
    TopLevelDestination(
        label = "Members",
        route = MembersRoute,
        routeClass = MembersRoute::class,
        selectedIcon = Icons.Filled.Groups,
        unselectedIcon = Icons.Outlined.Groups,
        testTag = "nav_members"
    ),
    TopLevelDestination(
        label = "Tree",
        route = TreeRoute,
        routeClass = TreeRoute::class,
        selectedIcon = Icons.Filled.AccountTree,
        unselectedIcon = Icons.Outlined.AccountTree,
        testTag = "nav_tree"
    ),
    TopLevelDestination(
        label = "Events",
        route = EventsRoute,
        routeClass = EventsRoute::class,
        selectedIcon = Icons.Filled.Celebration,
        unselectedIcon = Icons.Outlined.Celebration,
        testTag = "nav_events"
    ),
    TopLevelDestination(
        label = "Gallery",
        route = GalleryRoute,
        routeClass = GalleryRoute::class,
        selectedIcon = Icons.Filled.PhotoLibrary,
        unselectedIcon = Icons.Outlined.PhotoLibrary,
        testTag = "nav_gallery"
    )
)

@Composable
fun FamilyHubApp(widthSizeClass: WindowWidthSizeClass) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isTopLevel = topLevelDestinations.any { dest ->
        currentDestination?.hasRoute(dest.routeClass) == true
    }
    val useRail = widthSizeClass != WindowWidthSizeClass.Compact

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (!useRail && isTopLevel) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentDestination?.hasRoute(dest.routeClass) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTopLevel(dest.route) },
                            modifier = Modifier.testTag(dest.testTag),
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = {
                                Text(
                                    text = dest.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (useRail && (isTopLevel || currentDestination?.hasRoute(SettingsRoute::class) == true)) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentDestination?.hasRoute(dest.routeClass) == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTopLevel(dest.route) },
                            modifier = Modifier.testTag("${dest.testTag}_rail"),
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    val settingsSelected = currentDestination?.hasRoute(SettingsRoute::class) == true
                    NavigationRailItem(
                        selected = settingsSelected,
                        onClick = { navController.navigate(SettingsRoute) { launchSingleTop = true } },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() }
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            onOpenMember = { id -> navController.navigate(MemberDetailRoute(id)) },
                            onAddMember = { navController.navigate(AddEditMemberRoute()) },
                            onOpenMembers = { navigateTopLevel(MembersRoute) },
                            onOpenTree = { navigateTopLevel(TreeRoute) },
                            onOpenEvents = { navigateTopLevel(EventsRoute) },
                            onOpenGallery = { navigateTopLevel(GalleryRoute) },
                            onOpenSettings = { navController.navigate(SettingsRoute) }
                        )
                    }
                    composable<MembersRoute> {
                        MembersScreen(
                            onOpenMember = { id -> navController.navigate(MemberDetailRoute(id)) },
                            onAddMember = { navController.navigate(AddEditMemberRoute()) },
                            onOpenSettings = { navController.navigate(SettingsRoute) }
                        )
                    }
                    composable<TreeRoute> {
                        FamilyTreeScreen(
                            onOpenMember = { id -> navController.navigate(MemberDetailRoute(id)) },
                            onAddMember = { navController.navigate(AddEditMemberRoute()) },
                            onOpenSettings = { navController.navigate(SettingsRoute) }
                        )
                    }
                    composable<EventsRoute> {
                        EventsScreen(
                            onOpenMember = { id -> navController.navigate(MemberDetailRoute(id)) },
                            onOpenSettings = { navController.navigate(SettingsRoute) }
                        )
                    }
                    composable<GalleryRoute> {
                        GalleryScreen(
                            onOpenMember = { id -> navController.navigate(MemberDetailRoute(id)) },
                            onOpenSettings = { navController.navigate(SettingsRoute) }
                        )
                    }
                    composable<SettingsRoute> {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }
                    composable<MemberDetailRoute> {
                        MemberDetailScreen(
                            onBack = { navController.popBackStack() },
                            onEditMember = { id -> navController.navigate(AddEditMemberRoute(id)) },
                            onOpenRelative = { id -> navController.navigate(MemberDetailRoute(id)) }
                        )
                    }
                    composable<AddEditMemberRoute> {
                        AddEditMemberScreen(
                            onBack = { navController.popBackStack() },
                            onSaved = { savedId ->
                                navController.popBackStack()
                                navController.navigate(MemberDetailRoute(savedId)) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenExistingMember = { existingId ->
                                navController.popBackStack()
                                navController.navigate(MemberDetailRoute(existingId))
                            }
                        )
                    }
                }
            }
        }
    }
}
