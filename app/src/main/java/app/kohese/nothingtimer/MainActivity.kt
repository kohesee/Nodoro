package app.kohese.nothingtimer
import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.kohese.nothingtimer.glyph.GlyphPhoneModel
import app.kohese.nothingtimer.glyph.InteractiveTimerDial
import app.kohese.nothingtimer.ui.ad.BottomNativeAdBanner
import app.kohese.nothingtimer.ui.controls.TimerControls
import app.kohese.nothingtimer.ui.nav.FloatingNavBar
import app.kohese.nothingtimer.ui.nav.LocalNavBarClearance
import app.kohese.nothingtimer.ui.nav.NavBarClearance
import app.kohese.nothingtimer.ui.onboarding.OnboardingScreen
import app.kohese.nothingtimer.ui.onboarding.TapHint
import app.kohese.nothingtimer.ui.pro.ProScreen
import app.kohese.nothingtimer.ui.settings.SettingsScreen
import app.kohese.nothingtimer.ui.theme.NDot57
import app.kohese.nothingtimer.ui.theme.NodoroTheme
import kotlinx.coroutines.delay

enum class AppTab { HOME, STATS, SETTINGS }

class MainActivity : ComponentActivity() {
    private val timerViewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Hands over from the starting (splash) theme to the app theme.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Consent flow + ads SDK (Google Play flavor only; a no-op in the GitHub build).
        timerViewModel.initMonetization(this)
        setContent { NodoroApp(timerViewModel) }
    }

    override fun onResume() {
        super.onResume()
        // Picks up purchases completed outside the app, pending payments, and refunds.
        timerViewModel.refreshPro()
        timerViewModel.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        // Lights go off when the app is left, unless the timer is actively counting down.
        timerViewModel.onAppBackgrounded()
    }
}

@Composable
private fun NodoroApp(timerViewModel: TimerViewModel) {
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    val timerState by timerViewModel.uiState
    var showPro by rememberSaveable { mutableStateOf(false) }
    // Kept here (not inside HomeScreen) so switching tabs doesn't reset the chosen dial / phone model.
    var showGlyphShape by rememberSaveable { mutableStateOf(false) }
    val isPro = timerState.isPro
    // Free users always get dynamic color; turning it off, accents and AMOLED are Nodoro Pro.
    val effectiveDynamic = !isPro || timerState.settings.dynamicColor
    val effectiveAmoled = isPro && timerState.settings.amoled

    // System bars follow the app's own theme (not just the OS theme) so icons stay legible.
    val systemDark = isSystemInDarkTheme()
    val isDark = when (timerState.settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val activity = LocalContext.current as ComponentActivity
    DisposableEffect(isDark) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(0, 0) { isDark },
            navigationBarStyle = SystemBarStyle.auto(0, 0) { isDark }
        )
        activity.window.isNavigationBarContrastEnforced = false
        onDispose { }
    }

    val appContext = LocalContext.current
    LaunchedEffect(timerState.proMessage) {
        timerState.proMessage?.let {
            Toast.makeText(appContext, it, Toast.LENGTH_LONG).show()
            timerViewModel.clearProMessage()
        }
    }

    NodoroTheme(
        themeMode = timerState.settings.themeMode,
        amoled = effectiveAmoled,
        dynamicColor = effectiveDynamic,
        accent = timerState.settings.accent
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            // Insets are applied manually below so content can draw behind the system bars and the floating overlay.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { _ ->
            val density = LocalDensity.current
            // Height of the floating overlay including the system navigation bar inset.
            var overlayHeight by remember { mutableStateOf(NavBarClearance) }
            Box(modifier = Modifier.fillMaxSize()) {
              // Screen content: clear of status bar, display cutout and side insets; bottom is handled via the clearance.
              Box(
                  modifier = Modifier
                      .fillMaxSize()
                      .windowInsetsPadding(
                          WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                      )
              ) {
              CompositionLocalProvider(LocalNavBarClearance provides overlayHeight) {
                when (selectedTab) {
                    AppTab.HOME -> Box(Modifier.fillMaxSize().padding(bottom = overlayHeight)) { HomeScreen(
                        state = timerState,
                        onToggleRunning = { timerViewModel.toggleRunning(activity) },
                        onRestart = timerViewModel::restart,
                        onSkip = timerViewModel::skip,
                        onDialHintDone = timerViewModel::markDialHintSeen,
                        showGlyphShape = showGlyphShape,
                        onShowGlyphShapeChange = { showGlyphShape = it }
                    ) }
                    AppTab.STATS -> StatsScreen(
                        state = timerState
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        state = timerState,
                        onSettingsChanged = timerViewModel::updateSettings,
                        onResetStats = timerViewModel::resetStats,
                        onOpenPro = { showPro = true }
                    )
                }
              }
              }
                // Floating overlay: sits above the system navigation bar / gesture area and respects cutouts.
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .onSizeChanged { overlayHeight = with(density) { it.height.toDp() } }
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                        )
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FloatingNavBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        dynamicColor = effectiveDynamic,
                        glyphEnabled = timerState.settings.glyphEnabled,
                        onToggleGlyph = timerViewModel::toggleGlyphEnabled
                    )
                    if (!isPro) {
                        BottomNativeAdBanner(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 12.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showPro,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut()
                ) {
                    BackHandler { showPro = false }
                    ProScreen(
                        state = timerState,
                        onBuy = { timerViewModel.purchasePro(activity) },
                        onWatchAd = { timerViewModel.watchAdForPass(activity) },
                        onRestore = timerViewModel::restorePro,
                        onClose = { showPro = false }
                    )
                }

                AnimatedVisibility(visible = !timerState.onboardingDone, exit = fadeOut() + slideOutVertically { -it / 8 }) {
                    OnboardingScreen(
                        settings = timerState.settings,
                        onSettingsChanged = timerViewModel::updateSettings,
                        onFinish = {
                            timerViewModel.completeOnboarding()
                            val name = timerState.glyphDeviceName
                            val isNothing = name != "Standard Android" && name != "Non-Nothing device"
                            if (!isNothing) {
                                Toast.makeText(appContext, "No Nothing Phone Detected", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: TimerUiState,
    onToggleRunning: () -> Unit,
    onRestart: () -> Unit,
    onSkip: () -> Unit,
    onDialHintDone: () -> Unit,
    showGlyphShape: Boolean,
    onShowGlyphShapeChange: (Boolean) -> Unit,
) {
    val totalSeconds = when (state.phase) {
        TimerPhase.FOCUS -> state.settings.focusMinutes * 60
        TimerPhase.SHORT_BREAK -> state.settings.shortBreakMinutes * 60
        TimerPhase.LONG_BREAK -> state.settings.longBreakMinutes * 60
    }.coerceAtLeast(1)
    val progress = state.secondsLeft.toFloat() / totalSeconds
    val isPaused = !state.isRunning && state.secondsLeft < totalSeconds && state.secondsLeft > 0
    val title = when (state.phase) {
        TimerPhase.FOCUS -> "FOCUS"
        TimerPhase.SHORT_BREAK -> "SHORT BREAK"
        TimerPhase.LONG_BREAK -> "LONG BREAK"
    }


    val activeModel = remember(state.glyphDeviceName) {
        GlyphPhoneModel.fromDeviceName(state.glyphDeviceName)
    }
    // Starts on the normal circular timer; the glyph artwork is one tap away.
    val showDialHint = state.onboardingDone && !state.dialHintSeen && !showGlyphShape
    LaunchedEffect(state.onboardingDone, state.dialHintSeen) {
        if (state.onboardingDone && !state.dialHintSeen) {
            delay(10_000)
            onDialHintDone()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sized in dp (not sp) so a large system font scale can't push the logo off narrow screens.
                    val logoSp = with(LocalDensity.current) { 58.dp.toSp() }
                    Text(
                        text = "NODORO",
                        fontFamily = NDot57,
                        fontWeight = FontWeight.Normal,
                        fontSize = logoSp,
                        letterSpacing = 3.sp,
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.titleMedium
                    )

                }
                Spacer(Modifier.height(18.dp))
                // Transparent pill with just an outline.
                Box(
                    modifier = Modifier
                        .border(1.5.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

            }

            // Dial takes the leftover height so the header and the controls are never squeezed.
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
            InteractiveTimerDial(
                secondsLeft = state.secondsLeft,
                progress = progress,
                isPaused = isPaused,
                isRunning = state.isRunning,
                isBlinkLightOn = state.isBlinkLightOn,
                isEndingBlink = state.isEndingBlink,
                glyphBrightness = if (state.isPro) state.settings.glyphBrightness else 1f,
                glyphReverse = state.isPro && state.settings.glyphReverse,
                currentModel = activeModel,
                progressStep = 1f / totalSeconds,
                showGlyphShape = showGlyphShape,
                onToggleDialShape = {
                    onShowGlyphShapeChange(!showGlyphShape)
                    onDialHintDone()
                }
            )
            if (showDialHint) TapHint()
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.upNextLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                TimerControls(
                    isRunning = state.isRunning,
                    onToggleRunning = onToggleRunning,
                    onRestart = onRestart,
                    onSkip = onSkip
                )
            }
        }
    }
}

@Composable
private fun M3BarChart(
    data: List<ChartBarData>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    highlightLabelColor: Color = MaterialTheme.colorScheme.onSurface,
    maxHeight: Dp = 130.dp
) {
    // Scale to the tallest bar so it always reaches the top of the chart.
    val maxMinutes = data.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    // Room above each bar for its value label.
    val barAreaMax = maxHeight - 20.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { item ->
                val fraction = if (maxMinutes > 0) (item.minutes.toFloat() / maxMinutes).coerceIn(0.04f, 1f) else 0.04f
                val isHighlighted = item.isHighlighted
                val isZero = item.minutes == 0

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    val valueLabel = when {
                        isZero -> ""
                        item.minutes >= 60 -> {
                            val h = item.minutes / 60
                            val m = item.minutes % 60
                            if (m > 0) "${h}h${m}m" else "${h}h"
                        }
                        else -> "${item.minutes}m"
                    }

                    Text(
                        text = valueLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                        color = if (isHighlighted) highlightLabelColor else labelColor,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))

                    val barHeight by animateDpAsState(
                        targetValue = barAreaMax * fraction,
                        animationSpec = tween(600),
                        label = "BarHeight"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(barHeight.coerceAtLeast(0.dp))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(
                                if (isHighlighted) highlightColor
                                else if (isZero) barColor.copy(alpha = 0.25f)
                                else barColor
                            )
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { item ->
                Text(
                    text = item.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (item.isHighlighted) FontWeight.Bold else FontWeight.Normal,
                    color = if (item.isHighlighted) highlightLabelColor else labelColor
                )
            }
        }
    }
}

@Composable
private fun StatsScreen(state: TimerUiState) {
    val stats = state.stats
    val weekTotalMins = stats.weekData.sumOf { it.minutes }
    val monthTotalMins = stats.monthData.sumOf { it.minutes }
    val yearTotalMins = stats.yearData.sumOf { it.minutes }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp + LocalNavBarClearance.current),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Text(
            "Stats",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // 1. Today (2 boxes with Xh Xm format)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Focus time box
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Focus",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatHoursMinutes(stats.todayFocusMinutes),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.completedSessions} / ${state.settings.dailyGoal} sessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Break time box
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Break",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatHoursMinutes(stats.todayBreakMinutes),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Rest & recharge",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. This week (Material 3 Chart)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("This week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Total ${formatHoursMinutes(weekTotalMins)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    M3BarChart(data = stats.weekData)
                }
            }
        }

        // 3. This month (Material 3 Chart)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("This month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Total ${formatHoursMinutes(monthTotalMins)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    M3BarChart(data = stats.monthData)
                }
            }
        }

        // 4. This year (Material 3 Chart)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("This year", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Total ${formatHoursMinutes(yearTotalMins)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    M3BarChart(data = stats.yearData)
                }
            }
        }

        // 5. Lifetime
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Lifetime", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LifetimeStatRow("Total focus time", formatHoursMinutes(stats.lifetimeFocusMinutes))
                    LifetimeStatRow("Total break time", formatHoursMinutes(stats.lifetimeBreakMinutes))
                    LifetimeStatRow("Completed sessions", "${stats.lifetimeSessions}")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LifetimeStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
    }
}
