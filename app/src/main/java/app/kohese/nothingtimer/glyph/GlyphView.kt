package app.kohese.nothingtimer.glyph

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

enum class GlyphPhoneModel(val displayName: String, val shortName: String, val assetFile: String) {
    PHONE_1("Nothing Phone (1)", "Phone (1)", "phone1.svg"),
    PHONE_2("Nothing Phone (2)", "Phone (2)", "phone2.svg"),
    PHONE_2A("Nothing Phone (2a)", "Phone (2a)", "phone2a.svg"),
    PHONE_3A("Nothing Phone (3a)", "Phone (3a)", "phone3a.svg"),
    PHONE_4A("Nothing Phone (4a)", "Phone (4a)", "phone4a.svg");

    companion object {
        fun fromDeviceName(name: String): GlyphPhoneModel {
            return when {
                name.contains("(1)") -> PHONE_1
                name.contains("(2a)") || name.contains("(2a) Plus") -> PHONE_2A
                name.contains("(2)") -> PHONE_2
                name.contains("(3a)") -> PHONE_3A
                name.contains("(4a)") -> PHONE_4A
                else -> PHONE_2
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InteractiveTimerDial(
    secondsLeft: Int,
    progress: Float,
    isPaused: Boolean,
    isRunning: Boolean,
    isBlinkLightOn: Boolean,
    glyphBrightness: Float,
    glyphReverse: Boolean,
    currentModel: GlyphPhoneModel,
    showGlyphShape: Boolean,
    onToggleDialShape: () -> Unit,
    modifier: Modifier = Modifier,
    isEndingBlink: Boolean = false,
    /** Fraction of the ring one second represents; lets the wavy ring glide instead of stepping. */
    progressStep: Float = 0f
) {
    val timeText = "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60)
    val density = LocalDensity.current

    // The dial is sized from the space it is given (capped at 300dp) so it can never overflow short
    // screens, and the digits are sized from the dial in physical pixels (Dp.toSp cancels the user's
    // font scale), so the layout looks identical across screen densities and font-size settings.
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
    val dialSize = minOf(maxWidth, maxHeight, 300.dp)
    val bigTimeSp = with(density) { (dialSize * 0.27f).toSp() }

    AnimatedContent(
        targetState = showGlyphShape,
        transitionSpec = {
            (fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.94f, animationSpec = tween(280)))
                .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.94f, animationSpec = tween(200)))
        },
        label = "DialShapeTransition"
    ) { isGlyph ->
        if (isGlyph) {
            when (currentModel) {
                GlyphPhoneModel.PHONE_4A -> {
                    // For Phone (4a): vertical SVG strip placed on the right side of the timer text
                    val rowWidth = minOf(maxWidth, 340.dp)
                    val rowHeight = minOf(maxHeight, 280.dp)
                    val stripHeight = rowHeight - 40.dp
                    // Same digit size as Phone (2a), shrunk only if the text slot is too narrow.
                    val slotWidth = rowWidth - 40.dp - 42.dp - 16.dp
                    val timeSp = minOf(bigTimeSp.value, with(density) { (slotWidth / 2.6f).toSp().value }).sp
                    Row(
                        modifier = Modifier
                            .size(width = rowWidth, height = rowHeight)
                            .clip(RoundedCornerShape(28.dp))
                            .clickable(onClick = onToggleDialShape)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .semantics {
                                contentDescription = "Glyph Phone 4a timer dial, remaining $timeText, tap to switch to circular dial"
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = timeSp, lineHeight = timeSp),
                                fontWeight = FontWeight.Light,
                                letterSpacing = (-2).sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        GlyphDisplay(
                            model = GlyphPhoneModel.PHONE_4A,
                            progress = progress,
                            isPaused = isPaused,
                            isBlinkLightOn = isBlinkLightOn,
                            isRunning = isRunning,
                            glyphBrightness = glyphBrightness,
                            glyphReverse = glyphReverse,
                            isEndingBlink = isEndingBlink,
                            modifier = Modifier
                                .width(42.dp)
                                .height(stripHeight.coerceAtLeast(120.dp))
                        )
                    }
                }
                GlyphPhoneModel.PHONE_1, GlyphPhoneModel.PHONE_2 -> {
                    // For Phone 1 & 2: SVG size made noticeably bigger and taller without clipping
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(minOf(if (currentModel == GlyphPhoneModel.PHONE_2) 220.dp else 410.dp, maxHeight))
                            .clickable(onClick = onToggleDialShape)
                            .semantics {
                                contentDescription = "Glyph timer dial, remaining $timeText, tap to switch to circular dial"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        GlyphDisplay(
                            model = currentModel,
                            progress = progress,
                            isPaused = isPaused,
                            isBlinkLightOn = isBlinkLightOn,
                            isRunning = isRunning,
                            glyphBrightness = glyphBrightness,
                            glyphReverse = glyphReverse,
                            isEndingBlink = isEndingBlink,
                            modifier = Modifier.fillMaxSize()
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            val smallSp = with(density) { (dialSize * 0.17f).toSp() }
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = smallSp, lineHeight = smallSp),
                                fontWeight = FontWeight.Light,
                                letterSpacing = (-2).sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
                else -> {
                    // For Phone 2a & 3a: same square footprint as the circular timer, with big digits.
                    Box(
                        modifier = Modifier
                            .size(dialSize)
                            .clickable(onClick = onToggleDialShape)
                            .semantics {
                                contentDescription = "Glyph timer dial, remaining $timeText, tap to switch to circular dial"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        GlyphDisplay(
                            model = currentModel,
                            progress = progress,
                            isPaused = isPaused,
                            isBlinkLightOn = isBlinkLightOn,
                            isRunning = isRunning,
                            glyphBrightness = glyphBrightness,
                            glyphReverse = glyphReverse,
                            isEndingBlink = isEndingBlink,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Centred on the dial with trimmed line metrics (no extra top/bottom padding inside the
                        // text box), then nudged down by a fraction of the dial so it scales on every screen:
                        // the artwork's optical centre sits lower than its bounding-box centre.
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = bigTimeSp,
                                lineHeight = bigTimeSp,
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both
                                )
                            ),
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-2).sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .offset(y = dialSize * 0.035f)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(dialSize)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleDialShape)
                    .semantics {
                        contentDescription = "Circular timer dial, remaining $timeText, tap to switch to glyph dial"
                    },
                contentAlignment = Alignment.Center
            ) {
                val waveAmplitude by animateFloatAsState(
                    targetValue = if (isRunning) 1f else 0f,
                    animationSpec = tween(400),
                    label = "WaveAmplitude"
                )
                val strokePx = with(LocalDensity.current) { 8.dp.toPx() }
                // Progress only changes once a second, so while running we glide towards the next
                // second's value over exactly one second with a linear ease. Otherwise snap-ish.
                val smoothProgress by animateFloatAsState(
                    targetValue = if (isRunning) (progress - progressStep).coerceAtLeast(0f) else progress,
                    animationSpec = if (isRunning) tween(1000, easing = LinearEasing) else tween(400),
                    label = "SmoothProgress"
                )
                val waveStroke = Stroke(width = strokePx, cap = StrokeCap.Round)
                CircularWavyProgressIndicator(
                    progress = { smoothProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    stroke = waveStroke,
                    trackStroke = waveStroke,
                    amplitude = { waveAmplitude },
                    wavelength = 40.dp
                )
                // Same digit size as the Phone (2a)/(3a) dial.
                val circleSp = bigTimeSp
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = circleSp,
                        lineHeight = circleSp,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    ),
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-2).sp,
                    maxLines = 1,
                    softWrap = false,
                    // Same vertical position as the phone dials' digits.
                    modifier = Modifier.offset(y = dialSize * 0.035f)
                )
            }
        }
    }
    }
}

@Composable
fun GlyphDisplay(
    model: GlyphPhoneModel,
    progress: Float,
    isPaused: Boolean,
    isBlinkLightOn: Boolean,
    isRunning: Boolean,
    glyphBrightness: Float,
    glyphReverse: Boolean,
    modifier: Modifier = Modifier,
    isEndingBlink: Boolean = false
) {
    val context = LocalContext.current
    val parsedSvg = remember(model) {
        context.assets.open("glyphs/${model.assetFile}").use { stream ->
            GlyphSvgParser.parse(stream)
        }
    }

    val cachedPaths = remember(parsedSvg) {
        parsedSvg.paths.associate { pathData ->
            pathData.id to try {
                PathParser().parsePathString(pathData.d).toPath()
            } catch (_: Exception) {
                Path()
            }
        }
    }

    val effectiveProgress = if (isEndingBlink) {
        1.0f
    } else if (glyphReverse) {
        (1.0f - progress).coerceIn(0f, 1f)
    } else {
        progress.coerceIn(0f, 1f)
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // Negative appearance on light theme: on dark theme LEDs are white on black,
    // on light theme LEDs are inverted to dark ink on white background so they look crisp, natural, and never disappear.
    // Follows the theme's onBackground: white on the plain dark theme, ink on plain light, and the
    // wallpaper-tinted tone when dynamic color is on (in both light and dark mode).
    val glyphColor = MaterialTheme.colorScheme.onBackground
    val clampedBrightness = glyphBrightness.coerceIn(0.10f, 1.0f)
    // Synchronized hardware blink: when paused or ending completion blink and blink light is OFF, light drops to ambient dimAlpha (never invisible)
    val isLightIlluminated = if (isEndingBlink) isBlinkLightOn else if (isPaused) isBlinkLightOn else true
    val dimAlpha = if (isDark) 0.10f else 0.14f

    // For Phone 2a / 3a the artwork only fills part of the SVG viewBox, so fit the actual
    // artwork bounds (in viewBox space, after the group transform) to the canvas instead.
    val artBounds = remember(cachedPaths, parsedSvg) {
        if (model != GlyphPhoneModel.PHONE_2A && model != GlyphPhoneModel.PHONE_3A) null
        else {
            val t = parsedSvg.transform
            var l = Float.MAX_VALUE
            var tp = Float.MAX_VALUE
            var r = -Float.MAX_VALUE
            var b = -Float.MAX_VALUE
            cachedPaths.values.forEach { p ->
                val bb = p.getBounds()
                if (bb.width > 0f || bb.height > 0f) {
                    l = min(l, bb.left * t.scaleX + t.translateX)
                    r = maxOf(r, bb.right * t.scaleX + t.translateX)
                    tp = min(tp, bb.top * t.scaleY + t.translateY)
                    b = maxOf(b, bb.bottom * t.scaleY + t.translateY)
                }
            }
            if (r > l && b > tp) listOf(l, tp, r - l, b - tp) else null
        }
    }

    Canvas(modifier = modifier) {
        val (vx, vy, vw, vh) = parsedSvg.viewBox
        val isRotated = model == GlyphPhoneModel.PHONE_2
        // Make Phone 1 and 2 noticeably bigger with safe margin so no SVG ever clips out
        val scaleMultiplier = when (model) {
            GlyphPhoneModel.PHONE_1, GlyphPhoneModel.PHONE_2 -> 0.98f
            GlyphPhoneModel.PHONE_4A -> 0.94f
            else -> 1.0f
        }
        val (fitX, fitY, fitW, fitH) = artBounds
            ?: listOf(vx, vy, vw, vh)
        val scale = if (isRotated) {
            // Phone 2 is rotated 90° left, so its width/height swap.
            min(size.width / vh, size.height / vw) * scaleMultiplier
        } else {
            min(size.width / fitW, size.height / fitH) * scaleMultiplier
        }
        val offsetX = (size.width - fitW * scale) / 2f - fitX * scale
        val offsetY = (size.height - fitH * scale) / 2f - fitY * scale

        withTransform({
            if (isRotated) {
                // Rotate the whole drawing (shapes, arcs, gradients) about the SVG centre,
                // so all animation logic keeps working in the original SVG coordinate space.
                translate(size.width / 2f, size.height / 2f)
                rotate(-90f, Offset.Zero)
                scale(scale, scale, Offset.Zero)
                translate(-(vx + vw / 2f), -(vy + vh / 2f))
            } else {
                translate(offsetX, offsetY)
                scale(scale, scale, Offset.Zero)
            }
        }) {
            val transform = parsedSvg.transform
            withTransform({
                translate(transform.translateX, transform.translateY)
                scale(transform.scaleX, transform.scaleY, Offset.Zero)
            }) {
                when (model) {
                    GlyphPhoneModel.PHONE_1 -> drawPhone1(cachedPaths, effectiveProgress, isLightIlluminated, clampedBrightness, dimAlpha, glyphColor)
                    GlyphPhoneModel.PHONE_2 -> drawPhone2(cachedPaths, effectiveProgress, isLightIlluminated, clampedBrightness, dimAlpha, glyphColor)
                    GlyphPhoneModel.PHONE_2A -> drawPhone2a(cachedPaths, effectiveProgress, isLightIlluminated, clampedBrightness, dimAlpha, glyphColor)
                    GlyphPhoneModel.PHONE_3A -> drawPhone3a(cachedPaths, effectiveProgress, isLightIlluminated, clampedBrightness, dimAlpha, glyphColor)
                    GlyphPhoneModel.PHONE_4A -> drawPhone4a(parsedSvg.rects, effectiveProgress, isLightIlluminated, clampedBrightness, dimAlpha, glyphColor, isRunning || isPaused, isPaused, isDark)
                }
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

/**
 * Phone 1: uses D channel (Path 3, the vertical timer bar at bottom).
 * Path 2 (C coil) is intentionally NOT lit up and kept at ambient dim (0.10 opacity).
 * Paths 0, 1, 4 also remain ambient dim.
 * Path 3 unfills from top downwards towards the bottom as progress decreases.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPhone1(
    paths: Map<String, Path>,
    progress: Float,
    isLightIlluminated: Boolean,
    clampedBrightness: Float,
    dimAlpha: Float,
    glyphColor: Color
) {
    // Ambient paths: 0 (camera), 1 (slash), 2 (C coil ring), 4 (dot) always drawn at dimAlpha (0.10 opacity)
    listOf("0", "1", "2", "4").forEach { id ->
        paths[id]?.let { path ->
            drawPath(path, glyphColor.copy(alpha = dimAlpha))
        }
    }

    // Path 3 is the D channel vertical exclamation bar (8 LED segments on Phone 1)
    paths["3"]?.let { dBarPath ->
        // Always draw the base ambient outline at dimAlpha (0.10 opacity) so it is never black
        drawPath(dBarPath, glyphColor.copy(alpha = dimAlpha))

        val active = progress * 8f
        val stops = Array(8) { i ->
            val segP = (active - i).coerceIn(0f, 1f)
            val alpha = if (isLightIlluminated) lerp(dimAlpha, clampedBrightness, segP) else dimAlpha
            (i / 7f) to glyphColor.copy(alpha = alpha)
        }
        // Stop 0 at bottom (90.991f, 356.73f), Stop 7 at top (90.991f, 308.75f)
        // As progress drops from 1.0 down to 0.0, Stop 7 (top) dims first, unfilling downwards
        val brush = Brush.linearGradient(
            colorStops = stops,
            start = Offset(90.991f, 356.73f),
            end = Offset(90.991f, 308.75f)
        )
        drawPath(dBarPath, brush)
    }
}

/**
 * Phone 2: uses ONLY path 3-18 (the 16-LED timer arc, C1_1 to C1_16).
 * Dimming starts at C1_1 (bottom-right) up to C1_16 (top-left).
 * All other paths remain at ambient dim (0.10 opacity).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPhone2(
    paths: Map<String, Path>,
    progress: Float,
    isLightIlluminated: Boolean,
    clampedBrightness: Float,
    dimAlpha: Float,
    glyphColor: Color
) {
    // All other paths remain ambient dim (0.10 opacity)
    listOf("0", "1", "2", "19", "20", "21", "22", "23", "24", "25-32").forEach { id ->
        paths[id]?.let { path ->
            drawPath(path, glyphColor.copy(alpha = dimAlpha))
        }
    }

    // Path 3-18: ONLY timer arc that lights up.
    // Base is always at dimAlpha (never black).
    paths["3-18"]?.let { path ->
        drawPath(path, glyphColor.copy(alpha = dimAlpha))

        // Uniform circular arc along the Phone 2 coil center (91f, 191f).
        // Starts at -100° (top-left / C1_16) and sweeps clockwise to -38° (bottom-right / C1_1).
        // As progress drops, sweepAngle decreases so C1_1 (-38°) dims first, up to C1_16 (-100°).
        if (isLightIlluminated && progress > 0f) {
            clipPath(path) {
                drawArc(
                    color = glyphColor.copy(alpha = clampedBrightness),
                    startAngle = -101f,
                    sweepAngle = progress * 64f,
                    useCenter = true,
                    topLeft = Offset(-14f, 86f),
                    size = Size(210f, 210f)
                )
            }
        }
    }
}

/**
 * Phone 2a: uses ONLY path 0-23 (the 24-LED camera arc).
 * Top unfill start: top dims first as progress drops, bottom stays filled.
 * Paths 24, 25 remain at ambient dim (0.10 opacity).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPhone2a(
    paths: Map<String, Path>,
    progress: Float,
    isLightIlluminated: Boolean,
    clampedBrightness: Float,
    dimAlpha: Float,
    glyphColor: Color
) {
    // Ambient paths: 24, 25 always at dimAlpha (0.10 opacity)
    listOf("24", "25").forEach { id ->
        paths[id]?.let { path ->
            drawPath(path, glyphColor.copy(alpha = dimAlpha))
        }
    }

    // Path 0-23: ONLY timer arc that lights up.
    // Base is always at dimAlpha (never black).
    paths["0-23"]?.let { path ->
        drawPath(path, glyphColor.copy(alpha = dimAlpha))

        val active = progress * 24f
        val stops = Array(24) { i ->
            val segP = (active - i).coerceIn(0f, 1f)
            val alpha = if (isLightIlluminated) lerp(dimAlpha, clampedBrightness, segP) else dimAlpha
            (i / 23f) to glyphColor.copy(alpha = alpha)
        }
        // Top unfill start: Stop 0 at bottom-left (27.84f, 95.8f), Stop 23 at top-right (63.5f, 54.78f).
        // As progress drops from 1.0 to 0.0, Stop 23 (top) dims first, unfilling towards Stop 0 (bottom).
        val brush = Brush.linearGradient(
            colorStops = stops,
            start = Offset(27.84f, 95.8f),
            end = Offset(63.5f, 54.78f)
        )
        drawPath(path, brush)
    }
}

/**
 * Phone 3a: starts top-left curve unfill (31-35), then bottom-left (0-19), then right channel (20-30).
 * All paths have ambient base at 0.10 opacity (never black).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPhone3a(
    paths: Map<String, Path>,
    progress: Float,
    isLightIlluminated: Boolean,
    clampedBrightness: Float,
    dimAlpha: Float,
    glyphColor: Color
) {
    val totalActiveStops = progress * 36f

    // Unfill order:
    // 1. Top-left curve (31-35, 20 stops): un-fills FIRST from 36 down to 16
    // 2. Bottom-left slash (0-19, 5 stops): un-fills SECOND from 16 down to 11
    // 3. Right vertical channel (20-30, 11 stops): un-fills LAST from 11 down to 0
    val topLeftActive = (totalActiveStops - 16f).coerceIn(0f, 20f)
    val bottomLeftActive = (totalActiveStops - 11f).coerceIn(0f, 5f)
    val rightActive = totalActiveStops.coerceIn(0f, 11f)

    // 1. Path 31-35: 20 stops (Top-left curve)
    paths["31-35"]?.let { path ->
        drawPath(path, glyphColor.copy(alpha = dimAlpha))

        val stops = Array(20) { i ->
            val segP = (topLeftActive - i).coerceIn(0f, 1f)
            val alpha = if (isLightIlluminated) lerp(dimAlpha, clampedBrightness, segP) else dimAlpha
            (i / 19f) to glyphColor.copy(alpha = alpha)
        }
        val brush = Brush.linearGradient(
            colorStops = stops,
            start = Offset(23.35f, 61.5f),
            end = Offset(50.65f, 14.3f)
        )
        drawPath(path, brush)
    }

    // 2. Path 0-19: 5 stops (Bottom-left slash)
    paths["0-19"]?.let { path ->
        drawPath(path, glyphColor.copy(alpha = dimAlpha))

        val stops = Array(5) { i ->
            val segP = (bottomLeftActive - i).coerceIn(0f, 1f)
            val alpha = if (isLightIlluminated) lerp(dimAlpha, clampedBrightness, segP) else dimAlpha
            (i / 4f) to glyphColor.copy(alpha = alpha)
        }
        val brush = Brush.linearGradient(
            colorStops = stops,
            start = Offset(38.22f, 139.3f),
            end = Offset(24.31f, 113.5f)
        )
        drawPath(path, brush)
    }

    // 3. Path 20-30: 11 stops (Right vertical channel)
    paths["20-30"]?.let { path ->
        drawPath(path, glyphColor.copy(alpha = dimAlpha))

        val stops = Array(11) { i ->
            val segP = (rightActive - i).coerceIn(0f, 1f)
            val alpha = if (isLightIlluminated) lerp(dimAlpha, clampedBrightness, segP) else dimAlpha
            (i / 10f) to glyphColor.copy(alpha = alpha)
        }
        val brush = Brush.linearGradient(
            colorStops = stops,
            start = Offset(160.46f, 60.4f),
            end = Offset(159.6f, 123.6f)
        )
        drawPath(path, brush)
    }
}

/**
 * Phone 4a: follows sequence 5, 4, 3, 2, 1, 0 (6 blocks unfilling from top down to bottom).
 * Rect 6 is red status dot: fills up red at bottom when paused!
 * All rects have base at 0.10 opacity (never black).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPhone4a(
    rects: List<SvgRect>,
    progress: Float,
    isLightIlluminated: Boolean,
    clampedBrightness: Float,
    dimAlpha: Float,
    glyphColor: Color,
    isActive: Boolean,
    isPaused: Boolean,
    isDark: Boolean
) {
    val rectMap = rects.associateBy { it.id }
    val order = listOf("5", "4", "3", "2", "1", "0")
    val totalActive = progress * 6f
    val redDimAlpha = if (isDark) 0.10f else 0.18f

    // Always draw all rects at dimAlpha (ambient opacity) so unlit is never invisible
    rects.forEach { rect ->
        drawRect(
            color = if (rect.id == "6") Color(0xFFE50914).copy(alpha = redDimAlpha) else glyphColor.copy(alpha = dimAlpha),
            topLeft = Offset(rect.x, rect.y),
            size = Size(rect.width, rect.height)
        )
    }

    // Draw active illumination on blocks (5 down to 0)
    order.forEachIndexed { idx, id ->
        val rect = rectMap[id] ?: return@forEachIndexed
        val segP = (totalActive - idx).coerceIn(0f, 1f)
        val alpha = if (isLightIlluminated) lerp(dimAlpha, clampedBrightness, segP) else dimAlpha
        drawRect(
            color = glyphColor.copy(alpha = alpha),
            topLeft = Offset(rect.x, rect.y),
            size = Size(rect.width, rect.height)
        )
    }

    // Rect 6 is red status dot: blinks on pause, stays lit when active (non-reversed)
    rectMap["6"]?.let { rect ->
        val redAlpha = if (isPaused) {
            // Blink in lockstep with white blocks during pause
            if (isLightIlluminated) clampedBrightness else redDimAlpha
        } else if (isActive) {
            // Running or idle but active: red dot stays solidly lit
            clampedBrightness
        } else {
            redDimAlpha
        }
        drawRect(
            color = Color(0xFFE50914).copy(alpha = redAlpha),
            topLeft = Offset(rect.x, rect.y),
            size = Size(rect.width, rect.height)
        )
    }
}
