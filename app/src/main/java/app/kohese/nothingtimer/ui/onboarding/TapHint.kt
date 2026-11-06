package app.kohese.nothingtimer.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * One-time hint shown over the circular dial: a touch icon slides in, taps, and a ripple ring
 * expands, showing that tapping the timer switches it to the phone's Glyph artwork.
 * It doesn't consume touches, so the dial underneath still receives the tap.
 */
@Composable
fun TapHint(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "TapHint")
    // 0..1 loop: approach (0-.35), press (.35-.5), ripple (.5-1)
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "TapHintT"
    )
    val approach = (t / 0.35f).coerceIn(0f, 1f)
    val press = if (t < 0.35f) 0f else if (t < 0.5f) (t - 0.35f) / 0.15f else 1f
    val ripple = ((t - 0.5f) / 0.5f).coerceIn(0f, 1f)
    val ringColor = MaterialTheme.colorScheme.primary

    // Pure decoration: no background, no pointer handlers, so every tap falls through to the dial below.
    val iconColor = MaterialTheme.colorScheme.onBackground
    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(220.dp)) {
            if (t >= 0.5f) {
                drawCircle(
                    color = ringColor.copy(alpha = (1f - ripple) * 0.6f),
                    radius = size.minDimension / 2f * (0.2f + 0.8f * ripple),
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }
        Icon(
            Icons.Rounded.TouchApp,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(112.dp)
                .offset(x = (24 * (1f - approach)).dp, y = (32 * (1f - approach)).dp)
                .alpha(0.35f + 0.65f * approach)
                .scale(1f - 0.2f * press * (1f - ripple))
        )
    }
}
