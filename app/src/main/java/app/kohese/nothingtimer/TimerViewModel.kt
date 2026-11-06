package app.kohese.nothingtimer

import android.app.Activity
import android.app.Application
import app.kohese.nothingtimer.monetization.REWARD_MAX_PER_MONTH
import app.kohese.nothingtimer.monetization.createMonetization
import android.content.Context
import android.content.SharedPreferences
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.kohese.nothingtimer.glyph.GlyphController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Accent used when dynamic color is off (a Nodoro Pro option). MONO is pure black and white. */
enum class AccentColor(val label: String, val hue: Float) {
    MONO("Mono", -1f),
    RED("Red", 2f),
    ORANGE("Orange", 24f),
    GREEN("Green", 135f),
    TEAL("Teal", 175f),
    BLUE("Blue", 217f),
    PURPLE("Purple", 270f),
    PINK("Pink", 330f)
}

data class TimerSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val dailyGoal: Int = 4,
    val alarmSound: Boolean = true,
    val alarmSoundUri: String? = null,
    val alarmSeconds: Int = 15,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoled: Boolean = false,
    val dynamicColor: Boolean = true,
    val accent: AccentColor = AccentColor.MONO,
    val language: String = "system",
    val glyphEnabled: Boolean = true,
    val glyphBrightness: Float = 1.0f,
    val glyphReverse: Boolean = false
)

enum class TimerPhase { FOCUS, SHORT_BREAK, LONG_BREAK }

data class ChartBarData(
    val label: String,
    val minutes: Int,
    val isHighlighted: Boolean = false,
    val secondaryMinutes: Int = 0
)

data class StatsSummary(
    val todayFocusMinutes: Int = 0,
    val todayBreakMinutes: Int = 0,
    val todaySessions: Int = 0,
    val weekData: List<ChartBarData> = emptyList(),
    val monthData: List<ChartBarData> = emptyList(),
    val yearData: List<ChartBarData> = emptyList(),
    val lifetimeFocusMinutes: Int = 0,
    val lifetimeBreakMinutes: Int = 0,
    val lifetimeSessions: Int = 0
)

data class TimerUiState(
    val settings: TimerSettings,
    val phase: TimerPhase = TimerPhase.FOCUS,
    val secondsLeft: Int = settings.focusMinutes * 60,
    val isRunning: Boolean = false,
    val isReset: Boolean = true,
    val isBlinkLightOn: Boolean = true,
    val isEndingBlink: Boolean = false,
    val completedSessions: Int = 0,
    val glyphDeviceName: String = "Non-Nothing device",
    val stats: StatsSummary = StatsSummary(),
    val isPro: Boolean = false,
    /** True when Pro was bought (not just a rewarded 24h pass). */
    val ownsPro: Boolean = false,
    /** Milliseconds left on the rewarded-ad Pro pass; 0 = none. */
    val passRemainingMs: Long = 0L,
    /** Rewarded passes still available this calendar month. */
    val passGrantsLeft: Int = REWARD_MAX_PER_MONTH,
    val adReady: Boolean = false,
    /** GitHub build: everything is unlocked and there is nothing to buy. */
    val fullVersion: Boolean = false,
    val proPrice: String? = null,
    val proBusy: Boolean = false,
    val proPending: Boolean = false,
    val proMessage: String? = null,
    val onboardingDone: Boolean = false,
    val dialHintSeen: Boolean = false
) {
    val upNextLabel: String
        get() = when (phase) {
            TimerPhase.FOCUS -> {
                val isLongBreak = (completedSessions + 1) % settings.dailyGoal.coerceAtLeast(1) == 0
                if (isLongBreak) {
                    "Up next: Long Break (${settings.longBreakMinutes} min)"
                } else {
                    "Up next: Short Break (${settings.shortBreakMinutes} min)"
                }
            }
            TimerPhase.SHORT_BREAK, TimerPhase.LONG_BREAK -> {
                "Up next: Focus (${settings.focusMinutes} min)"
            }
        }
}

/** Owns all timer state so the countdown survives activity recreation. */
class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences: SharedPreferences = application.getSharedPreferences(PREFERENCES_NAME, 0)
    private var deadlineElapsedMs: Long? = null
    private var tickerJob: Job? = null
    private var activeAlarmRingtone: Ringtone? = null
    private var alarmLimitJob: Job? = null
    private val glyphController = GlyphController(application)
    /** Ads / billing / Pro. Real in the Play flavor, "everything unlocked" in the GitHub flavor. */
    private val monetization = createMonetization(application, viewModelScope)

    var uiState = mutableStateOf(
        monetization.status.value.let { pro ->
            TimerUiState(
                settings = loadSettings(preferences),
                glyphDeviceName = glyphController.deviceName,
                stats = loadStats(preferences),
                completedSessions = preferences.getInt("stats_sessions_${LocalDate.now()}", 0),
                isPro = pro.isPro,
                ownsPro = pro.ownsPro,
                fullVersion = monetization.isFullVersion,
                onboardingDone = preferences.getBoolean("onboarding_done", false),
                dialHintSeen = preferences.getBoolean("dial_hint_seen", false)
            )
        }
    )
        private set

    init {
        applyGlyph(uiState.value.settings, uiState.value.isPro)
        viewModelScope.launch {
            monetization.status.collect { pro ->
                val cur = uiState.value
                uiState.value = cur.copy(
                    isPro = pro.isPro,
                    ownsPro = pro.ownsPro,
                    passRemainingMs = pro.passRemainingMs,
                    passGrantsLeft = pro.passGrantsLeft,
                    adReady = pro.adReady,
                    proPrice = pro.price,
                    proBusy = pro.busy,
                    proPending = pro.pending,
                    proMessage = pro.message
                )
                if (pro.isPro != cur.isPro) applyGlyph(cur.settings, pro.isPro)
            }
        }
        viewModelScope.launch {
            glyphController.isBlinkLightOn.collect { on ->
                uiState.value = uiState.value.copy(isBlinkLightOn = on)
            }
        }
        viewModelScope.launch {
            glyphController.isEndingBlink.collect { ending ->
                uiState.value = uiState.value.copy(isEndingBlink = ending)
            }
        }
        refreshRemaining()
    }

    /** Start/pause. A due interstitial (free Play users, between sessions) is shown before a start. */
    fun toggleRunning(activity: Activity) {
        if (uiState.value.isRunning) pause()
        else monetization.showInterstitialIfDue(activity) { start() }
    }

    /** Consent flow + ads SDK; call once from the activity. */
    fun initMonetization(activity: Activity) = monetization.init(activity)

    fun toggleGlyphEnabled() {
        val next = !uiState.value.settings.glyphEnabled
        updateSettings(uiState.value.settings.copy(glyphEnabled = next))
    }

    fun start() {
        stopAlarmSound()
        glyphController.stopBlinking()
        glyphController.stopAnimation()
        if (uiState.value.isRunning) return
        triggerSubtleClickHaptic()
        val seconds = uiState.value.secondsLeft.coerceAtLeast(1)
        deadlineElapsedMs = SystemClock.elapsedRealtime() + seconds * 1_000L
        uiState.value = uiState.value.copy(isRunning = true, isReset = false)
        val total = durationFor(uiState.value.phase, uiState.value.settings).coerceAtLeast(1)
        glyphController.updateProgress(seconds.toFloat() / total)
        startTicker()
    }

    fun pause() {
        stopAlarmSound()
        triggerSubtleClickHaptic()
        refreshRemaining()
        deadlineElapsedMs = null
        uiState.value = uiState.value.copy(isRunning = false, isReset = false)
        tickerJob?.cancel()
        val total = durationFor(uiState.value.phase, uiState.value.settings).coerceAtLeast(1)
        val pausedProgress = (uiState.value.secondsLeft.toFloat() / total).coerceIn(0f, 1f)
        glyphController.startBlinking(pausedProgress)
    }

    fun restart() {
        stopAlarmSound()
        glyphController.stopBlinking()
        deadlineElapsedMs = null
        tickerJob?.cancel()
        val total = durationFor(uiState.value.phase, uiState.value.settings).coerceAtLeast(1)
        val currentProgress = (uiState.value.secondsLeft.toFloat() / total).coerceIn(0f, 1f)
        val focusDuration = uiState.value.settings.focusMinutes * 60
        uiState.value = uiState.value.copy(
            isRunning = false,
            isReset = true,
            phase = TimerPhase.FOCUS,
            secondsLeft = focusDuration
        )
        glyphController.animateJumpToFullRing(
            fromProgress = currentProgress,
            onStep = { triggerSubtleTickHaptic() }
        )
    }

    fun skip() {
        stopAlarmSound()
        glyphController.stopBlinking()
        deadlineElapsedMs = null
        tickerJob?.cancel()
        val total = durationFor(uiState.value.phase, uiState.value.settings).coerceAtLeast(1)
        val currentProgress = (uiState.value.secondsLeft.toFloat() / total).coerceIn(0f, 1f)
        val next = nextPhase(uiState.value.phase)
        val nextDuration = durationFor(next, uiState.value.settings)
        uiState.value = uiState.value.copy(
            isRunning = false,
            isReset = true,
            phase = next,
            secondsLeft = nextDuration
        )
        glyphController.animateJumpToFullRing(
            fromProgress = currentProgress,
            onStep = { triggerSubtleTickHaptic() }
        )
    }

    /** Glyph lighting is free; brightness and reverse direction are Nodoro Pro features. */
    private fun applyGlyph(settings: TimerSettings, isPro: Boolean) {
        glyphController.setEnabled(settings.glyphEnabled)
        glyphController.setBrightness(if (isPro) settings.glyphBrightness else 1f)
        glyphController.setReversed(isPro && settings.glyphReverse)
    }

    fun purchasePro(activity: Activity) = monetization.purchase(activity)
    fun watchAdForPass(activity: Activity) = monetization.watchRewardedAd(activity)
    /** Re-checks purchases and the rewarded pass (resume, restore). */
    fun refreshPro() = monetization.refresh()
    fun restorePro() {
        monetization.refresh()
        uiState.value = uiState.value.copy(proMessage = "Checking Google Play for your purchase…")
    }
    fun clearProMessage() {
        monetization.clearMessage()
        uiState.value = uiState.value.copy(proMessage = null)
    }    /** App left the foreground: Glyph lights go off unless the timer is actually counting down. */
    fun onAppBackgrounded() {
        if (uiState.value.isRunning) return
        glyphController.turnOff()
    }

    /** App back in the foreground: restore the lights for the current state. */
    fun onAppForegrounded() {
        val s = uiState.value
        if (!s.settings.glyphEnabled || s.isRunning) return
        val total = durationFor(s.phase, s.settings).coerceAtLeast(1)
        val progress = (s.secondsLeft.toFloat() / total).coerceIn(0f, 1f)
        if (!s.isReset) glyphController.startBlinking(progress) else glyphController.updateProgress(1f)
    }

    fun completeOnboarding() {
        preferences.edit().putBoolean("onboarding_done", true).apply()
        uiState.value = uiState.value.copy(onboardingDone = true)
    }

    fun markDialHintSeen() {
        if (uiState.value.dialHintSeen) return
        preferences.edit().putBoolean("dial_hint_seen", true).apply()
        uiState.value = uiState.value.copy(dialHintSeen = true)
    }

    /** Wipes every recorded statistic (daily and lifetime) and the session counter. */
    fun resetStats() {
        val editor = preferences.edit()
        for (key in preferences.all.keys) {
            if (key.startsWith("stats_")) editor.remove(key)
        }
        editor.apply()
        uiState.value = uiState.value.copy(completedSessions = 0, stats = loadStats(preferences))
    }

    fun updateSettings(settings: TimerSettings) {
        val current = uiState.value
        val canEditTimer = current.isReset && !current.isRunning
        val effectiveSettings = if (!canEditTimer) {
            // Keep timer durations locked while timer is running or paused
            settings.copy(
                focusMinutes = current.settings.focusMinutes,
                shortBreakMinutes = current.settings.shortBreakMinutes,
                longBreakMinutes = current.settings.longBreakMinutes,
                dailyGoal = current.settings.dailyGoal
            )
        } else {
            settings
        }

        saveSettings(preferences, effectiveSettings)
        applyGlyph(effectiveSettings, current.isPro)
        uiState.value = current.copy(settings = effectiveSettings)

        if (canEditTimer) {
            val newDuration = durationFor(current.phase, effectiveSettings)
            uiState.value = uiState.value.copy(secondsLeft = newDuration)
            glyphController.updateProgress(1.0f)
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive && uiState.value.isRunning) {
                // 50ms ticker interval for silky-smooth sub-segment glyph updates (20 fps)
                delay(50)
                refreshRemaining()
            }
        }
    }

    private fun refreshRemaining() {
        val deadline = deadlineElapsedMs ?: return
        val now = SystemClock.elapsedRealtime()
        val remainingMs = (deadline - now).coerceAtLeast(0L)
        val remainingSeconds = ((remainingMs + 999L) / 1_000L).toInt()
        val totalSeconds = durationFor(uiState.value.phase, uiState.value.settings).coerceAtLeast(1)
        val fractionalProgress = (remainingMs.toFloat() / (totalSeconds * 1_000f)).coerceIn(0f, 1f)

        if (remainingMs <= 0L) {
            completePhase()
        } else {
            if (uiState.value.secondsLeft != remainingSeconds) {
                uiState.value = uiState.value.copy(secondsLeft = remainingSeconds)
            }
            glyphController.updateProgress(fractionalProgress)
        }
    }

    private fun completePhase() {
        deadlineElapsedMs = null
        tickerJob?.cancel()
        playAlarmSound()
        triggerVibration()
        glyphController.animateCompletion()

        val current = uiState.value
        val isFocus = current.phase == TimerPhase.FOCUS
        val completed = if (isFocus) current.completedSessions + 1 else current.completedSessions

        // Persist session statistics
        val focusAdded = if (isFocus) current.settings.focusMinutes else 0
        val breakAdded = if (!isFocus) {
            if (current.phase == TimerPhase.SHORT_BREAK) current.settings.shortBreakMinutes else current.settings.longBreakMinutes
        } else 0
        recordSessionStats(focusAdded, breakAdded, isFocus)
        if (isFocus) monetization.onFocusSessionCompleted()

        val next = if (isFocus) {
            if (completed % current.settings.dailyGoal.coerceAtLeast(1) == 0) TimerPhase.LONG_BREAK else TimerPhase.SHORT_BREAK
        } else {
            TimerPhase.FOCUS
        }

        val updatedStats = loadStats(preferences)
        uiState.value = current.copy(
            isRunning = false,
            isReset = true,
            phase = next,
            secondsLeft = durationFor(next, current.settings),
            completedSessions = completed,
            stats = updatedStats
        )
    }

    private fun getVibrator(): Vibrator? {
        val app = getApplication<Application>()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator ?: (@Suppress("DEPRECATION") app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun triggerSubtleClickHaptic() {
        if (!uiState.value.settings.vibration) return
        try {
            val vibrator = getVibrator() ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } catch (_: Exception) {
                    vibrator.vibrate(VibrationEffect.createOneShot(25L, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(25L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25L)
            }
        } catch (e: Exception) {
            Log.e("TimerViewModel", "Haptic click error", e)
        }
    }

    private fun triggerSubtleTickHaptic() {
        if (!uiState.value.settings.vibration) return
        try {
            val vibrator = getVibrator() ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } catch (_: Exception) {
                    vibrator.vibrate(VibrationEffect.createOneShot(12L, 140))
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(12L, 140))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(12L)
            }
        } catch (e: Exception) {
            Log.e("TimerViewModel", "Haptic tick error", e)
        }
    }

    private fun recordSessionStats(focusMinutes: Int, breakMinutes: Int, isSessionCompleted: Boolean) {
        val todayStr = LocalDate.now().toString()
        val currentFocus = preferences.getInt("stats_focus_$todayStr", 0)
        val currentBreak = preferences.getInt("stats_break_$todayStr", 0)
        val currentSessions = preferences.getInt("stats_sessions_$todayStr", 0)

        val lifetimeFocus = preferences.getInt("stats_lifetime_focus", 0)
        val lifetimeBreak = preferences.getInt("stats_lifetime_break", 0)
        val lifetimeSessions = preferences.getInt("stats_lifetime_sessions", 0)

        preferences.edit()
            .putInt("stats_focus_$todayStr", currentFocus + focusMinutes)
            .putInt("stats_break_$todayStr", currentBreak + breakMinutes)
            .putInt("stats_sessions_$todayStr", currentSessions + (if (isSessionCompleted) 1 else 0))
            .putInt("stats_lifetime_focus", lifetimeFocus + focusMinutes)
            .putInt("stats_lifetime_break", lifetimeBreak + breakMinutes)
            .putInt("stats_lifetime_sessions", lifetimeSessions + (if (isSessionCompleted) 1 else 0))
            .apply()
    }

    fun stopAlarmSound() {
        alarmLimitJob?.cancel()
        alarmLimitJob = null
        try {
            activeAlarmRingtone?.stop()
            activeAlarmRingtone = null
        } catch (_: Exception) {}
        glyphController.stopBlinking()
    }

    private fun playAlarmSound() {
        if (!uiState.value.settings.alarmSound) return
        try {
            val uri = uiState.value.settings.alarmSoundUri?.let { Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            stopAlarmSound()
            val limitMs = uiState.value.settings.alarmSeconds.coerceIn(1, 30) * 1_000L
            activeAlarmRingtone = RingtoneManager.getRingtone(getApplication(), uri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
                play()
            }
            // Hard cap on how long the alarm may ring.
            alarmLimitJob = viewModelScope.launch {
                delay(limitMs)
                try { activeAlarmRingtone?.stop() } catch (_: Exception) {}
                activeAlarmRingtone = null
            }
        } catch (_: Exception) {}
    }

    private fun triggerVibration() {
        if (!uiState.value.settings.vibration) return
        try {
            val vibrator = getVibrator() ?: return
            if (!vibrator.hasVibrator()) return
            val timings = longArrayOf(0, 400, 200, 400)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        } catch (e: Exception) {
            Log.e("TimerViewModel", "Vibration error", e)
        }
    }

    private fun nextPhase(phase: TimerPhase) = if (phase == TimerPhase.FOCUS) TimerPhase.SHORT_BREAK else TimerPhase.FOCUS

    private fun durationFor(phase: TimerPhase, settings: TimerSettings): Int = when (phase) {
        TimerPhase.FOCUS -> settings.focusMinutes * 60
        TimerPhase.SHORT_BREAK -> settings.shortBreakMinutes * 60
        TimerPhase.LONG_BREAK -> settings.longBreakMinutes * 60
    }

    override fun onCleared() {
        stopAlarmSound()
        tickerJob?.cancel()
        glyphController.cleanup()
        monetization.release()
        super.onCleared()
    }
}

private const val PREFERENCES_NAME = "nodoro_preferences"

private fun loadSettings(preferences: SharedPreferences): TimerSettings {
    val themeModeStr = preferences.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
    val themeMode = try {
        ThemeMode.valueOf(themeModeStr)
    } catch (_: Exception) {
        if (preferences.getBoolean("dark_mode", false)) ThemeMode.DARK else ThemeMode.SYSTEM
    }

    return TimerSettings(
        focusMinutes = preferences.getInt("focus_minutes", 25),
        shortBreakMinutes = preferences.getInt("short_break_minutes", 5),
        longBreakMinutes = preferences.getInt("long_break_minutes", 15),
        dailyGoal = preferences.getInt("daily_goal", 4),
        alarmSound = preferences.getBoolean("alarm_sound", true),
        alarmSoundUri = preferences.getString("alarm_sound_uri", null),
        alarmSeconds = preferences.getInt("alarm_seconds", 15).coerceIn(1, 30),
        sound = preferences.getBoolean("sound", true),
        vibration = preferences.getBoolean("vibration", true),
        themeMode = themeMode,
        amoled = preferences.getBoolean("amoled", false),
        dynamicColor = preferences.getBoolean("dynamic_color", true),
        accent = try { AccentColor.valueOf(preferences.getString("accent", null) ?: "MONO") } catch (_: Exception) { AccentColor.MONO },
        language = preferences.getString("app_language", "system") ?: "system",
        glyphEnabled = preferences.getBoolean("glyph_enabled", true),
        glyphBrightness = preferences.getFloat("glyph_brightness", 1.0f),
        glyphReverse = preferences.getBoolean("glyph_reverse", false)
    )
}

private fun saveSettings(preferences: SharedPreferences, settings: TimerSettings) {
    preferences.edit()
        .putInt("focus_minutes", settings.focusMinutes)
        .putInt("short_break_minutes", settings.shortBreakMinutes)
        .putInt("long_break_minutes", settings.longBreakMinutes)
        .putInt("daily_goal", settings.dailyGoal)
        .putBoolean("alarm_sound", settings.alarmSound)
        .putString("alarm_sound_uri", settings.alarmSoundUri)
        .putInt("alarm_seconds", settings.alarmSeconds)
        .putBoolean("sound", settings.sound)
        .putBoolean("vibration", settings.vibration)
        .putString("theme_mode", settings.themeMode.name)
        .putBoolean("dark_mode", settings.themeMode == ThemeMode.DARK)
        .putBoolean("amoled", settings.amoled)
        .putBoolean("dynamic_color", settings.dynamicColor)
        .putString("accent", settings.accent.name)
        .putString("app_language", settings.language)
        .putBoolean("glyph_enabled", settings.glyphEnabled)
        .putFloat("glyph_brightness", settings.glyphBrightness)
        .putBoolean("glyph_reverse", settings.glyphReverse)
        .apply()
}

fun formatHoursMinutes(totalMinutes: Int): String {
    val h = (totalMinutes / 60).coerceAtLeast(0)
    val m = (totalMinutes % 60).coerceAtLeast(0)
    return "${h}h ${m}m"
}

private fun loadStats(preferences: SharedPreferences): StatsSummary {
    // Clear out any previously seeded fake dummy stats if present
    if (preferences.getBoolean("stats_seeded", false)) {
        val editor = preferences.edit()
        for (key in preferences.all.keys) {
            if (key.startsWith("stats_")) {
                editor.remove(key)
            }
        }
        editor.remove("stats_seeded")
        editor.apply()
    }

    val today = LocalDate.now()
    val todayStr = today.toString()

    val todayFocus = preferences.getInt("stats_focus_$todayStr", 0)
    val todayBreak = preferences.getInt("stats_break_$todayStr", 0)
    val todaySessions = preferences.getInt("stats_sessions_$todayStr", 0)

    val lifetimeFocus = preferences.getInt("stats_lifetime_focus", 0)
    val lifetimeBreak = preferences.getInt("stats_lifetime_break", 0)
    val lifetimeSessions = preferences.getInt("stats_lifetime_sessions", 0)

    // 1. This week: 7 days Monday -> Sunday
    val currentDayOfWeek = today.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
    val startOfWeek = today.minusDays((currentDayOfWeek - 1).toLong())
    val weekDays = (0..6).map { dayOffset ->
        val date = startOfWeek.plusDays(dayOffset.toLong())
        val dateStr = date.toString()
        val isToday = date == today
        val dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val focusMins = preferences.getInt("stats_focus_$dateStr", 0)
        ChartBarData(
            label = dayLabel,
            minutes = focusMins,
            isHighlighted = isToday
        )
    }

    // 2. This month: weekly buckets (W1, W2, W3, W4, W5)
    val daysInMonth = today.lengthOfMonth()
    val monthWeeks = mutableListOf<ChartBarData>()
    var dayCursor = 1
    var weekIndex = 1
    while (dayCursor <= daysInMonth) {
        val weekEnd = minOf(dayCursor + 6, daysInMonth)
        var bucketMins = 0
        var includesToday = false
        for (d in dayCursor..weekEnd) {
            val date = today.withDayOfMonth(d)
            if (date == today) includesToday = true
            val dateStr = date.toString()
            bucketMins += preferences.getInt("stats_focus_$dateStr", 0)
        }
        monthWeeks.add(
            ChartBarData(
                label = "W$weekIndex",
                minutes = bucketMins,
                isHighlighted = includesToday
            )
        )
        dayCursor += 7
        weekIndex++
    }

    // 3. This year: 12 months (Jan -> Dec)
    val currentMonthVal = today.monthValue
    val currentYear = today.year
    val yearMonths = (1..12).map { monthVal ->
        val monthObj = Month.of(monthVal)
        val monthLabel = monthObj.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val isCurrentMonth = monthVal == currentMonthVal
        var monthTotalMins = 0
        val daysInThatMonth = monthObj.length(today.isLeapYear)
        for (d in 1..daysInThatMonth) {
            val dateStr = "%04d-%02d-%02d".format(currentYear, monthVal, d)
            monthTotalMins += preferences.getInt("stats_focus_$dateStr", 0)
        }
        ChartBarData(
            label = monthLabel,
            minutes = monthTotalMins,
            isHighlighted = isCurrentMonth
        )
    }

    return StatsSummary(
        todayFocusMinutes = todayFocus,
        todayBreakMinutes = todayBreak,
        todaySessions = todaySessions,
        weekData = weekDays,
        monthData = monthWeeks,
        yearData = yearMonths,
        lifetimeFocusMinutes = lifetimeFocus,
        lifetimeBreakMinutes = lifetimeBreak,
        lifetimeSessions = lifetimeSessions
    )
}
















































