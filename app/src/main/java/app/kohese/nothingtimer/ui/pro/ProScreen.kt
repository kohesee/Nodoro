@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kohese.nothingtimer.ui.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.toShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import app.kohese.nothingtimer.R
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kohese.nothingtimer.TimerUiState
import app.kohese.nothingtimer.ui.SpinningShape
import app.kohese.nothingtimer.ui.theme.NDot57

private data class ProFeature(val icon: ImageVector, val title: String, val detail: String)

private val FEATURES = listOf(
    ProFeature(Icons.Rounded.Block, "No ads. Ever.", "Every banner and interstitial, gone for good"),
    ProFeature(Icons.Rounded.BrightnessMedium, "Glyph brightness", "Dial your phone's LEDs from subtle to full blast"),
    ProFeature(Icons.Rounded.SwapHoriz, "Reverse progress", "Choose whether the Glyph ring fills up or drains down"),
    ProFeature(Icons.Rounded.Palette, "Accent colors", "Turn off dynamic color and pick a major accent"),
    ProFeature(Icons.Rounded.DarkMode, "AMOLED black", "True black backgrounds for dark mode"),
    ProFeature(Icons.Rounded.Favorite, "Support indie dev", "One small payment keeps Nodoro growing")
)

private fun segment(index: Int, count: Int): Shape {
    val top = if (index == 0) 28.dp else 6.dp
    val bottom = if (index == count - 1) 28.dp else 6.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

private fun formatRemaining(ms: Long): String {
    val totalMin = (ms / 60_000L).coerceAtLeast(0)
    return "${totalMin / 60}h ${totalMin % 60}m"
}
@Composable
fun ProScreen(
    state: TimerUiState,
    onBuy: () -> Unit,
    onWatchAd: () -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val passActive = state.passRemainingMs > 0
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                FilledTonalIconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SpinningShape(
                        polygon = MaterialShapes.Clover8Leaf,
                        size = 210.dp,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        durationMs = 36_000
                    )
                    Image(
                        painter = painterResource(R.drawable.nodoro_pro_logo),
                        contentDescription = "Nodoro Pro",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(140.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text("NODORO PRO", fontFamily = NDot57, fontSize = 38.sp, letterSpacing = 2.sp, maxLines = 1, softWrap = false)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Pay once. Yours forever.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    FEATURES.forEachIndexed { i, f ->
                        Surface(
                            shape = segment(i, FEATURES.size),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(f.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(f.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        f.detail,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.ownsPro) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(
                            Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "You're Pro. Thank you!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onBuy,
                        enabled = !state.proBusy && !state.proPending,
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                    ) {
                        if (state.proBusy) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(
                                if (state.proPending) "Payment pending…" else "Unlock forever for ${state.proPrice ?: "\u20B999"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val canWatch = !passActive && state.passGrantsLeft > 0
                    OutlinedButton(
                        onClick = onWatchAd,
                        enabled = canWatch && state.adReady,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Rounded.PlayCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                passActive -> "Pro pass active · ${formatRemaining(state.passRemainingMs)} left"
                                state.passGrantsLeft <= 0 -> "No free passes left this month"
                                !state.adReady -> "Loading video…"
                                else -> "Watch a video · 24h Pro"
                            },
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        "${state.passGrantsLeft} of 7 free passes left this month",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    TextButton(onClick = onRestore) { Text("Restore purchase") }

                }
            }
        }
    }
}

