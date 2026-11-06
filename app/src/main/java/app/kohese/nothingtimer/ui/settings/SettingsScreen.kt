@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.kohese.nothingtimer.ui.settings

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import app.kohese.nothingtimer.R
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.horizontalScroll

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.rounded.AutoAwesome

import androidx.compose.material.icons.rounded.Check

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import app.kohese.nothingtimer.AccentColor

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kohese.nothingtimer.ThemeMode
import app.kohese.nothingtimer.TimerSettings
import app.kohese.nothingtimer.TimerUiState
import app.kohese.nothingtimer.ui.SpinningShape
import app.kohese.nothingtimer.ui.nav.LocalNavBarClearance
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class SettingsSheet(val title: String) {
    TIMER("Timer"),
    ALARM("Alarm & feedback"),
    GLYPH("Glyph"),
    APPEARANCE("Appearance"),
    LANGUAGE("Language"),
    ABOUT("About")
}

private val OuterCorner = 28.dp
private val InnerCorner = 6.dp

/** First item gets large top corners, last item large bottom corners, the rest are tightly rounded. */
private fun segmentShape(index: Int, count: Int): Shape {
    val top = if (index == 0) OuterCorner else InnerCorner
    val bottom = if (index == count - 1) OuterCorner else InnerCorner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
fun SettingsScreen(
    state: TimerUiState,
    onSettingsChanged: (TimerSettings) -> Unit,
    onResetStats: () -> Unit,
    onOpenPro: () -> Unit
) {
    val context = LocalContext.current
    val settings = state.settings
    var openSheet by rememberSaveable { mutableStateOf<SettingsSheet?>(null) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    val alarmLimitMs by rememberUpdatedState(settings.alarmSeconds * 1_000L)

    // ---- Alarm tone preview ----
    var previewRingtone by remember { mutableStateOf<Ringtone?>(null) }
    var isPlayingPreview by remember { mutableStateOf(false) }

    fun stopPreview() {
        try { previewRingtone?.stop() } catch (_: Exception) {}
        isPlayingPreview = false
    }

    fun playPreview(uriString: String?) {
        val uri = if (uriString.isNullOrEmpty()) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        else Uri.parse(uriString)
        stopPreview()
        if (uri != null) {
            try {
                val r = RingtoneManager.getRingtone(context, uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) r?.isLooping = true
                r?.play()
                previewRingtone = r
                isPlayingPreview = r != null
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose { try { previewRingtone?.stop() } catch (_: Exception) {} }
    }

    // Reset the play/stop button once the tone ends, and enforce the alarm length limit.
    LaunchedEffect(isPlayingPreview, previewRingtone) {
        var elapsed = 0L
        while (isPlayingPreview) {
            delay(300)
            elapsed += 300
            if (previewRingtone?.isPlaying != true) isPlayingPreview = false
            else if (elapsed >= alarmLimitMs) stopPreview()
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val uriString = uri?.toString() ?: ""
            onSettingsChanged(settings.copy(alarmSoundUri = uriString))
            playPreview(uriString)
        }
    }

    fun launchRingtonePicker() {
        val default = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val current = if (settings.alarmSoundUri.isNullOrEmpty()) default else Uri.parse(settings.alarmSoundUri)
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, default)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current)
        }
        ringtonePickerLauncher.launch(intent)
    }

    val isNothing = state.glyphDeviceName != "Standard Android" && state.glyphDeviceName != "Non-Nothing device"

    // ---- Main list ----
    val entries = listOf(
        SettingsEntry(
            SettingsSheet.TIMER, Icons.Rounded.Timer,
            "${settings.focusMinutes} / ${settings.shortBreakMinutes} / ${settings.longBreakMinutes} min · goal ${settings.dailyGoal}"
        ),
        SettingsEntry(
            SettingsSheet.ALARM, Icons.Rounded.Notifications,
            listOf(
                if (settings.alarmSound) "Alarm on" else "Alarm off",
                if (settings.vibration) "Vibration on" else "Vibration off"
            ).joinToString(" · ")
        ),
        SettingsEntry(

            SettingsSheet.GLYPH, Icons.Rounded.Lightbulb,

            if (!settings.glyphEnabled) "Off"

            else if (state.isPro) "On · ${(settings.glyphBrightness * 100).roundToInt()}% brightness"

            else "On"

        ),

        SettingsEntry(

            SettingsSheet.APPEARANCE, Icons.Rounded.Palette,

            themeLabel(settings.themeMode) + " · " +

                if (!state.isPro || settings.dynamicColor) "Dynamic color" else settings.accent.label

        ),

        SettingsEntry(SettingsSheet.LANGUAGE, Icons.Rounded.Language, "English"),
        SettingsEntry(SettingsSheet.ABOUT, Icons.Rounded.Info, "Version 1.0")
    )

    val groups = listOf(entries.take(4), entries.drop(4))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 20.dp,
                bottom = 24.dp + LocalNavBarClearance.current
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        )
        // The GitHub build has everything unlocked, so there is nothing to sell.
        if (!state.fullVersion) {
        SegmentedListItem(

            shape = segmentShape(0, 1),

            onClick = onOpenPro,

            containerOverride = MaterialTheme.colorScheme.primaryContainer

        ) {
            Box(contentAlignment = Alignment.Center) {
                SpinningShape(
                    polygon = MaterialShapes.Clover8Leaf,
                    size = 58.dp,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    durationMs = 36_000,
                )
                Image(
                    painter = painterResource(R.drawable.nodoro_pro_logo),
                    contentDescription = "Nodoro Pro",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {

                Text(

                    "Nodoro Pro",

                    style = MaterialTheme.typography.titleMedium,

                    fontWeight = FontWeight.Bold,

                    color = MaterialTheme.colorScheme.onPrimaryContainer

                )

                Text(

                    if (state.isPro) "Unlocked. Thank you!" else "No ads, Glyph control, accents, AMOLED",

                    style = MaterialTheme.typography.bodyMedium,

                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis

                )

            }

            if (state.isPro) {

                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)

            } else {

                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {

                    Text(

                        state.proPrice ?: "₹99",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)

                    )

                }

            }

        }

        Spacer(Modifier.height(14.dp))
        }


        groups.forEachIndexed { groupIndex, group ->

            if (groupIndex > 0) Spacer(Modifier.height(14.dp))
            group.forEachIndexed { index, entry ->
                SegmentedListItem(
                    shape = segmentShape(index, group.size),
                    onClick = { openSheet = entry.sheet }
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(entry.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            entry.sheet.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            entry.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                "Reset stats",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset stats?") },
            text = { Text("This permanently deletes all your daily, weekly, monthly, yearly and lifetime stats. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onResetStats()
                    showResetDialog = false
                }) { Text("Reset", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ---- Bottom sheet ----
    val sheet = openSheet
    if (sheet != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                if (sheet == SettingsSheet.ALARM) stopPreview()
                openSheet = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    sheet.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
                )
                when (sheet) {
                    SettingsSheet.TIMER -> TimerSheet(state, onSettingsChanged)
                    SettingsSheet.ALARM -> AlarmSheet(
                        context = context,
                        settings = settings,
                        isPlayingPreview = isPlayingPreview,
                        onSettingsChanged = onSettingsChanged,
                        onPickTone = ::launchRingtonePicker,
                        onTogglePreview = {
                            if (isPlayingPreview) stopPreview() else playPreview(settings.alarmSoundUri)
                        }
                    )
                    SettingsSheet.GLYPH -> GlyphSheet(state, isNothing, onSettingsChanged) { openSheet = null; onOpenPro() }
                    SettingsSheet.APPEARANCE -> AppearanceSheet(settings, state.isPro, onSettingsChanged) { openSheet = null; onOpenPro() }
                    SettingsSheet.LANGUAGE -> LanguageSheet(settings.language) { code ->
                        onSettingsChanged(settings.copy(language = code))
                        openSheet = null
                        applyAppLanguage(context, code)
                    }
                    SettingsSheet.ABOUT -> AboutSheet()
                }
            }
        }
    }
}

private data class SettingsEntry(
    val sheet: SettingsSheet,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val summary: String
)

// ---------------------------------------------------------------- sheets

@Composable
private fun TimerSheet(state: TimerUiState, onSettingsChanged: (TimerSettings) -> Unit) {
    val settings = state.settings
    // Mirrors TimerViewModel.updateSettings, which ignores duration edits while running/paused.
    val canEdit = state.isReset && !state.isRunning

    if (!canEdit) {
        Surface(
            shape = RoundedCornerShape(OuterCorner),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                "Timer is running or paused. Reset the timer to change durations.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    // Focus / short break / long break share one connected toggle-button group on a single line.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val fields = listOf(
            Triple("Focus", settings.focusMinutes, 1..180),
            Triple("Short break", settings.shortBreakMinutes, 1..60),
            Triple("Long break", settings.longBreakMinutes, 1..120)
        )
        fields.forEachIndexed { i, (label, value, range) ->
            val shape = when (i) {
                0 -> RoundedCornerShape(topStart = OuterCorner, bottomStart = OuterCorner, topEnd = InnerCorner, bottomEnd = InnerCorner)
                fields.lastIndex -> RoundedCornerShape(topStart = InnerCorner, bottomStart = InnerCorner, topEnd = OuterCorner, bottomEnd = OuterCorner)
                else -> RoundedCornerShape(InnerCorner)
            }
            MinutesField(
                label = label,
                value = value,
                range = range,
                enabled = canEdit,
                shape = shape,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) { v ->
                onSettingsChanged(
                    when (i) {
                        0 -> settings.copy(focusMinutes = v)
                        1 -> settings.copy(shortBreakMinutes = v)
                        else -> settings.copy(longBreakMinutes = v)
                    }
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    SegmentedListItem(shape = RoundedCornerShape(OuterCorner), onClick = null) {
        StepperRow("Focus sessions", "Long break after each set", settings.dailyGoal, 1, 12, canEdit) { onSettingsChanged(settings.copy(dailyGoal = it)) }
    }
}

/** One toggle-style segment: label on top, minutes typed directly into an inline numeric field. */
@Composable
private fun MinutesField(
    label: String,
    value: Int,
    range: IntRange,
    enabled: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    onValid: (Int) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    var focused by remember { mutableStateOf(false) }
    val container = if (focused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (focused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val focusManager = LocalFocusManager.current

    Surface(shape = shape, color = container, contentColor = content, modifier = modifier.alpha(if (enabled) 1f else 0.38f)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            BasicTextField(
                value = text,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(3)
                    text = digits
                    digits.toIntOrNull()?.takeIf { it in range }?.let(onValid)
                },
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = content,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(content),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        focused = state.isFocused
                        // Leaving the field with an empty/out-of-range entry restores the saved value.
                        if (!state.isFocused) text = value.toString()
                    }
            )
            Text("min", style = MaterialTheme.typography.labelMedium, color = content.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun AlarmSheet(
    context: Context,
    settings: TimerSettings,
    isPlayingPreview: Boolean,
    onSettingsChanged: (TimerSettings) -> Unit,
    onPickTone: () -> Unit,
    onTogglePreview: () -> Unit
) {
    SegmentedListItem(shape = segmentShape(0, 5), onClick = { onSettingsChanged(settings.copy(alarmSound = !settings.alarmSound)) }) {
        SwitchContent("Alarm sound", "Play sound when timer ends", settings.alarmSound)
    }
    SegmentedListItem(shape = segmentShape(1, 5), onClick = onPickTone) {
        Column(Modifier.weight(1f)) {
            Text("Alarm tone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(
                alarmSoundTitle(context, settings.alarmSoundUri),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onTogglePreview) {
            Icon(
                if (isPlayingPreview) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlayingPreview) "Stop preview" else "Play preview"
            )
        }
    }
    SegmentedListItem(shape = segmentShape(2, 5), onClick = null) {
        Column(Modifier
            .fillMaxWidth()
            .alpha(if (settings.alarmSound) 1f else 0.38f)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Alarm length limit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text("Alarm stops ringing after this long", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "${settings.alarmSeconds}s",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = settings.alarmSeconds.toFloat(),
                onValueChange = { onSettingsChanged(settings.copy(alarmSeconds = it.roundToInt().coerceIn(1, 30))) },
                valueRange = 1f..30f,
                steps = 28,
                enabled = settings.alarmSound,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    SegmentedListItem(shape = segmentShape(3, 5), onClick = { onSettingsChanged(settings.copy(sound = !settings.sound)) }) {
        SwitchContent("Sound", "Button and timer sounds", settings.sound)
    }
    SegmentedListItem(shape = segmentShape(4, 5), onClick = { onSettingsChanged(settings.copy(vibration = !settings.vibration)) }) {
        SwitchContent("Vibration", "Haptic feedback for alerts", settings.vibration)
    }
}

@Composable
private fun ProBadge() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Text(
            "PRO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun GlyphSheet(
    state: TimerUiState,
    isNothing: Boolean,
    onSettingsChanged: (TimerSettings) -> Unit,
    onOpenPro: () -> Unit
) {
    val settings = state.settings
    val isPro = state.isPro
    SegmentedListItem(shape = segmentShape(0, 4), onClick = null) {
        Column(Modifier.weight(1f)) {
            Text("Detected phone", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                state.glyphDeviceName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = if (isNothing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (isNothing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Text(
                if (isNothing) "Connected" else "Simulator",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
    // Glyph lighting is free.
    SegmentedListItem(shape = segmentShape(1, 4), onClick = { onSettingsChanged(settings.copy(glyphEnabled = !settings.glyphEnabled)) }) {
        SwitchContent("Glyph lighting", "Sync countdown dial with back LED lights", settings.glyphEnabled)
    }
    // Brightness is a Nodoro Pro feature.
    val canTune = isPro && settings.glyphEnabled
    SegmentedListItem(shape = segmentShape(2, 4), onClick = if (!isPro) onOpenPro else null) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .alpha(if (canTune || !isPro) 1f else 0.38f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(if (isPro) 1f else 0.6f)) {
                    Text(
                        "Brightness",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!isPro) {
                        Spacer(Modifier.width(8.dp))
                        ProBadge()
                    }
                }
                Text(
                    "${((if (isPro) settings.glyphBrightness else 1f) * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = if (isPro) settings.glyphBrightness else 1f,
                onValueChange = { onSettingsChanged(settings.copy(glyphBrightness = it)) },
                valueRange = 0.10f..1.0f,
                enabled = canTune,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (canTune) 1f else 0.5f)
            )
        }
    }
    // Reverse direction is a Nodoro Pro feature.
    SegmentedListItem(
        shape = segmentShape(3, 4),
        onClick = if (!isPro) onOpenPro else if (settings.glyphEnabled) ({ onSettingsChanged(settings.copy(glyphReverse = !settings.glyphReverse)) }) else null
    ) {
        SwitchContent(
            "Reverse progress direction",
            "Glyph starts at 0% and completes ring as timer traverses",
            isPro && settings.glyphReverse,
            enabled = isPro && settings.glyphEnabled,
            proLocked = !isPro
        )
    }
}

@Composable
private fun AppearanceSheet(
    settings: TimerSettings,
    isPro: Boolean,
    onSettingsChanged: (TimerSettings) -> Unit,
    onOpenPro: () -> Unit
) {
    // ---- Theme (free) ----
    SegmentedListItem(shape = segmentShape(0, 1), onClick = null) {
        Column(Modifier.fillMaxWidth()) {
            Text("Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            val modes = ThemeMode.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { onSettingsChanged(settings.copy(themeMode = mode)) },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size)
                    ) { Text(themeLabel(mode)) }
                }
            }
        }
    }
    Spacer(Modifier.height(14.dp))

    // ---- Color (dynamic always on for free, accents + AMOLED are Pro) ----
    val dynamicOn = !isPro || settings.dynamicColor
    val showAmoled = settings.themeMode != ThemeMode.LIGHT
    val count = if (showAmoled) 3 else 2

    SegmentedListItem(
        shape = segmentShape(0, count),
        onClick = if (!isPro) onOpenPro else ({ onSettingsChanged(settings.copy(dynamicColor = !settings.dynamicColor)) })
    ) {
        SwitchContent(
            "Dynamic color",
            if (isPro) "Use your wallpaper's palette" else "Always on. Turn it off with Nodoro Pro",
            dynamicOn,
            proLocked = !isPro
        )
    }

    val canPickAccent = isPro && !settings.dynamicColor
    SegmentedListItem(
        shape = segmentShape(1, count),
        onClick = if (!isPro) onOpenPro else null
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Accent color",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!isPro) {
                    Spacer(Modifier.width(8.dp))
                    ProBadge()
                }
            }
            Text(
                when {
                    !isPro -> "Pick a major accent color"
                    settings.dynamicColor -> "Turn off dynamic color to choose an accent"
                    else -> settings.accent.label
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .alpha(if (canPickAccent) 1f else 0.38f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccentColor.entries.forEach { accent ->
                    val selected = canPickAccent && settings.accent == accent
                    val swatch = if (accent == AccentColor.MONO) Color(0xFF111111) else Color.hsl(accent.hue, 0.7f, 0.5f)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                            .clickable(enabled = canPickAccent) {
                                onSettingsChanged(
                                    settings.copy(
                                        accent = accent
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (accent == AccentColor.MONO) {
                            // Half white to show it's black and white.
                            Box(Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.White))
                        }
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = accent.label, tint = if (accent == AccentColor.MONO) Color.Black else Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showAmoled) {
        SegmentedListItem(
            shape = segmentShape(2, count),
            onClick = if (!isPro) onOpenPro else ({ onSettingsChanged(settings.copy(amoled = !settings.amoled)) })
        ) {
            SwitchContent(
                "AMOLED black",
                "Pure black background in dark mode",
                isPro && settings.amoled,
                proLocked = !isPro
            )
        }
    }
}



@Composable
private fun LanguageSheet(selected: String, onSelect: (String) -> Unit) {
    // English is the only language for now; more are on the way.
    SegmentedListItem(shape = segmentShape(0, 2), onClick = { onSelect("en") }) {
        RadioButton(selected = true, onClick = null)
        Spacer(Modifier.width(16.dp))
        Text("English", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
    }
    SegmentedListItem(shape = segmentShape(1, 2), onClick = null) {
        Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(
            "More languages",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
            Text(
                "Coming soon",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun AboutSheet() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0" } catch (_: Exception) { "1.0" }
    }
    val socials = listOf(
        Triple(painterResource(R.drawable.ic_brand_github), "GitHub", "https://github.com/kohesee"),
        Triple(rememberVectorPainter(Icons.Rounded.Email), "Email", "mailto:kohese.dev@proton.me"),
        Triple(rememberVectorPainter(Icons.Rounded.Public), "Website", "https://kohese.tech"),
        Triple(painterResource(R.drawable.ic_brand_x), "X", "https://x.com/kalamitifier")
    )

    SegmentedListItem(shape = segmentShape(0, 3), onClick = null) {
        Image(
            painter = painterResource(R.drawable.nodoro_logo),
            contentDescription = "Nodoro",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialShapes.Cookie12Sided.toShape())
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Nodoro", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Version $version", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        FilledTonalIconButton(onClick = { uriHandler.openUri("https://github.com/kohesee") }) {
            Icon(painterResource(R.drawable.ic_brand_github), contentDescription = "GitHub", modifier = Modifier.size(22.dp))
        }
    }
    SegmentedListItem(shape = segmentShape(1, 3), onClick = null) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.kohese_pfp),
                    contentDescription = "Kaivalya Beohar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialShapes.Square.toShape())
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Kaivalya Beohar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Developer", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.padding(start = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                socials.forEach { (icon, label, url) ->
                    FilledTonalIconButton(
                        onClick = { uriHandler.openUri(url) },
                        modifier = Modifier.width(48.dp)
                    ) {
                        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    SegmentedListItem(shape = segmentShape(2, 3), onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) }) {
        Icon(Icons.Rounded.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text("Privacy policy", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- building blocks

/** A list row that is part of a segmented group; the shape decides which corners are rounded. */
@Composable
private fun SegmentedListItem(
    shape: Shape,
    onClick: (() -> Unit)?,
    containerOverride: Color? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val container = containerOverride ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = container, modifier = Modifier.fillMaxWidth()) { rowContent() }
    } else {
        Surface(shape = shape, color = container, modifier = Modifier.fillMaxWidth()) { rowContent() }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SwitchContent(
    label: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean = true,
    proLocked: Boolean = false
) {
    Column(Modifier
        .weight(1f)
        .alpha(if (enabled || proLocked) 1f else 0.38f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            if (proLocked) {
                Spacer(Modifier.width(8.dp))
                ProBadge()
            }
        }
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.width(12.dp))
    Switch(checked = checked, onCheckedChange = null, enabled = enabled && !proLocked)
}
@Composable
private fun androidx.compose.foundation.layout.RowScope.StepperRow(
    label: String,
    detail: String,
    value: Int,
    min: Int,
    max: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit
) {
    Column(Modifier
        .weight(1f)
        .alpha(if (enabled) 1f else 0.38f)) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    FilledTonalIconButton(
        onClick = { if (value > min) onChange(value - 1) },
        enabled = enabled && value > min
    ) { Icon(Icons.Rounded.Remove, contentDescription = "Decrease $label") }
    Text(
        "$value",
        modifier = Modifier
            .width(44.dp)
            .alpha(if (enabled) 1f else 0.38f),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    FilledTonalIconButton(
        onClick = { if (value < max) onChange(value + 1) },
        enabled = enabled && value < max
    ) { Icon(Icons.Rounded.Add, contentDescription = "Increase $label") }
}

// ---------------------------------------------------------------- helpers

private val LANGUAGES = listOf(
    "system" to "System default",
    "en" to "English",
    "es" to "Español",
    "fr" to "Français",
    "de" to "Deutsch",
    "ja" to "日本語",
    "zh" to "中文"
)

private fun languageName(code: String): String = LANGUAGES.firstOrNull { it.first == code }?.second ?: "System default"

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun alarmSoundTitle(context: Context, uriString: String?): String {
    val uri = if (uriString.isNullOrEmpty()) {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    } else {
        Uri.parse(uriString)
    } ?: return "Default alarm"
    return try {
        RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Default alarm"
    } catch (_: Exception) {
        "Default alarm"
    }
}

private fun applyAppLanguage(context: Context, langCode: String) {
    try {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        localeManager?.applicationLocales =
            if (langCode == "system") LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(langCode)
    } catch (_: Exception) {}
}

/** Hosted on GitHub Pages from the docs/ folder of the repository. */
private const val PRIVACY_POLICY_URL = "https://kohesee.github.io/Nodoro/privacy-policy.html"
