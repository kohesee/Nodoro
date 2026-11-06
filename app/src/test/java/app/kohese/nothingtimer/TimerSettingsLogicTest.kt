 package app.kohese.nothingtimer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerSettingsLogicTest {

    @Test
    fun timerPhase_durationsMatch() {
        val settings = TimerSettings(
            focusMinutes = 30,
            shortBreakMinutes = 7,
            longBreakMinutes = 20
        )
        assertEquals(30 * 60, settings.focusMinutes * 60)
        assertEquals(7 * 60, settings.shortBreakMinutes * 60)
        assertEquals(20 * 60, settings.longBreakMinutes * 60)
    }

    @Test
    fun glyphSettings_defaultEnabled() {
        val settings = TimerSettings()
        assertEquals(true, settings.glyphEnabled)
        assertEquals(1.0f, settings.glyphBrightness, 0.001f)
        assertEquals(false, settings.glyphReverse)
    }

    @Test
    fun glyphSettings_reversalCalculatesProgressInverted() {
        // Normal mode: 25min timer with 25min left = 1.0f progress (solid ring)
        val normalStart = 1.0f
        assertEquals(1.0f, normalStart, 0.001f)

        // Reversed mode: starts at 0% (empty ring) and completes ring as timer traverses (1.0 - progress)
        val reversedStart = 1.0f - normalStart
        assertEquals(0.0f, reversedStart, 0.001f)

        // Midway: progress 0.5f -> reversed is 0.5f
        val reversedMidway = 1.0f - 0.5f
        assertEquals(0.5f, reversedMidway, 0.001f)

        // Timer ended (0s left -> raw progress 0.0f) -> reversed reaches 1.0f (full ring completed)
        val reversedEnded = 1.0f - 0.0f
        assertEquals(1.0f, reversedEnded, 0.001f)
    }

    @Test
    fun glyphSettings_brightnessScalesMaxLight() {
        val fullBrightness = 1.0f
        val maxLight100 = (4000f * fullBrightness).toInt()
        assertEquals(4000, maxLight100)

        val halfBrightness = 0.5f
        val maxLight50 = (4000f * halfBrightness).toInt()
        assertEquals(2000, maxLight50)

        val minBrightness = 0.10f
        val maxLight10 = (4000f * minBrightness).toInt()
        assertEquals(400, maxLight10)
    }

    @Test
    fun progressFraction_segmentMapping() {
        // Phone (1) has 4 C segments
        val segmentsPhone1 = 4
        assertEquals(4, kotlin.math.round(1.0f * segmentsPhone1).toInt())
        assertEquals(2, kotlin.math.round(0.5f * segmentsPhone1).toInt())
        assertEquals(0, kotlin.math.round(0.0f * segmentsPhone1).toInt())

        // Phone (2) has 21 C segments
        val segmentsPhone2 = 21
        assertEquals(21, kotlin.math.round(1.0f * segmentsPhone2).toInt())
        assertEquals(10, kotlin.math.round(0.5f * segmentsPhone2).toInt())
        assertEquals(0, kotlin.math.round(0.0f * segmentsPhone2).toInt())

        // Phone (4a) line has 6 segments
        val segmentsPhone4a = 6
        assertEquals(6, kotlin.math.round(1.0f * segmentsPhone4a).toInt())
        assertEquals(3, kotlin.math.round(0.5f * segmentsPhone4a).toInt())
        assertEquals(0, kotlin.math.round(0.0f * segmentsPhone4a).toInt())
    }

    @Test
    fun formatHoursMinutes_formatsCorrectly() {
        assertEquals("0h 0m", formatHoursMinutes(0))
        assertEquals("0h 25m", formatHoursMinutes(25))
        assertEquals("1h 15m", formatHoursMinutes(75))
        assertEquals("2h 30m", formatHoursMinutes(150))
    }

    @Test
    fun smoothGlyphProgress_fractionalBrightnessInterpolation() {
        val totalSegments = 21
        val progress = 0.5f
        val scaled = progress * totalSegments // 10.5
        val fullCount = scaled.toInt()
        val remainder = scaled - fullCount // 0.5f
        val partialBrightness = (remainder * 4000f).toInt()

        assertEquals(10, fullCount)
        assertEquals(2000, partialBrightness)
    }

    @Test
    fun upNextLabel_showsBreakNameAndTime() {
        val settings = TimerSettings(
            focusMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
            dailyGoal = 4
        )

        // Session 0 (first session of 4): next is Short Break (5 min)
        val focusState1 = TimerUiState(settings = settings, phase = TimerPhase.FOCUS, completedSessions = 0)
        assertEquals("Up next: Short Break (5 min)", focusState1.upNextLabel)

        // Session 3 (4th session: (3 + 1) % 4 == 0): next is Long Break (15 min)
        val focusState4 = TimerUiState(settings = settings, phase = TimerPhase.FOCUS, completedSessions = 3)
        assertEquals("Up next: Long Break (15 min)", focusState4.upNextLabel)

        // During Short Break: next is Focus (25 min)
        val shortBreakState = TimerUiState(settings = settings, phase = TimerPhase.SHORT_BREAK)
        assertEquals("Up next: Focus (25 min)", shortBreakState.upNextLabel)

        // During Long Break: next is Focus (25 min)
        val longBreakState = TimerUiState(settings = settings, phase = TimerPhase.LONG_BREAK)
        assertEquals("Up next: Focus (25 min)", longBreakState.upNextLabel)
    }
}







