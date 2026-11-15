package com.nuvio.tv.matrix

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import com.nuvio.tv.DrawerItem
import com.nuvio.tv.DrawerItemIcon
import com.nuvio.tv.LocalContentFocusRequester
import com.nuvio.tv.LocalSidebarExpanded
import com.nuvio.tv.R
import com.nuvio.tv.navigateToDrawerRoute
import com.nuvio.tv.ui.components.BrandWordmark
import com.nuvio.tv.ui.components.ProfileAvatarCircle
import com.nuvio.tv.ui.navigation.NuvioNavHost
import com.nuvio.tv.ui.navigation.Screen
import com.nuvio.tv.ui.theme.NuvioMotion
import com.nuvio.tv.ui.theme.NuvioTheme

/**
 * Matrix 3.1 — the sidebar is hidden and replaced by a Netflix-style top
 * bar carrying the same destinations (Home, Discover when enabled, Search,
 * Library, IPTV, Settings) plus the profile switcher. No destination
 * becomes unreachable: every drawer item is a top-bar item.
 *
 * [LocalSidebarExpanded] is always false here, so home rows stop
 * reserving sidebar space. BACK on a root route exits the app (with the
 * usual confirm-exit flow) instead of opening a sidebar; inner screens
 * keep their own BackHandlers, which take priority over this one.
 *
 * The old sidebar scaffolds in MainActivity are kept but no longer used.
 */
@Composable
fun MatrixTopBarScaffold(
    navController: NavHostController,
    startDestination: String,
    currentRoute: String?,
    rootRoutes: Set<String>,
    drawerItems: List<DrawerItem>,
    selectedDrawerRoute: String?,
    activeProfileName: String,
    activeProfileColorHex: String,
    activeProfileAvatarImageUrl: String?,
    showProfileSelector: Boolean,
    onSwitchProfile: () -> Unit,
    onNavigate: (String) -> Unit,
    onExitApp: () -> Unit,
) {
    val showTopBar = currentRoute in rootRoutes
    val contentFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = currentRoute in rootRoutes) {
        onExitApp()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalSidebarExpanded provides false,
            LocalContentFocusRequester provides contentFocusRequester,
        ) {
            NuvioNavHost(
                navController = navController,
                startDestination = startDestination,
                // The top bar is the header now: screens hide theirs.
                hideBuiltInHeaders = true,
                discoverIsRoot = Screen.Discover.route in rootRoutes,
            )
        }

        if (showTopBar) {
            MatrixTopNavBar(
                drawerItems = drawerItems,
                selectedDrawerRoute = selectedDrawerRoute,
                onDrawerItemClick = { targetRoute ->
                    keyboardController?.hide()
                    onNavigate(targetRoute)
                    navigateToDrawerRoute(
                        navController = navController,
                        currentRoute = currentRoute,
                        targetRoute = targetRoute,
                    )
                },
                activeProfileName = activeProfileName,
                activeProfileColorHex = activeProfileColorHex,
                activeProfileAvatarImageUrl = activeProfileAvatarImageUrl,
                showProfileSelector = showProfileSelector,
                onSwitchProfile = onSwitchProfile,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun MatrixTopNavBar(
    drawerItems: List<DrawerItem>,
    selectedDrawerRoute: String?,
    onDrawerItemClick: (String) -> Unit,
    activeProfileName: String,
    activeProfileColorHex: String,
    activeProfileAvatarImageUrl: String?,
    showProfileSelector: Boolean,
    onSwitchProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Gradient scrim so the bar stays legible over the hero backdrop.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.72f),
                        Color.Black.copy(alpha = 0.45f),
                        Color.Black.copy(alpha = 0f),
                    )
                )
            )
            .padding(
                start = NuvioTheme.spacing.lg,
                end = NuvioTheme.spacing.lg,
                top = NuvioTheme.spacing.md,
                bottom = NuvioTheme.spacing.lg,
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandWordmark(
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .width(132.dp)
                    .height(36.dp),
            )
            Spacer(modifier = Modifier.width(NuvioTheme.spacing.xl))
            drawerItems.forEach { item ->
                if (item.route == Screen.Iptv.route) {
                    // The Live TV pill: dark pill + white outline, like his
                    // reference photo (a logo, not a plain nav item).
                    TopNavLiveTvItem(
                        selected = selectedDrawerRoute == item.route,
                        onClick = { onDrawerItemClick(item.route) },
                    )
                } else {
                    TopNavItem(
                        label = item.label,
                        iconRes = item.iconRes,
                        icon = item.icon,
                        selected = selectedDrawerRoute == item.route,
                        onClick = { onDrawerItemClick(item.route) },
                    )
                }
                Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
            }
            Spacer(modifier = Modifier.weight(1f))
            if (showProfileSelector && activeProfileName.isNotEmpty()) {
                TopNavProfileItem(
                    profileName = activeProfileName,
                    profileColorHex = activeProfileColorHex,
                    profileAvatarImageUrl = activeProfileAvatarImageUrl,
                    onClick = onSwitchProfile,
                )
            }
        }
    }
}

/**
 * The Live TV pill in the top bar: a dark pill with a white outline and
 * "Live TV" text, matching his reference photo. Opens the IPTV screen.
 */
@Composable
private fun TopNavLiveTvItem(
    selected: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    val liveTvLabel = stringResource(R.string.topbar_live_tv)
    val itemScale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(durationMillis = NuvioMotion.tokens.durations.fast),
        label = "topNavLiveTvScale",
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .onFocusChanged { isFocused = it.hasFocus },
        colors = CardDefaults.colors(
            containerColor = Color.Black.copy(alpha = 0.55f),
            focusedContainerColor = Color.Black.copy(alpha = 0.55f),
        ),
        border = CardDefaults.border(
            border = androidx.tv.material3.Border(
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
                shape = shape,
            ),
            focusedBorder = androidx.tv.material3.Border(
                border = BorderStroke(2.dp, Color.White),
                shape = shape,
            ),
        ),
        shape = CardDefaults.shape(shape = shape),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Text(
            text = liveTvLabel,
            color = Color.White,
            style = androidx.tv.material3.MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            modifier = Modifier
                .semantics {
                    contentDescription = liveTvLabel
                    role = Role.Tab
                    this.selected = selected
                }
                .padding(
                    horizontal = NuvioTheme.spacing.md,
                    vertical = NuvioTheme.spacing.sm,
                ),
        )
    }
}

@Composable
private fun TopNavItem(
    label: String,
    iconRes: Int?,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val colors = NuvioTheme.colors
    val accentColor = NuvioTheme.palette.secondary
    val shape = RoundedCornerShape(50)
    val backgroundColorTarget = when {
        isFocused && selected -> accentColor.copy(alpha = 0.32f)
        isFocused -> Color.White.copy(alpha = 0.14f)
        selected -> accentColor.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColorTarget,
        animationSpec = tween(durationMillis = NuvioMotion.tokens.durations.fast),
        label = "topNavItemBackground",
    )
    val contentColorTarget = when {
        selected -> Color.White
        isFocused -> colors.TextPrimary
        else -> colors.TextSecondary
    }
    val animatedContentColor by animateColorAsState(
        targetValue = contentColorTarget,
        animationSpec = tween(durationMillis = NuvioMotion.tokens.durations.fast),
        label = "topNavItemContent",
    )
    val itemScale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(durationMillis = NuvioMotion.tokens.durations.fast),
        label = "topNavItemScale",
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .onFocusChanged { isFocused = it.hasFocus },
        colors = CardDefaults.colors(
            containerColor = animatedBackgroundColor,
            focusedContainerColor = animatedBackgroundColor,
        ),
        border = CardDefaults.border(border = androidx.tv.material3.Border.None),
        shape = CardDefaults.shape(shape = shape),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Row(
            modifier = Modifier
                .semantics {
                    contentDescription = label
                    role = Role.Tab
                    this.selected = selected
                }
                .padding(
                    horizontal = NuvioTheme.spacing.md,
                    vertical = NuvioTheme.spacing.sm,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DrawerItemIcon(
                iconRes = iconRes,
                icon = icon,
                tint = animatedContentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
            Text(
                text = label,
                color = animatedContentColor,
                style = androidx.tv.material3.MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TopNavProfileItem(
    profileName: String,
    profileColorHex: String,
    profileAvatarImageUrl: String?,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    val backgroundColor = if (isFocused) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Transparent
    }
    val itemScale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(durationMillis = NuvioMotion.tokens.durations.fast),
        label = "topNavProfileScale",
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .onFocusChanged { isFocused = it.hasFocus },
        colors = CardDefaults.colors(
            containerColor = backgroundColor,
            focusedContainerColor = backgroundColor,
        ),
        border = CardDefaults.border(border = androidx.tv.material3.Border.None),
        shape = CardDefaults.shape(shape = shape),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Row(
            modifier = Modifier
                .semantics {
                    contentDescription = profileName
                    role = Role.Button
                }
                .padding(
                    horizontal = NuvioTheme.spacing.md,
                    vertical = NuvioTheme.spacing.xs,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatarCircle(
                name = profileName,
                colorHex = profileColorHex,
                size = 36.dp,
                avatarImageUrl = profileAvatarImageUrl,
                imageCrossfade = false,
            )
            Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
            Text(
                text = profileName,
                color = NuvioTheme.colors.TextPrimary,
                style = androidx.tv.material3.MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
            )
        }
    }
}
