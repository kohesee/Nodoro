package app.kohese.nothingtimer.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally

import androidx.compose.foundation.background

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.kohese.nothingtimer.AppTab
import app.kohese.nothingtimer.ui.theme.Poppins

/** Space screens should leave at the bottom so content can scroll clear of the floating bar. */
val NavBarClearance = 104.dp

/** Measured height of the floating bottom overlay (nav bar + ad), so screens can pad their content. */
val LocalNavBarClearance = compositionLocalOf { NavBarClearance }

private val PillHeight = 60.dp
private val PillRadius = 30.dp
private val PillGap = 8.dp

private data class NavDestination(val tab: AppTab, val icon: ImageVector, val label: String)

private val destinations = listOf(
    NavDestination(AppTab.HOME, Icons.Rounded.Timer, "Timer"),
    NavDestination(AppTab.STATS, MonitoringIcon, "Stats"),
    NavDestination(AppTab.SETTINGS, Icons.Rounded.Settings, "Settings")
)

private data class NavColors(
    val selectedContainer: Color,
    val selectedContent: Color,
    val groupContainer: Color,
    val groupContent: Color,
    val outline: Color
)

@Composable
private fun navColors(dynamicColor: Boolean): NavColors {
    val scheme = MaterialTheme.colorScheme
    if (dynamicColor) {
        return NavColors(
            selectedContainer = scheme.primary,
            selectedContent = scheme.onPrimary,
            groupContainer = scheme.secondaryContainer,
            groupContent = scheme.onSecondaryContainer,
            outline = scheme.outline.copy(alpha = 0.25f)
        )
    }
    // Strict black & white: selected pill is inverted against the group pill.
    val dark = scheme.background.luminance() < 0.5f
    val ink = Color.Black
    val paper = Color.White
    return if (dark) {
        NavColors(paper, ink, ink, paper, paper.copy(alpha = 0.22f))
    } else {
        NavColors(ink, paper, paper, ink, ink.copy(alpha = 0.14f))
    }
}

/**
 * Floating bottom navigation. The selected destination is its own pill (icon + label);
 * the remaining destinations that sit next to each other share one joined background pill.
 * Selection changes animate colors, corner shapes, gaps and label width.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    dynamicColor: Boolean,
    glyphEnabled: Boolean,
    onToggleGlyph: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = navColors(dynamicColor)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        destinations.forEachIndexed { index, dest ->
            val selected = dest.tab == selectedTab
            val prevUnselected = index > 0 && destinations[index - 1].tab != selectedTab
            val nextUnselected = index < destinations.lastIndex && destinations[index + 1].tab != selectedTab
            NavPill(
                destination = dest,
                selected = selected,
                joinStart = !selected && prevUnselected,
                joinEnd = !selected && nextUnselected,
                hasGapBefore = index > 0 && !(prevUnselected && !selected),
                colors = colors,
                onClick = { onTabSelected(dest.tab) }
            )
        }
        GlyphTorchButton(enabled = glyphEnabled, colors = colors, onClick = onToggleGlyph)
    }
}

/** Glyph on/off toggle: always its own circle, next to the tab pills, styled like them. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GlyphTorchButton(
    enabled: Boolean,
    colors: NavColors,
    onClick: () -> Unit
) {
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val container by animateColorAsState(
        if (enabled) colors.selectedContainer else colors.groupContainer, effects, label = "torchContainer"
    )
    val content by animateColorAsState(
        if (enabled) colors.selectedContent else colors.groupContent, effects, label = "torchContent"
    )
    Box(
        modifier = Modifier
            .padding(start = PillGap)
            .shadow(8.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(container)
            .border(1.dp, colors.outline, CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Switch
                contentDescription = if (enabled) "Turn off Glyph lights" else "Turn on Glyph lights"
            }
            .size(PillHeight),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (enabled) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(28.dp).rotate(45f)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavPill(
    destination: NavDestination,
    selected: Boolean,
    joinStart: Boolean,
    joinEnd: Boolean,
    hasGapBefore: Boolean,
    colors: NavColors,
    onClick: () -> Unit
) {
    val motion = MaterialTheme.motionScheme
    val effects = motion.defaultEffectsSpec<Color>()
    val spatialDp = motion.defaultSpatialSpec<Dp>()

    val container by animateColorAsState(
        if (selected) colors.selectedContainer else colors.groupContainer, effects, label = "navContainer"
    )
    val content by animateColorAsState(
        if (selected) colors.selectedContent else colors.groupContent, effects, label = "navContent"
    )
    val gap by animateDpAsState(if (hasGapBefore) PillGap else 0.dp, spatialDp, label = "navGap")
    val startRadius by animateDpAsState(if (joinStart) 0.dp else PillRadius, spatialDp, label = "navStart")
    val endRadius by animateDpAsState(if (joinEnd) 0.dp else PillRadius, spatialDp, label = "navEnd")

    val shape = RoundedCornerShape(
        topStart = startRadius.coerceAtLeast(0.dp),
        bottomStart = startRadius.coerceAtLeast(0.dp),
        topEnd = endRadius.coerceAtLeast(0.dp),
        bottomEnd = endRadius.coerceAtLeast(0.dp)
    )

    Row(
        modifier = Modifier
            .padding(start = gap.coerceAtLeast(0.dp))
            // Clip the shadow on edges shared with a neighbouring pill so no shadow shows between them.
            .drawWithContent {
                clipRect(
                    left = if (joinStart) 0f else -4000f,
                    right = if (joinEnd) size.width else size.width + 4000f,
                    top = -4000f,
                    bottom = size.height + 4000f
                ) { this@drawWithContent.drawContent() }
            }
            .shadow(4.dp, shape)
            .drawWithContent {
                drawContent()
                // Outline, skipped on edges where this pill joins a neighbouring group pill.
                val w = 1.dp.toPx()
                clipRect(
                    left = if (joinStart) w / 2f else -w,
                    right = size.width - (if (joinEnd) w / 2f else -w),
                    top = -w,
                    bottom = size.height + w
                ) {
                    drawPath(
                        path = Path().apply {
                                                    val s = CornerRadius(startRadius.coerceAtLeast(0.dp).toPx())
                                                    val e = CornerRadius(endRadius.coerceAtLeast(0.dp).toPx())
                                                    addRoundRect(RoundRect(0f, 0f, size.width, size.height, s, e, e, s))
                                                },
                        color = colors.outline,
                        style = Stroke(w)
                    )
                }
            }
            .clip(shape)
            .background(container)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Tab
                this.selected = selected
                contentDescription = destination.label
            }
            .defaultMinSize(minWidth = 64.dp, minHeight = PillHeight)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.width(28.dp)
        )
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
            exit = shrinkHorizontally(motion.defaultSpatialSpec()) + fadeOut(motion.defaultEffectsSpec())
        ) {
            Row {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = destination.label,
                    color = content,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Poppins,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
            }
        }
    }
}
