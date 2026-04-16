package com.mazzika.lyrics.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mazzika.lyrics.ui.theme.LocalStudioTokens
import com.mazzika.lyrics.ui.theme.goldBrush

private data class NavTab(
    val label: String,
    val outlineIcon: ImageVector,
    val filledIcon: ImageVector,
    val screen: Screen,
)

private val navTabs = listOf(
    NavTab("Accueil", NavIconHome, NavIconHomeFilled, Screen.Home),
    NavTab("Catalogue", NavIconGrid, NavIconGridFilled, Screen.Catalog),
    NavTab("Dossiers", NavIconFolder, NavIconFolderFilled, Screen.Folders),
    NavTab("Session", NavIconBroadcast, NavIconBroadcastFilled, Screen.Sync),
)

/**
 * Indicator Bar bottom navigation (Studio Moderne).
 *
 *  - `--nav-bg` background color
 *  - 1dp gold gradient line at the top edge
 *  - active tab: 3dp gold indicator bar above + gold-gradient filled icon + gold-gradient label
 *  - inactive: outline icon 26dp stroke 2, dim text color
 */
@Composable
fun BottomNavBar(navController: NavController) {
    val tokens = LocalStudioTokens.current
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.navBg)
            .drawBehind {
                val inset = 24.dp.toPx()
                drawLine(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.5f to tokens.gold.copy(alpha = 0.6f),
                        1f to Color.Transparent,
                    ),
                    start = Offset(inset, 0f),
                    end = Offset(size.width - inset, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 0.dp, bottom = 10.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Top,
        ) {
            navTabs.forEach { tab ->
                val selected = isTabSelected(currentRoute, tab.screen)
                NavTabItem(
                    tab = tab,
                    selected = selected,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (!selected) {
                            navController.navigate(tab.screen.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        }
    }
}

private fun isTabSelected(currentRoute: String?, tabScreen: Screen): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == tabScreen.route) return true
    return when (tabScreen) {
        Screen.Folders -> currentRoute.startsWith("folder/")
        Screen.Sync -> currentRoute == Screen.ReaderSync.route
        else -> false
    }
}

@Composable
private fun NavTabItem(
    tab: NavTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalStudioTokens.current
    val gold = tokens.goldBrush()

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Indicator bar (3dp, 24dp wide) flush to the top edge
        Box(
            modifier = Modifier
                .size(width = 24.dp, height = 3.dp)
                .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                .background(if (selected) gold else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))),
        )

        Spacer(Modifier.height(6.dp))

        // Icon
        Box(
            modifier = Modifier.size(26.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = tab.filledIcon,
                    contentDescription = tab.label,
                    tint = Color.Black, // overwritten by gold brush overlay
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = gold, blendMode = BlendMode.SrcIn)
                        },
                )
            } else {
                Icon(
                    imageVector = tab.outlineIcon,
                    contentDescription = tab.label,
                    tint = tokens.textDim,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Label
        if (selected) {
            Text(
                text = tab.label,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = gold, blendMode = BlendMode.SrcIn)
                    },
            )
        } else {
            Text(
                text = tab.label,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                color = tokens.textDim,
            )
        }
    }
}
