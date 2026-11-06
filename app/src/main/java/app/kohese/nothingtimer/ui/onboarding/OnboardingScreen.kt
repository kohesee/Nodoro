@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.kohese.nothingtimer.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import androidx.compose.material3.toShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import app.kohese.nothingtimer.R
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import app.kohese.nothingtimer.ThemeMode
import app.kohese.nothingtimer.TimerSettings
import app.kohese.nothingtimer.ui.SpinningShape
import app.kohese.nothingtimer.ui.theme.NDot57
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 4

@Composable
fun OnboardingScreen(
    settings: TimerSettings,
    onSettingsChanged: (TimerSettings) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == PAGE_COUNT - 1

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                // Keeps the layout stable on the last page.
                TextButton(onClick = onFinish, enabled = !isLast) {
                    Text(if (isLast) "" else "Skip")
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> RhythmPage(settings)
                    2 -> GlyphPage()
                    else -> StylePage(settings, onSettingsChanged)
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(PAGE_COUNT) { i ->
                        val selected = pagerState.currentPage == i
                        val width by animateDpAsState(
                            if (selected) 28.dp else 10.dp,
                            spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                            label = "DotWidth"
                        )
                        val color by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            label = "DotColor"
                        )
                        Box(Modifier.height(10.dp).width(width).background(color, CircleShape))
                    }
                }
                Button(
                    onClick = {
                        if (isLast) onFinish()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(if (isLast) "Get started" else "Next", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (isLast) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PageLayout(
    hero: @Composable () -> Unit,
    title: String,
    body: String,
    content: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        hero()
        Spacer(Modifier.height(32.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        content()
    }
}

@Composable
private fun Hero(polygon: RoundedPolygon, icon: ImageVector, reverse: Boolean = false) {
    Box(contentAlignment = Alignment.Center) {
        SpinningShape(
            polygon = MaterialShapes.Sunny,
            size = 220.dp,
            color = MaterialTheme.colorScheme.secondaryContainer,
            durationMs = 40_000,
            reverse = !reverse
        )
        SpinningShape(
            polygon = polygon,
            size = 160.dp,
            color = MaterialTheme.colorScheme.primaryContainer,
            durationMs = 30_000,
            reverse = reverse
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

@Composable
private fun WelcomePage() {
    PageLayout(
        hero = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Hero(MaterialShapes.Cookie9Sided, Icons.Rounded.Timer)
                Spacer(Modifier.height(24.dp))
                Text("NODORO", fontFamily = NDot57, fontSize = 48.sp, letterSpacing = 3.sp)
            }
        },
        title = "Focus deeply. Rest properly.",
        body = "A calm Pomodoro timer with a signature look, built to bring your Nothing phone's Glyph to life."
    )
}

@Composable
private fun RhythmPage(settings: TimerSettings) {
    PageLayout(
        hero = { Hero(MaterialShapes.Clover4Leaf, Icons.Rounded.Repeat, reverse = true) },
        title = "Your rhythm",
        body = "Work in focused sessions, then recharge. After ${settings.dailyGoal} focus sessions you earn a long break. " +
            "Tweak every duration in Settings.",
    ) {
        val items = listOf(
            "Focus" to settings.focusMinutes,
            "Short break" to settings.shortBreakMinutes,
            "Long break" to settings.longBreakMinutes
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEachIndexed { i, (label, minutes) ->
                val shape = when (i) {
                    0 -> RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                    items.lastIndex -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 28.dp, bottomEnd = 28.dp)
                    else -> RoundedCornerShape(6.dp)
                }
                Surface(
                    shape = shape,
                    color = if (i == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (i == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$minutes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("min", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun GlyphPage() {
    PageLayout(
        hero = { Hero(MaterialShapes.Burst, Icons.Rounded.Lightbulb) },
        title = "Made for Glyph",
        body = "Tap the timer to swap the dial for your phone's own Glyph artwork. " +
            "On a Nothing phone, your real Glyph LEDs follow the countdown too."
    )
}

@Composable
private fun StylePage(settings: TimerSettings, onSettingsChanged: (TimerSettings) -> Unit) {
    PageLayout(
        hero = { Hero(MaterialShapes.Flower, Icons.Rounded.Palette, reverse = true) },
        title = "Make it yours",
        body = "Pick a look. You can change this any time in Settings."
    ) {
        val modes = ThemeMode.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = settings.themeMode == mode,
                    onClick = { onSettingsChanged(settings.copy(themeMode = mode)) },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size)
                ) {
                    Text(
                        when (mode) {
                            ThemeMode.SYSTEM -> "System"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        }
                    )
                }
            }
        }
    }
}

