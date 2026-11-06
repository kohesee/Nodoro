package app.kohese.nothingtimer.glyph

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class GlyphController(private val context: Context) {
    private val controllerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var blinkingJob: Job? = null
    private var animationJob: Job? = null

    private val _isBlinkLightOn = MutableStateFlow(true)
    val isBlinkLightOn: StateFlow<Boolean> = _isBlinkLightOn.asStateFlow()

    private val _isEndingBlink = MutableStateFlow(false)
    val isEndingBlink: StateFlow<Boolean> = _isEndingBlink.asStateFlow()

    private var glyphManager: GlyphManager? = null
    private var isSessionOpen = false
    private var targetDeviceKey: String? = null
    private var channelSegments: List<Int> = emptyList()
    private var lastProgress: Float = -1f
    private var isEnabled: Boolean = true
    private var lastFullCount: Int = -1
    private var lastPartialLight: Int = -1
    private var lastWasOff: Boolean = false

    var brightness: Float = 1.0f
        private set

    var isReversed: Boolean = false
        private set

    var deviceName: String = "Non-Nothing device"
        private set

    val isSupportedDevice: Boolean
        get() = targetDeviceKey != null

    init {
        setupDeviceMapping()
        initManager()
    }

    private fun setupDeviceMapping() {
        when {
            Common.is20111() -> {
                targetDeviceKey = Glyph.DEVICE_20111
                deviceName = "Nothing Phone (1)"
                // D channels (vertical exclamation timer bar at bottom): D1_1 (bottom) to D1_8 (top)
                // Unfills from top (D1_8) down to bottom (D1_1) as progress decreases
                channelSegments = listOf(
                    Glyph.Code_20111.D1_1,
                    Glyph.Code_20111.D1_2,
                    Glyph.Code_20111.D1_3,
                    Glyph.Code_20111.D1_4,
                    Glyph.Code_20111.D1_5,
                    Glyph.Code_20111.D1_6,
                    Glyph.Code_20111.D1_7,
                    Glyph.Code_20111.D1_8
                )
            }
            Common.is22111() -> {
                targetDeviceKey = Glyph.DEVICE_22111
                deviceName = "Nothing Phone (2)"
                // Only C1 channels (16 segments): C1_1 starts dimming first up to C1_16
                channelSegments = listOf(
                    Glyph.Code_22111.C1_16, Glyph.Code_22111.C1_15, Glyph.Code_22111.C1_14, Glyph.Code_22111.C1_13,
                    Glyph.Code_22111.C1_12, Glyph.Code_22111.C1_11, Glyph.Code_22111.C1_10, Glyph.Code_22111.C1_9,
                    Glyph.Code_22111.C1_8, Glyph.Code_22111.C1_7, Glyph.Code_22111.C1_6, Glyph.Code_22111.C1_5,
                    Glyph.Code_22111.C1_4, Glyph.Code_22111.C1_3, Glyph.Code_22111.C1_2, Glyph.Code_22111.C1_1
                )
            }
            Common.is23111() || Common.is23113() -> {
                targetDeviceKey = if (Common.is23113()) Glyph.DEVICE_23113 else Glyph.DEVICE_23111
                deviceName = if (Common.is23113()) "Nothing Phone (2a) Plus" else "Nothing Phone (2a)"
                // Only C channels around camera: C_24 dims first down to C_1
                channelSegments = listOf(
                    Glyph.Code_23111.C_1, Glyph.Code_23111.C_2, Glyph.Code_23111.C_3, Glyph.Code_23111.C_4,
                    Glyph.Code_23111.C_5, Glyph.Code_23111.C_6, Glyph.Code_23111.C_7, Glyph.Code_23111.C_8,
                    Glyph.Code_23111.C_9, Glyph.Code_23111.C_10, Glyph.Code_23111.C_11, Glyph.Code_23111.C_12,
                    Glyph.Code_23111.C_13, Glyph.Code_23111.C_14, Glyph.Code_23111.C_15, Glyph.Code_23111.C_16,
                    Glyph.Code_23111.C_17, Glyph.Code_23111.C_18, Glyph.Code_23111.C_19, Glyph.Code_23111.C_20,
                    Glyph.Code_23111.C_21, Glyph.Code_23111.C_22, Glyph.Code_23111.C_23, Glyph.Code_23111.C_24
                )
            }
            Common.is24111() -> {
                targetDeviceKey = Glyph.DEVICE_24111
                deviceName = "Nothing Phone (3a) / (3a) Pro"
                // All channels around camera: circular glyph pattern
                channelSegments = listOf(
                    Glyph.Code_24111.A_1, Glyph.Code_24111.A_2, Glyph.Code_24111.A_3, Glyph.Code_24111.A_4,
                    Glyph.Code_24111.A_5, Glyph.Code_24111.A_6, Glyph.Code_24111.A_7, Glyph.Code_24111.A_8,
                    Glyph.Code_24111.A_9, Glyph.Code_24111.A_10, Glyph.Code_24111.A_11,
                    Glyph.Code_24111.B_1, Glyph.Code_24111.B_2, Glyph.Code_24111.B_3, Glyph.Code_24111.B_4, Glyph.Code_24111.B_5,
                    Glyph.Code_24111.C_1, Glyph.Code_24111.C_2, Glyph.Code_24111.C_3, Glyph.Code_24111.C_4,
                    Glyph.Code_24111.C_5, Glyph.Code_24111.C_6, Glyph.Code_24111.C_7, Glyph.Code_24111.C_8,
                    Glyph.Code_24111.C_9, Glyph.Code_24111.C_10, Glyph.Code_24111.C_11, Glyph.Code_24111.C_12,
                    Glyph.Code_24111.C_13, Glyph.Code_24111.C_14, Glyph.Code_24111.C_15, Glyph.Code_24111.C_16,
                    Glyph.Code_24111.C_17, Glyph.Code_24111.C_18, Glyph.Code_24111.C_19, Glyph.Code_24111.C_20
                )
            }
            Common.is25111() -> {
                targetDeviceKey = Glyph.DEVICE_25111
                deviceName = "Nothing Phone (4a)"
                // Reversed line (A_6 to A_1): A_1 dims first up to A_6
                channelSegments = listOf(
                    Glyph.Code_25111.A_6, Glyph.Code_25111.A_5, Glyph.Code_25111.A_4,
                    Glyph.Code_25111.A_3, Glyph.Code_25111.A_2, Glyph.Code_25111.A_1
                )
            }
            Common.is25131() -> {
                targetDeviceKey = Glyph.DEVICE_25131
                deviceName = "Nothing Phone (4b)"
                // Normal line (A_1 to A_4)
                channelSegments = listOf(
                    Glyph.Code_25131.A_1, Glyph.Code_25131.A_2, Glyph.Code_25131.A_3, Glyph.Code_25131.A_4
                )
            }
            else -> {
                targetDeviceKey = null
                deviceName = "Standard Android"
            }
        }
    }

    private fun initManager() {
        try {
            glyphManager = GlyphManager.getInstance(context)
            glyphManager?.init(object : GlyphManager.Callback {
                override fun onServiceConnected(componentName: ComponentName?) {
                    try {
                        val registered = if (targetDeviceKey != null) {
                            glyphManager?.register(targetDeviceKey) ?: false
                        } else {
                            glyphManager?.register() ?: false
                        }
                        if (registered) {
                            glyphManager?.openSession()
                            isSessionOpen = true
                            if (lastProgress >= 0f && isEnabled) {
                                updateProgress(lastProgress)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("GlyphController", "Failed to register or open session", e)
                    }
                }

                override fun onServiceDisconnected(componentName: ComponentName?) {
                    isSessionOpen = false
                }
            })
        } catch (e: Exception) {
            Log.e("GlyphController", "GlyphManager initialization error", e)
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            // Stop hardware only — do NOT stop blinking (SVG blink must continue independently)
            stopAnimation()
            turnOffHardware()
        } else {
            // Idle timer has no progress yet: show the full ring so the lights visibly come on.
            lastWasOff = true
            updateProgress(if (lastProgress >= 0f) lastProgress else 1.0f)
        }
    }

    fun setBrightness(value: Float) {
        val clamped = value.coerceIn(0.10f, 1.0f)
        if (kotlin.math.abs(brightness - clamped) > 0.01f) {
            brightness = clamped
            if (isEnabled && lastProgress >= 0f) {
                lastWasOff = true
                lastFullCount = -1
                lastPartialLight = -1
                updateProgress(lastProgress)
            }
        }
    }

    fun setReversed(reversed: Boolean) {
        if (isReversed != reversed) {
            isReversed = reversed
            if (isEnabled && lastProgress >= 0f) {
                lastWasOff = true
                lastFullCount = -1
                lastPartialLight = -1
                updateProgress(lastProgress)
            }
        }
    }

    fun updateProgress(progress: Float) {
        lastProgress = progress
        if (!isEnabled || !isSessionOpen || channelSegments.isEmpty()) return
        val gm = glyphManager ?: return
        try {
            val total = channelSegments.size
            val effectiveProgress = if (isReversed) {
                1.0f - progress.coerceIn(0f, 1f)
            } else {
                progress.coerceIn(0f, 1f)
            }
            val maxLight = (4000f * brightness).roundToInt().coerceIn(100, 4000)

            if (effectiveProgress <= 0.0005f) {
                if (!lastWasOff) {
                    gm.turnOff()
                    lastWasOff = true
                    lastFullCount = -1
                    lastPartialLight = -1
                }
                return
            }

            // Sub-segment fractional brightness for silky smooth light transitions
            val scaled = effectiveProgress * total
            val fullCount = scaled.toInt().coerceIn(0, total)
            val remainder = scaled - fullCount
            val partialLight = if (fullCount < total && remainder > 0.01f) {
                (remainder * maxLight.toFloat()).roundToInt().coerceIn(0, maxLight)
            } else {
                0
            }

            // Skip redundant IPC transmissions when delta is unnoticeable
            if (!lastWasOff && fullCount == lastFullCount && kotlin.math.abs(partialLight - lastPartialLight) < 60) {
                return
            }

            lastWasOff = false
            lastFullCount = fullCount
            lastPartialLight = partialLight

            val builder = gm.glyphFrameBuilder
            for (i in 0 until fullCount) {
                builder.buildChannel(channelSegments[i], maxLight)
            }
            if (fullCount < total && partialLight > 0) {
                builder.buildChannel(channelSegments[fullCount], partialLight)
            }
            gm.toggle(builder.build())
        } catch (e: Exception) {
            Log.e("GlyphController", "Failed to update smooth glyph progress", e)
        }
    }

    /** Continuously blinks the Glyph lights at the paused progress level until resumed or next action. */
    fun startBlinking(pausedProgress: Float? = null) {
        stopAnimation()
        stopBlinking()
        val rawP = (pausedProgress ?: lastProgress).coerceIn(0f, 1f)
        val p = if (isReversed) (1.0f - rawP).coerceIn(0f, 1f) else rawP
        val maxLight = (4000f * brightness).roundToInt().coerceIn(100, 4000)
        val gm = glyphManager
        blinkingJob = controllerScope.launch {
            var lightOn = true
            while (isActive) {
                _isBlinkLightOn.value = lightOn
                // Only drive hardware if enabled at this moment
                val canDriveHardware = isEnabled && isSessionOpen && channelSegments.isNotEmpty() && gm != null
                if (canDriveHardware) {
                    try {
                        if (lightOn) {
                            val total = channelSegments.size
                            val effectiveP = if (p <= 0.01f) {
                                (1f / total).coerceAtMost(0.15f)
                            } else p
                            val scaled = (effectiveP * total).coerceIn(0f, total.toFloat())
                            val fullCount = scaled.toInt().coerceIn(0, total)
                            val remainder = scaled - fullCount
                            val partialLight = if (fullCount < total && remainder > 0.01f) {
                                (remainder * maxLight.toFloat()).roundToInt().coerceIn(0, maxLight)
                            } else 0

                            val builder = gm.glyphFrameBuilder
                            for (i in 0 until fullCount) {
                                builder.buildChannel(channelSegments[i], maxLight)
                            }
                            if (fullCount < total && partialLight > 0) {
                                builder.buildChannel(channelSegments[fullCount], partialLight)
                            }
                            gm.toggle(builder.build())
                        } else {
                            gm.turnOff()
                        }
                    } catch (e: Exception) {
                        Log.e("GlyphController", "Blink error", e)
                    }
                }
                lightOn = !lightOn
                delay(500)
            }
        }
    }

    /** 3-cycle completion animation played when timer countdown reaches 0. Drives both hardware and UI StateFlow in lockstep. */
    fun animateCompletion() {
        stopBlinking()
        stopAnimation()
        val gm = glyphManager
        val canDriveHardware = isEnabled && isSessionOpen && channelSegments.isNotEmpty() && gm != null
        val maxLight = (4000f * brightness).roundToInt().coerceIn(100, 4000)

        animationJob = controllerScope.launch {
            _isEndingBlink.value = true
            try {
                repeat(3) { _ ->
                    _isBlinkLightOn.value = true
                    if (canDriveHardware) {
                        try {
                            val builder = gm.glyphFrameBuilder
                            for (channel in channelSegments) {
                                builder.buildChannel(channel, maxLight)
                            }
                            gm.toggle(builder.build())
                        } catch (e: Exception) {
                            Log.e("GlyphController", "Completion blink ON error", e)
                        }
                    }
                    delay(250)

                    _isBlinkLightOn.value = false
                    if (canDriveHardware) {
                        try {
                            gm.turnOff()
                        } catch (e: Exception) {
                            Log.e("GlyphController", "Completion blink OFF error", e)
                        }
                    }
                    delay(250)
                }
            } finally {
                _isEndingBlink.value = false
                _isBlinkLightOn.value = true
                if (canDriveHardware) {
                    try {
                        gm.turnOff()
                    } catch (_: Exception) {}
                }
                lastWasOff = true
                lastFullCount = -1
                lastPartialLight = -1
            }
        }
    }

    fun stopBlinking() {
        blinkingJob?.cancel()
        blinkingJob = null
        _isBlinkLightOn.value = true
    }

    fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null
        _isEndingBlink.value = false
        _isBlinkLightOn.value = true
    }

    /**
     * Smooth animated sweep that starts from whichever point the timer was paused at
     * and sweeps back to the initial phase position. Fires onStep along the jump.
     */
    fun animateJumpToFullRing(
        fromProgress: Float = lastProgress,
        onStep: (() -> Unit)? = null
    ) {
        stopBlinking()
        stopAnimation()
        val startP = (if (isReversed) (1f - fromProgress) else fromProgress).coerceIn(0f, 1f)
        val targetP = if (isReversed) 0.0f else 1.0f

        if (!isEnabled || !isSessionOpen || channelSegments.isEmpty()) {
            updateProgress(1.0f)
            if (onStep != null) {
                animationJob = controllerScope.launch {
                    val ticks = 5
                    val tickDelay = 300L / ticks
                    for (i in 1..ticks) {
                        onStep.invoke()
                        delay(tickDelay)
                    }
                }
            }
            return
        }
        val gm = glyphManager ?: run {
            updateProgress(1.0f)
            return
        }

        val maxLight = (4000f * brightness).roundToInt().coerceIn(100, 4000)

        animationJob = controllerScope.launch {
            try {
                val total = channelSegments.size
                val distance = kotlin.math.abs(targetP - startP).coerceAtLeast(0.04f)
                val steps = ((distance * total * 2).roundToInt()).coerceIn(8, 28)
                val stepDelayMs = (320L / steps).coerceAtLeast(12L)

                var previousFullCount = (startP * total).toInt().coerceIn(0, total)
                lastWasOff = false
                lastFullCount = -1
                lastPartialLight = -1

                for (step in 1..steps) {
                    if (!isActive) break
                    val fraction = step.toFloat() / steps
                    val eased = 1f - (1f - fraction) * (1f - fraction)
                    val currentP = startP + (targetP - startP) * eased

                    val scaled = (currentP * total).coerceIn(0f, total.toFloat())
                    val fullCount = scaled.toInt().coerceIn(0, total)
                    val remainder = scaled - fullCount
                    val partialLight = if (fullCount < total && remainder > 0.01f) {
                        (remainder * maxLight.toFloat()).roundToInt().coerceIn(0, maxLight)
                    } else 0

                    if (fullCount != previousFullCount) {
                        previousFullCount = fullCount
                        onStep?.invoke()
                    }

                    if (fullCount == 0 && partialLight == 0) {
                        gm.turnOff()
                    } else {
                        val builder = gm.glyphFrameBuilder
                        for (i in 0 until fullCount) {
                            builder.buildChannel(channelSegments[i], maxLight)
                        }
                        if (fullCount < total && partialLight > 0) {
                            builder.buildChannel(channelSegments[fullCount], partialLight)
                        }
                        gm.toggle(builder.build())
                    }
                    delay(stepDelayMs)
                }

                if (targetP <= 0.001f) {
                    gm.turnOff()
                    lastWasOff = true
                    lastProgress = 1.0f
                    lastFullCount = 0
                    lastPartialLight = 0
                } else {
                    val finalBuilder = gm.glyphFrameBuilder
                    for (ch in channelSegments) {
                        finalBuilder.buildChannel(ch, maxLight)
                    }
                    gm.toggle(finalBuilder.build())
                    lastProgress = 1.0f
                    lastFullCount = total
                    lastPartialLight = 0
                    lastWasOff = false
                }
                onStep?.invoke()
            } catch (e: Exception) {
                Log.e("GlyphController", "Error animating jump", e)
                updateProgress(1.0f)
            }
        }
    }

    fun turnOff() {
        stopBlinking()
        stopAnimation()
        turnOffHardware()
    }

    /** Turn off hardware LEDs only, without touching blink/animation state */
    private fun turnOffHardware() {
        lastWasOff = true
        lastFullCount = -1
        lastPartialLight = -1
        if (!isSessionOpen) return
        try {
            glyphManager?.turnOff()
        } catch (e: Exception) {
            Log.e("GlyphController", "Failed to turn off glyphs", e)
        }
    }

    fun cleanup() {
        stopBlinking()
        stopAnimation()
        controllerScope.coroutineContext.cancelChildren()
        try {
            if (isSessionOpen) {
                glyphManager?.turnOff()
                glyphManager?.closeSession()
                isSessionOpen = false
            }
            glyphManager?.unInit()
            glyphManager = null
        } catch (e: Exception) {
            Log.e("GlyphController", "Failed cleanup", e)
        }
    }
}

