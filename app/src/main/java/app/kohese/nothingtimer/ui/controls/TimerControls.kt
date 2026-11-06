package app.kohese.nothingtimer.ui.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ButtonSize = 88.dp
private val JoinedGap = 2.dp
private val SeparatedGap = 14.dp
private val OuterRadiusJoined = 44.dp
private val InnerRadiusJoined = 18.dp
private val SeparatedRadius = 28.dp

/**
 * Restart / Play-Pause / Skip as large M3 Expressive FABs.
 * While idle or paused they form one joined group; while the timer is running they split
 * apart and round off. Logic stays with the caller via the three callbacks.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimerControls(
    isRunning: Boolean,
    onToggleRunning: () -> Unit,
    onRestart: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = MaterialTheme.motionScheme
    val dpSpec = motion.defaultSpatialSpec<Dp>()

    val gap by animateDpAsState(if (isRunning) SeparatedGap else JoinedGap, dpSpec, label = "controlsGap")
    val outer by animateDpAsState(if (isRunning) SeparatedRadius else OuterRadiusJoined, dpSpec, label = "controlsOuter")
    val inner by animateDpAsState(if (isRunning) SeparatedRadius else InnerRadiusJoined, dpSpec, label = "controlsInner")

    // Radii can briefly overshoot below zero with spring specs, so clamp.
    val o = outer.coerceAtLeast(0.dp)
    val i = inner.coerceAtLeast(0.dp)
    val restartShape = RoundedCornerShape(topStart = o, bottomStart = o, topEnd = i, bottomEnd = i)
    val playShape = RoundedCornerShape(i)
    val skipShape = RoundedCornerShape(topStart = i, bottomStart = i, topEnd = o, bottomEnd = o)

    val colors = MaterialTheme.colorScheme
    val noElevation = FloatingActionButtonDefaults.elevation()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap.coerceAtLeast(0.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LargeFloatingActionButton(
            onClick = onRestart,
            modifier = Modifier
                .size(ButtonSize)
                .semantics { contentDescription = "Restart timer" },
            shape = restartShape,
            containerColor = colors.surfaceVariant,
            contentColor = colors.onSurfaceVariant,
            elevation = noElevation
        ) {
            Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize))
        }

        LargeFloatingActionButton(
            onClick = onToggleRunning,
            modifier = Modifier
                .size(ButtonSize)
                .semantics { contentDescription = if (isRunning) "Pause timer" else "Start timer" },
            shape = playShape,
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            elevation = noElevation
        ) {
            AnimatedContent(
                targetState = isRunning,
                transitionSpec = {
                    (fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), initialScale = 0.6f))
                        .togetherWith(fadeOut(motion.defaultEffectsSpec()) + scaleOut(motion.defaultSpatialSpec(), targetScale = 0.6f))
                },
                label = "playPauseIcon"
            ) { running ->
                Icon(
                    imageVector = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize)
                )
            }
        }

        LargeFloatingActionButton(
            onClick = onSkip,
            modifier = Modifier
                .size(ButtonSize)
                .semantics { contentDescription = "Skip to next phase" },
            shape = skipShape,
            containerColor = colors.surfaceVariant,
            contentColor = colors.onSurfaceVariant,
            elevation = noElevation
        ) {
            Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize))
        }
    }
}
