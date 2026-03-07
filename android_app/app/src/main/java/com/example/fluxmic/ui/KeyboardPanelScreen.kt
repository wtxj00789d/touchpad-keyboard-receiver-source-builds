package com.example.fluxmic.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.fluxmic.model.KeyConfig
import com.example.fluxmic.model.PageConfig
import com.example.fluxmic.model.UiState
import com.example.fluxmic.ui.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private const val PREFS_NAME = "fluxmic_prefs"
private const val PREF_BG_MEDIA_URI = "background_media_uri"
private const val PREF_BG_VIDEO_URI = "background_video_uri" // legacy key for migration
private const val PREF_TEXT_MODE = "key_text_mode"
private const val PREF_RIPPLE_MODE = "ripple_quality_mode"
private const val GLASS_BUTTON_ALPHA = 0.10f

private enum class RipplePhase {
    Press,
    Hold,
    Release
}

private data class GlobalRipplePulse(
    val id: Int,
    val origin: Offset,
    val startRadius: Float,
    val maxRadius: Float,
    val startedAtMs: Long,
    val phase: MutableState<RipplePhase>,
    val radiusAnim: Animatable<Float, AnimationVector1D>,
    val alphaAnim: Animatable<Float, AnimationVector1D>,
    val coreAlphaAnim: Animatable<Float, AnimationVector1D>
)

private enum class KeyTextMode {
    NORMAL,
    LIGHT,
    DARK;

    fun next(): KeyTextMode {
        return when (this) {
            NORMAL -> LIGHT
            LIGHT -> DARK
            DARK -> NORMAL
        }
    }
}

private enum class RippleQualityMode(
    val title: String,
    val maxConcurrentRipples: Int,
    val pathSegments: Int
) {
    VERY_LOW(title = "VERY_LOW", maxConcurrentRipples = 3, pathSegments = 28),
    LOW(title = "LOW", maxConcurrentRipples = 3, pathSegments = 32),
    MEDIUM(title = "MEDIUM", maxConcurrentRipples = 4, pathSegments = 36),
    HIGH(title = "HIGH", maxConcurrentRipples = 5, pathSegments = 48);

    fun next(): RippleQualityMode {
        return when (this) {
            VERY_LOW -> LOW
            LOW -> MEDIUM
            MEDIUM -> HIGH
            HIGH -> VERY_LOW
        }
    }
}

@Composable
fun KeyboardPanelScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var urlInput by rememberSaveable { mutableStateOf(uiState.serverUrl) }
    var showOverlay by rememberSaveable { mutableStateOf(false) }
    var customBackgroundUri by rememberSaveable { mutableStateOf(loadBackgroundMediaUri(context)) }
    var keyTextMode by rememberSaveable {
        mutableStateOf(loadKeyTextMode(context))
    }
    var rippleQualityMode by rememberSaveable {
        mutableStateOf(loadRippleQualityMode(context))
    }

    val pickBackgroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            customBackgroundUri = uri.toString()
            saveBackgroundMediaUri(context, customBackgroundUri)
        }
    }

    LaunchedEffect(uiState.serverUrl) {
        if (!uiState.connecting) {
            urlInput = uiState.serverUrl
        }
    }

    BackHandler(enabled = true) {
        showOverlay = !showOverlay
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MediaOrGradientBackground(
            modifier = Modifier.fillMaxSize(),
            customBackgroundUri = customBackgroundUri
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            if (showOverlay) {
                QuickBar(
                    uiState = uiState,
                    onConnectClick = {
                        viewModel.setServerUrl(urlInput)
                        if (uiState.connected) viewModel.disconnect() else viewModel.connect()
                    },
                    onMuteClick = { viewModel.toggleMute() },
                    showControls = showOverlay,
                    onToggleControls = { showOverlay = !showOverlay }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ControlBar(
                    urlInput = urlInput,
                    onUrlChanged = {
                        urlInput = it
                        viewModel.setServerUrl(it)
                    },
                    hasCustomBackground = !customBackgroundUri.isNullOrBlank(),
                    backgroundStatus = backgroundStatusText(context, customBackgroundUri),
                    onPickBackground = { pickBackgroundLauncher.launch(arrayOf("video/*", "image/*")) },
                    onClearBackground = {
                        customBackgroundUri = null
                        saveBackgroundMediaUri(context, null)
                    },
                    keyTextMode = keyTextMode,
                    onTextModeToggle = {
                        keyTextMode = keyTextMode.next()
                        saveKeyTextMode(context, keyTextMode)
                    },
                    rippleQualityMode = rippleQualityMode,
                    onRippleModeToggle = {
                        rippleQualityMode = rippleQualityMode.next()
                        saveRippleQualityMode(context, rippleQualityMode)
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                StatusBar(uiState)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.pages.isNotEmpty()) {
                if (showOverlay) {
                    GlassTabBar(
                        pages = uiState.pages,
                        selectedPageIndex = uiState.selectedPageIndex,
                        onSelect = { viewModel.selectPage(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val page = uiState.pages[uiState.selectedPageIndex]
                if (!page.keyRows.isNullOrEmpty()) {
                    FluxRowKeyboard(
                        modifier = Modifier.weight(1f),
                        page = page,
                        stateFlags = uiState.stateFlags,
                        keyTextMode = keyTextMode,
                        rippleQualityMode = rippleQualityMode,
                        onKeyDown = { viewModel.onKeyPressed(it) },
                        onKeyRepeat = { viewModel.onKeyRepeated(it) },
                        onKeyUp = { viewModel.onKeyReleased(it) }
                    )
                } else {
                    FluxKeyGrid(
                        modifier = Modifier.weight(1f),
                        page = page,
                        stateFlags = uiState.stateFlags,
                        keyTextMode = keyTextMode,
                        rippleQualityMode = rippleQualityMode,
                        onTrigger = { key, longPress ->
                            viewModel.onKeyTriggered(key, longPress)
                        }
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No layout loaded")
                }
            }
        }
    }
}

@Composable
private fun GlassTabBar(
    pages: List<PageConfig>,
    selectedPageIndex: Int,
    onSelect: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        TabRow(
            selectedTabIndex = selectedPageIndex,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            divider = {},
            indicator = {}
        ) {
            pages.forEachIndexed { idx, page ->
                val selected = idx == selectedPageIndex
                Tab(
                    selected = selected,
                    onClick = { onSelect(idx) },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color.White.copy(alpha = 0.78f),
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = if (selected) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = GLASS_BUTTON_ALPHA + 0.08f),
                                                Color.White.copy(alpha = GLASS_BUTTON_ALPHA + 0.02f)
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = GLASS_BUTTON_ALPHA),
                                                Color.White.copy(alpha = GLASS_BUTTON_ALPHA)
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = page.title,
                                modifier = Modifier.align(Alignment.Center),
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                )
            }
        }
    }
}

private enum class BackgroundSourceKind {
    IMAGE,
    VIDEO,
    UNKNOWN
}

@Composable
private fun MediaOrGradientBackground(
    modifier: Modifier = Modifier,
    customBackgroundUri: String?
) {
    val context = LocalContext.current
    val customUri = remember(customBackgroundUri) { customBackgroundUri?.let(Uri::parse) }
    val customReadable = remember(customUri) { customUri?.let { isUriReadable(context, it) } == true }
    val customKind = remember(customUri) {
        if (customUri == null) {
            BackgroundSourceKind.UNKNOWN
        } else {
            detectBackgroundSourceKind(context, customUri)
        }
    }

    if (customReadable && customUri != null && customKind == BackgroundSourceKind.IMAGE) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    adjustViewBounds = false
                }
            },
            update = { imageView ->
                imageView.setImageURI(customUri)
            },
            modifier = modifier
        )
        return
    }

    val hasAssetVideo = remember {
        runCatching {
            context.assets.openFd("background.mp4").close()
            true
        }.getOrElse { false }
    }

    val mediaUri = when {
        customReadable && customUri != null && customKind == BackgroundSourceKind.VIDEO -> customUri
        hasAssetVideo -> Uri.parse("asset:///background.mp4")
        else -> null
    }

    if (mediaUri == null) {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0B1D2C),
                        Color(0xFF1C4968),
                        Color(0xFF153046),
                        Color(0xFF2A6F97)
                    )
                )
            )
        )
        return
    }

    val player = remember(mediaUri.toString(), context) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(mediaUri))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                this.player = player
            }
        },
        modifier = modifier
    )
}

@Composable
private fun FluxRowKeyboard(
    modifier: Modifier,
    page: PageConfig,
    stateFlags: Map<String, Boolean>,
    keyTextMode: KeyTextMode,
    rippleQualityMode: RippleQualityMode,
    onKeyDown: (KeyConfig) -> Unit,
    onKeyRepeat: (KeyConfig) -> Unit,
    onKeyUp: (KeyConfig) -> Unit
) {
    val rows = page.keyRows ?: return
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val pointerToKey = remember(page.id) { mutableStateMapOf<Long, String>() }
    val pointerToRippleId = remember(page.id) { mutableStateMapOf<Long, Int>() }
    val keyPressCount = remember(page.id) { mutableStateMapOf<String, Int>() }
    val keyRepeatJobs = remember(page.id) { mutableStateMapOf<String, Job>() }
    val interactionSources = remember(page.id) { mutableStateMapOf<String, MutableInteractionSource>() }
    val activePresses = remember(page.id) { mutableStateMapOf<String, PressInteraction.Press>() }
    val keyRootOffsets = remember(page.id) { mutableStateMapOf<String, Offset>() }
    val keyRippleRadius = remember(page.id) { mutableStateMapOf<String, Float>() }
    val backgroundRipples = remember(page.id) { mutableStateListOf<GlobalRipplePulse>() }
    var nextRippleId by remember(page.id) { mutableIntStateOf(1) }
    var keyboardRootOffset by remember(page.id) { mutableStateOf(Offset.Zero) }
    val pressStartRadiusPx = with(density) { 10.dp.toPx() }
    val ripple: Indication = rememberRipple(bounded = true)
    val repeatDelayMs = 360L
    val repeatIntervalMs = 52L
    val shiftActive by remember(rows, keyPressCount) {
        derivedStateOf {
            rows.flatten().any { rowKey ->
                (keyPressCount[rowKey.id] ?: 0) > 0 && isShiftKey(rowKey)
            }
        }
    }

    DisposableEffect(page.id) {
        onDispose {
            keyRepeatJobs.values.forEach { it.cancel() }
            keyRepeatJobs.clear()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { keyboardRootOffset = it.positionInRoot() }
    ) {
        BackgroundRippleLayer(
            ripples = backgroundRipples,
            rippleQualityMode = rippleQualityMode,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rows.forEach { rowKeys ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowKeys.forEach { key ->
                        val isPressed = (keyPressCount[key.id] ?: 0) > 0
                        val isActive = key.stateKey?.let { stateFlags[it] } == true

                        KeyTile(
                            key = key,
                            selected = isPressed,
                            active = isActive,
                            keyTextMode = keyTextMode,
                            shiftActive = shiftActive,
                            modifier = Modifier
                                .weight(key.width.coerceAtLeast(0.5f))
                                .onGloballyPositioned { coordinates ->
                                    keyRootOffsets[key.id] = coordinates.positionInRoot()
                                    val base = coordinates.size.height.toFloat()
                                    keyRippleRadius[key.id] = base * 1.38f
                                }
                                .indication(
                                    interactionSource = interactionSources.getOrPut(key.id) { MutableInteractionSource() },
                                    indication = ripple
                                )
                                .pointerInput(page.id, key.id) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val pointerId = down.id.value
                                        val alreadyTracked = pointerToKey.containsKey(pointerId)
                                        if (!alreadyTracked && pointerToKey.size >= 3) {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                                                if (!change.pressed) break
                                            }
                                            return@awaitEachGesture
                                        }

                                        pointerToKey[pointerId] = key.id
                                        val keyRoot = keyRootOffsets[key.id]
                                        val localOrigin = if (keyRoot != null) {
                                            (keyRoot - keyboardRootOffset) + down.position
                                        } else {
                                            down.position
                                        }
                                        val rippleId = emitGlobalRipple(
                                            scope = scope,
                                            ripples = backgroundRipples,
                                            id = nextRippleId++,
                                            origin = localOrigin,
                                            maxRadius = keyRippleRadius[key.id],
                                            startRadius = pressStartRadiusPx,
                                            maxConcurrentRipples = rippleQualityMode.maxConcurrentRipples
                                        )
                                        pointerToRippleId[pointerId] = rippleId

                                        keyPressCount[key.id] = (keyPressCount[key.id] ?: 0) + 1
                                        if ((keyPressCount[key.id] ?: 0) == 1) {
                                            val interactionSource = interactionSources.getOrPut(key.id) { MutableInteractionSource() }
                                            val press = PressInteraction.Press(down.position)
                                            activePresses[key.id] = press
                                            interactionSource.tryEmit(press)

                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onKeyDown(key)
                                            if (key.action.kind.name == "KEY") {
                                                keyRepeatJobs.remove(key.id)?.cancel()
                                                keyRepeatJobs[key.id] = scope.launch {
                                                    delay(repeatDelayMs)
                                                    while ((keyPressCount[key.id] ?: 0) > 0) {
                                                        onKeyRepeat(key)
                                                        delay(repeatIntervalMs)
                                                    }
                                                }
                                            }
                                        }

                                        while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                                        if (!change.pressed) {
                                            val releasedRippleId = pointerToRippleId.remove(pointerId)
                                            if (releasedRippleId != null) {
                                                releaseGlobalRipple(
                                                    scope = scope,
                                                    ripples = backgroundRipples,
                                                    rippleId = releasedRippleId
                                                )
                                            }
                                            pointerToKey.remove(pointerId)
                                            val left = (keyPressCount[key.id] ?: 1) - 1
                                            if (left <= 0) {
                                                    keyPressCount.remove(key.id)
                                                    keyRepeatJobs.remove(key.id)?.cancel()
                                                    val interactionSource = interactionSources.getOrPut(key.id) { MutableInteractionSource() }
                                                    val press = activePresses.remove(key.id)
                                                    if (press != null) {
                                                        interactionSource.tryEmit(PressInteraction.Release(press))
                                                    }
                                                    onKeyUp(key)
                                                } else {
                                                    keyPressCount[key.id] = left
                                                }
                                                break
                                            }
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickBar(
    uiState: UiState,
    onConnectClick: () -> Unit,
    onMuteClick: () -> Unit,
    showControls: Boolean,
    onToggleControls: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Keyboard",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )

        Button(onClick = onConnectClick) {
            Text(if (uiState.connected) "Disconnect" else "Connect")
        }

        OutlinedButton(
            onClick = onMuteClick,
            colors = glassOutlinedButtonColors(),
            border = glassOutlinedBorder()
        ) {
            Text(if (uiState.mute) "Unmute" else "Mute")
        }

        OutlinedButton(
            onClick = onToggleControls,
            colors = glassOutlinedButtonColors(),
            border = glassOutlinedBorder()
        ) {
            Text(if (showControls) "Hide URL" else "URL")
        }
    }
}

@Composable
private fun ControlBar(
    urlInput: String,
    onUrlChanged: (String) -> Unit,
    hasCustomBackground: Boolean,
    backgroundStatus: String,
    onPickBackground: () -> Unit,
    onClearBackground: () -> Unit,
    keyTextMode: KeyTextMode,
    onTextModeToggle: () -> Unit,
    rippleQualityMode: RippleQualityMode,
    onRippleModeToggle: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlChanged,
                label = { Text("Server WS URL") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = glassTextFieldColors()
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPickBackground,
                colors = glassOutlinedButtonColors(),
                border = glassOutlinedBorder()
            ) {
                Text("Import BG")
            }
            OutlinedButton(
                onClick = onClearBackground,
                enabled = hasCustomBackground,
                colors = glassOutlinedButtonColors(),
                border = glassOutlinedBorder()
            ) {
                Text("Clear BG")
            }
            OutlinedButton(
                onClick = onTextModeToggle,
                colors = glassOutlinedButtonColors(),
                border = glassOutlinedBorder()
            ) {
                Text("Text: ${keyTextMode.name}")
            }
            OutlinedButton(
                onClick = onRippleModeToggle,
                colors = glassOutlinedButtonColors(),
                border = glassOutlinedBorder()
            ) {
                Text("Ripple: ${rippleQualityMode.title}")
            }
            Text(
                text = backgroundStatus,
                style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
            )
        }
    }
}

@Composable
private fun StatusBar(uiState: UiState) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusItem("Conn", uiState.statusText)
            StatusItem("Mic", "${(uiState.micLevel * 100).toInt()}%")
            StatusItem("Mute", uiState.mute.toString())
            StatusItem("Layout", uiState.layoutName)
        }
        if (uiState.activeWindow.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Active: ${uiState.activeWindow}",
                style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
            )
        }
        if (uiState.lastEvent.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Event: ${uiState.lastEvent}",
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.92f))
            )
        }
    }
}

@Composable
private fun StatusItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.88f))
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FluxKeyGrid(
    modifier: Modifier,
    page: PageConfig,
    stateFlags: Map<String, Boolean>,
    keyTextMode: KeyTextMode,
    rippleQualityMode: RippleQualityMode,
    onTrigger: (key: KeyConfig, longPress: Boolean) -> Unit
) {
    val totalCells = page.rows * page.cols
    val cellList = remember(page.id, page.keys) {
        MutableList<KeyConfig?>(totalCells) { idx -> page.keys.getOrNull(idx) }
    }

    var highlightedIndex by remember(page.id) { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val moveThresholdPx = with(density) { 14.dp.toPx() }
    val spacingPx = with(density) { 8.dp.toPx() }
    val pressStartRadiusPx = with(density) { 10.dp.toPx() }
    val scope = rememberCoroutineScope()
    val backgroundRipples = remember(page.id) { mutableStateListOf<GlobalRipplePulse>() }
    var nextRippleId by remember(page.id) { mutableIntStateOf(1) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .pointerInput(page.id, page.rows, page.cols, page.keys) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    val downPos = down.position
                    val rippleId = emitGlobalRipple(
                        scope = scope,
                        ripples = backgroundRipples,
                        id = nextRippleId++,
                        origin = down.position,
                        maxRadius = ((size.width.toFloat() / page.cols.coerceAtLeast(1)) * 1.35f),
                        startRadius = pressStartRadiusPx,
                        maxConcurrentRipples = rippleQualityMode.maxConcurrentRipples
                    )
                    var activeIndex = indexFromPosition(
                        pos = down.position,
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                        rows = page.rows,
                        cols = page.cols,
                        spacing = spacingPx
                    )
                    highlightedIndex = activeIndex

                    var moved = false
                    var longTriggered = false
                    var longJob: Job? = scope.launch {
                        delay(420)
                        val key = cellList.getOrNull(activeIndex ?: -1)
                        if (!moved && key?.holdAction != null) {
                            longTriggered = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTrigger(key, true)
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: continue

                        if (!change.pressed) {
                            releaseGlobalRipple(
                                scope = scope,
                                ripples = backgroundRipples,
                                rippleId = rippleId
                            )
                            longJob?.cancel()
                            if (!longTriggered) {
                                val key = cellList.getOrNull(activeIndex ?: -1)
                                if (key != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTrigger(key, false)
                                }
                            }
                            highlightedIndex = null
                            break
                        }

                        val distance = (change.position - downPos).getDistance()
                        if (distance > moveThresholdPx) {
                            moved = true
                            longJob?.cancel()
                        }

                        if (moved) {
                            val next = indexFromPosition(
                                pos = change.position,
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                                rows = page.rows,
                                cols = page.cols,
                                spacing = spacingPx
                            )
                            if (next != activeIndex) {
                                activeIndex = next
                                highlightedIndex = next
                            }
                        }
                    }
                }
            }
    ) {
        BackgroundRippleLayer(
            ripples = backgroundRipples,
            rippleQualityMode = rippleQualityMode,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(page.rows) { row ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(page.cols) { col ->
                        val idx = row * page.cols + col
                        val key = cellList[idx]
                        KeyTile(
                            key = key,
                            selected = highlightedIndex == idx,
                            active = key?.stateKey?.let { stateFlags[it] } == true,
                            keyTextMode = keyTextMode,
                            shiftActive = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyTile(
    key: KeyConfig?,
    selected: Boolean,
    active: Boolean,
    keyTextMode: KeyTextMode,
    shiftActive: Boolean,
    modifier: Modifier = Modifier
) {
    val isCapsKey = remember(key?.id, key?.label, key?.action?.payload, key?.stateKey) {
        isCapsLockKey(key)
    }
    val capsOn = isCapsKey && active
    val capsLedAlpha by animateFloatAsState(
        targetValue = if (capsOn) 1f else 0f,
        animationSpec = tween(durationMillis = 140, easing = LinearOutSlowInEasing),
        label = "caps-led-alpha"
    )
    val glassColor by animateColorAsState(
        targetValue = when {
            selected -> Color.White.copy(alpha = GLASS_BUTTON_ALPHA + 0.08f)
            capsOn -> Color.White.copy(alpha = GLASS_BUTTON_ALPHA + 0.06f)
            active -> Color.White.copy(alpha = GLASS_BUTTON_ALPHA + 0.04f)
            else -> Color.White.copy(alpha = GLASS_BUTTON_ALPHA)
        },
        label = "key-bg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> Color.White.copy(alpha = 0.34f)
            capsOn -> Color.White.copy(alpha = 0.42f)
            else -> Color.White.copy(alpha = 0.18f)
        },
        label = "key-border"
    )

    val (textColor, textShadow) = remember(keyTextMode) {
        when (keyTextMode) {
            KeyTextMode.NORMAL -> {
                Color.White to Shadow(
                    color = Color.Black.copy(alpha = 0.85f),
                    offset = Offset(0f, 1.5f),
                    blurRadius = 2.5f
                )
            }
            KeyTextMode.LIGHT -> {
                Color.Black to Shadow(
                    color = Color.White.copy(alpha = 0.55f),
                    offset = Offset(0f, 1f),
                    blurRadius = 1.2f
                )
            }
            KeyTextMode.DARK -> Color.White to null
        }
    }
    val displayLabel = remember(key, shiftActive) { keyDisplayLabel(key, shiftActive) }

    Box(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(glassColor, RoundedCornerShape(12.dp))
                .border(
                    1.dp,
                    borderColor,
                    RoundedCornerShape(12.dp)
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (key == null) {
                Text("", modifier = Modifier.fillMaxSize())
            } else if ((key.icon ?: "").isBlank()) {
                Text(
                    text = displayLabel,
                    style = MaterialTheme.typography.titleMedium.copy(color = textColor, shadow = textShadow),
                    textAlign = TextAlign.Center
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = key.icon ?: "",
                        style = MaterialTheme.typography.headlineSmall.copy(color = textColor, shadow = textShadow),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.labelMedium.copy(color = textColor, shadow = textShadow),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (isCapsKey) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(7.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.08f + 0.80f * capsLedAlpha),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.18f + 0.58f * capsLedAlpha),
                        shape = CircleShape
                    )
            )
        }
    }
}

private fun isCapsLockKey(key: KeyConfig?): Boolean {
    if (key == null) return false
    if (key.stateKey.equals("caps_lock", ignoreCase = true)) return true
    if (key.id.equals("k_caps", ignoreCase = true)) return true
    if (key.label.equals("Caps", ignoreCase = true)) return true
    val payload = key.action.payload?.toString()?.trim('"')?.trim()?.uppercase()
    return payload == "CAPSLOCK"
}

private fun isShiftKey(key: KeyConfig?): Boolean {
    val token = actionToken(key)
    return token == "SHIFT" || token == "LSHIFT" || token == "RSHIFT"
}

private fun keyDisplayLabel(key: KeyConfig?, shiftActive: Boolean): String {
    if (key == null) return ""
    val original = key.label?.takeIf { it.isNotBlank() } ?: key.id
    val token = actionToken(key)
    if (token.isEmpty()) return original

    if (token.length == 1 && token[0] in 'A'..'Z') {
        return if (shiftActive) token else token.lowercase()
    }

    if (token.length == 1 && token[0] in '0'..'9') {
        return if (shiftActive) SHIFT_DIGIT_MAP[token] ?: original else token
    }

    val symbolPair = SHIFT_SYMBOL_TOKEN_MAP[token]
    if (symbolPair != null) {
        return if (shiftActive) symbolPair.second else symbolPair.first
    }

    return original
}

private fun actionToken(key: KeyConfig?): String {
    val raw = key?.action?.payload?.toString()?.trim() ?: return ""
    return raw.trim('"').uppercase()
}

private val SHIFT_DIGIT_MAP = mapOf(
    "1" to "!",
    "2" to "@",
    "3" to "#",
    "4" to "$",
    "5" to "%",
    "6" to "^",
    "7" to "&",
    "8" to "*",
    "9" to "(",
    "0" to ")"
)

private val SHIFT_SYMBOL_TOKEN_MAP = mapOf(
    "GRAVE" to ("`" to "~"),
    "MINUS" to ("-" to "_"),
    "EQUALS" to ("=" to "+"),
    "LBRACKET" to ("[" to "{"),
    "RBRACKET" to ("]" to "}"),
    "BACKSLASH" to ("\\" to "|"),
    "SEMICOLON" to (";" to ":"),
    "APOSTROPHE" to ("'" to "\""),
    "COMMA" to ("," to "<"),
    "PERIOD" to ("." to ">"),
    "SLASH" to ("/" to "?")
)

private fun indexFromPosition(
    pos: Offset,
    width: Float,
    height: Float,
    rows: Int,
    cols: Int,
    spacing: Float
): Int? {
    if (rows <= 0 || cols <= 0) return null
    val cellW = (width - spacing * (cols - 1)) / cols
    val cellH = (height - spacing * (rows - 1)) / rows
    if (cellW <= 0f || cellH <= 0f) return null

    val col = floor(pos.x / (cellW + spacing)).toInt()
    val row = floor(pos.y / (cellH + spacing)).toInt()
    if (row !in 0 until rows || col !in 0 until cols) return null

    val xStart = col * (cellW + spacing)
    val yStart = row * (cellH + spacing)
    if (pos.x > xStart + cellW || pos.y > yStart + cellH) return null

    return row * cols + col
}

private fun emitGlobalRipple(
    scope: CoroutineScope,
    ripples: SnapshotStateList<GlobalRipplePulse>,
    id: Int,
    origin: Offset,
    maxRadius: Float?,
    startRadius: Float,
    maxConcurrentRipples: Int
): Int {
    val resolvedMaxRadius = (maxRadius ?: 220f).coerceIn(110f, 420f)
    val resolvedStartRadius = startRadius.coerceIn(8f, 28f)
    val holdRadius = (resolvedMaxRadius * 0.38f).coerceAtLeast(resolvedStartRadius + 4f)
    val pulse = GlobalRipplePulse(
        id = id,
        origin = origin,
        startRadius = resolvedStartRadius,
        maxRadius = resolvedMaxRadius,
        startedAtMs = SystemClock.elapsedRealtime(),
        phase = mutableStateOf(RipplePhase.Press),
        radiusAnim = Animatable(resolvedStartRadius),
        alphaAnim = Animatable(0f),
        coreAlphaAnim = Animatable(0f)
    )
    ripples.add(pulse)
    while (ripples.size > maxConcurrentRipples.coerceIn(3, 8)) {
        val first = ripples.firstOrNull() ?: break
        if (first.phase.value == RipplePhase.Release) {
            ripples.removeAt(0)
        } else {
            break
        }
    }
    scope.launch {
        launch {
            pulse.radiusAnim.animateTo(
                targetValue = holdRadius,
                animationSpec = tween(durationMillis = 80, easing = FastOutLinearInEasing)
            )
        }
        launch {
            pulse.alphaAnim.animateTo(
                targetValue = 0.30f,
                animationSpec = tween(durationMillis = 56, easing = FastOutLinearInEasing)
            )
            if (pulse.phase.value == RipplePhase.Press) {
                pulse.alphaAnim.animateTo(
                    targetValue = 0.20f,
                    animationSpec = tween(durationMillis = 170, easing = LinearOutSlowInEasing)
                )
            }
        }
        launch {
            pulse.coreAlphaAnim.animateTo(
                targetValue = 0.20f,
                animationSpec = tween(durationMillis = 54, easing = FastOutLinearInEasing)
            )
            if (pulse.phase.value == RipplePhase.Press) {
                pulse.coreAlphaAnim.animateTo(
                    targetValue = 0.04f,
                    animationSpec = tween(durationMillis = 130, easing = LinearOutSlowInEasing)
                )
            }
        }
        if (pulse.phase.value == RipplePhase.Press) {
            pulse.phase.value = RipplePhase.Hold
        }
    }
    return id
}

private fun releaseGlobalRipple(
    scope: CoroutineScope,
    ripples: SnapshotStateList<GlobalRipplePulse>,
    rippleId: Int
) {
    val pulse = ripples.firstOrNull { it.id == rippleId } ?: return
    if (pulse.phase.value == RipplePhase.Release) {
        return
    }

    pulse.phase.value = RipplePhase.Release
    scope.launch {
        launch {
            pulse.radiusAnim.animateTo(
                targetValue = pulse.maxRadius,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }
        launch {
            pulse.alphaAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 760, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            pulse.coreAlphaAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing)
            )
        }
        delay(1120)
        ripples.removeAll { it.id == rippleId }
    }
}

private data class RippleRingSpec(
    val delayMs: Float,
    val travelMs: Float,
    val alphaScale: Float,
    val widthScale: Float,
    val startRadiusScale: Float,
    val irregularityFraction: Float
)

private data class RippleRingDrawState(
    val radius: Float,
    val alpha: Float,
    val strokeWidth: Float,
    val irregularityFraction: Float
)

private data class RippleRenderProfile(
    val ringSpecs: List<RippleRingSpec>,
    val alphaBoost: Float
)

private val WATER_RING_SPECS_OLD_HIGH = listOf(
    RippleRingSpec(delayMs = 0f, travelMs = 420f, alphaScale = 1.12f, widthScale = 1.20f, startRadiusScale = 1.00f, irregularityFraction = 0.012f),
    RippleRingSpec(delayMs = 70f, travelMs = 520f, alphaScale = 0.98f, widthScale = 1.08f, startRadiusScale = 1.08f, irregularityFraction = 0.014f),
    RippleRingSpec(delayMs = 140f, travelMs = 620f, alphaScale = 0.82f, widthScale = 0.96f, startRadiusScale = 1.16f, irregularityFraction = 0.016f),
    RippleRingSpec(delayMs = 220f, travelMs = 760f, alphaScale = 0.68f, widthScale = 0.90f, startRadiusScale = 1.22f, irregularityFraction = 0.017f)
)

private val WATER_RING_SPECS_OLD_MEDIUM = WATER_RING_SPECS_OLD_HIGH.take(3).mapIndexed { index, it ->
    it.copy(
        alphaScale = if (index == 0) it.alphaScale * 0.92f else it.alphaScale * 0.95f,
        widthScale = it.widthScale * 0.94f
    )
}

private val WATER_RING_SPECS_OLD_LOW = listOf(
    RippleRingSpec(delayMs = 0f, travelMs = 420f, alphaScale = 1.00f, widthScale = 1.00f, startRadiusScale = 1.02f, irregularityFraction = 0.010f),
    RippleRingSpec(delayMs = 120f, travelMs = 560f, alphaScale = 0.78f, widthScale = 0.90f, startRadiusScale = 1.13f, irregularityFraction = 0.011f)
)

private val WATER_RING_SPECS_VERY_LOW = listOf(
    RippleRingSpec(delayMs = 0f, travelMs = 390f, alphaScale = 0.86f, widthScale = 0.92f, startRadiusScale = 1.03f, irregularityFraction = 0.009f),
    RippleRingSpec(delayMs = 140f, travelMs = 540f, alphaScale = 0.62f, widthScale = 0.82f, startRadiusScale = 1.14f, irregularityFraction = 0.010f)
)

private fun rippleRenderProfile(mode: RippleQualityMode): RippleRenderProfile {
    return when (mode) {
        RippleQualityMode.VERY_LOW -> RippleRenderProfile(
            ringSpecs = WATER_RING_SPECS_VERY_LOW,
            alphaBoost = 0.90f
        )
        RippleQualityMode.LOW -> RippleRenderProfile(
            ringSpecs = WATER_RING_SPECS_VERY_LOW,
            alphaBoost = 0.94f
        )
        RippleQualityMode.MEDIUM -> RippleRenderProfile(
            ringSpecs = WATER_RING_SPECS_OLD_LOW,
            alphaBoost = 1.00f
        )
        RippleQualityMode.HIGH -> RippleRenderProfile(
            ringSpecs = WATER_RING_SPECS_OLD_MEDIUM,
            alphaBoost = 1.03f
        )
    }
}

@Composable
private fun rememberRippleFrameTimeMs(active: Boolean): Long {
    val frameTimeMs = remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(active) {
        if (!active) {
            frameTimeMs.longValue = SystemClock.elapsedRealtime()
            return@LaunchedEffect
        }
        while (isActive) {
            withFrameNanos { frameTimeMs.longValue = SystemClock.elapsedRealtime() }
        }
    }
    return frameTimeMs.longValue
}

private fun computeRingDrawState(
    pulse: GlobalRipplePulse,
    ageMs: Float,
    envelopeAlpha: Float,
    spec: RippleRingSpec
): RippleRingDrawState? {
    val raw = (ageMs - spec.delayMs) / spec.travelMs
    if (raw <= 0f) return null

    val progress = raw.coerceIn(0f, 1f)
    val eased = FastOutSlowInEasing.transform(progress)
    val startRadius = pulse.startRadius * spec.startRadiusScale
    val radius = lerpFloat(startRadius, pulse.maxRadius, eased)
    val life = (1f - progress).coerceIn(0f, 1f)
    val alpha = envelopeAlpha * spec.alphaScale * life.pow(1.35f)
    if (alpha <= 0.0015f) return null

    val baseStroke = (pulse.maxRadius * 0.0135f).coerceIn(1.6f, 5.2f)
    val strokeWidth = baseStroke * spec.widthScale * (0.88f + 0.12f * life)

    return RippleRingDrawState(
        radius = radius,
        alpha = alpha,
        strokeWidth = strokeWidth,
        irregularityFraction = spec.irregularityFraction
    )
}

private fun drawWaterRing(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    center: Offset,
    ringState: RippleRingDrawState,
    seed: Float,
    pathSegments: Int
) = with(drawScope) {
    if (ringState.radius <= 1f || ringState.alpha <= 0f) return@with

    val path = buildWaterRingPath(
        center = center,
        radius = ringState.radius,
        irregularityFraction = ringState.irregularityFraction,
        seed = seed,
        segments = pathSegments
    )

    drawPath(
        path = path,
        color = Color(0xFFE6F4FF).copy(alpha = ringState.alpha * 0.46f),
        style = Stroke(
            width = ringState.strokeWidth * 1.9f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
    drawPath(
        path = path,
        color = Color.White.copy(alpha = ringState.alpha),
        style = Stroke(
            width = ringState.strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun buildWaterRingPath(
    center: Offset,
    radius: Float,
    irregularityFraction: Float,
    seed: Float,
    segments: Int
): Path {
    val resolvedSegments = segments.coerceIn(24, 96)
    val amplitude = (radius * irregularityFraction).coerceIn(0.6f, 4.2f)
    val path = Path()
    for (i in 0..resolvedSegments) {
        val t = i.toFloat() / resolvedSegments.toFloat()
        val angle = (t * 2f * PI.toFloat())
        val radialOffset = amplitude * waterNoise(angle = angle, seed = seed)
        val pointRadius = radius + radialOffset
        val x = center.x + cos(angle) * pointRadius
        val y = center.y + sin(angle) * pointRadius
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
}

private fun waterNoise(angle: Float, seed: Float): Float {
    val n1 = sin(angle * 3f + seed * 0.71f)
    val n2 = sin(angle * 5f + seed * 1.17f + 0.55f) * 0.45f
    val n3 = cos(angle * 7f + seed * 0.43f + 1.10f) * 0.25f
    return ((n1 + n2 + n3) / 1.70f).coerceIn(-1f, 1f)
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

@Composable
private fun BackgroundRippleLayer(
    ripples: List<GlobalRipplePulse>,
    rippleQualityMode: RippleQualityMode,
    modifier: Modifier = Modifier
) {
    val renderProfile = remember(rippleQualityMode) { rippleRenderProfile(rippleQualityMode) }
    val nowMs = rememberRippleFrameTimeMs(active = ripples.isNotEmpty())
    Canvas(modifier = modifier) {
        ripples.forEach { pulse ->
            val envelopeAlpha = pulse.alphaAnim.value.coerceIn(0f, 1f)
            val coreAlpha = pulse.coreAlphaAnim.value.coerceIn(0f, 1f)
            if (envelopeAlpha <= 0f && coreAlpha <= 0f) {
                return@forEach
            }

            val ageMs = (nowMs - pulse.startedAtMs).coerceAtLeast(0L).toFloat()
            val boostedEnvelope = when (pulse.phase.value) {
                RipplePhase.Release -> max(envelopeAlpha, if (ageMs < 220f) 0.18f else 0f)
                RipplePhase.Press -> max(envelopeAlpha, 0.22f)
                RipplePhase.Hold -> envelopeAlpha
            }
            val phaseFactor = when (pulse.phase.value) {
                RipplePhase.Press -> 1.08f
                RipplePhase.Hold -> 1.00f
                RipplePhase.Release -> 1.00f
            }

            renderProfile.ringSpecs.forEachIndexed { index, spec ->
                val ringState = computeRingDrawState(
                    pulse = pulse,
                    ageMs = ageMs,
                    envelopeAlpha = boostedEnvelope * phaseFactor * renderProfile.alphaBoost,
                    spec = spec
                ) ?: return@forEachIndexed

                drawWaterRing(
                    drawScope = this,
                    center = pulse.origin,
                    ringState = ringState,
                    seed = pulse.id * 0.89f + index * 1.37f,
                    pathSegments = rippleQualityMode.pathSegments
                )
            }

            val splashProgress = (ageMs / 100f).coerceIn(0f, 1f)
            val splashAlpha = (coreAlpha * (1f - splashProgress)).coerceIn(0f, 0.08f)
            if (splashAlpha > 0.001f) {
                val splashRadius = lerpFloat(
                    start = pulse.startRadius * 0.42f,
                    stop = pulse.startRadius * 1.08f,
                    fraction = FastOutSlowInEasing.transform(splashProgress)
                )
                drawCircle(
                    color = Color.White.copy(alpha = splashAlpha),
                    center = pulse.origin,
                    radius = splashRadius,
                    style = Stroke(width = (pulse.startRadius * 0.18f).coerceIn(0.8f, 1.8f))
                )
            }
        }
    }
}

@Composable
private fun glassOutlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    containerColor = Color.White.copy(alpha = GLASS_BUTTON_ALPHA),
    contentColor = Color.White,
    disabledContainerColor = Color.White.copy(alpha = 0.05f),
    disabledContentColor = Color.White.copy(alpha = 0.45f)
)

@Composable
private fun glassOutlinedBorder() = androidx.compose.foundation.BorderStroke(
    1.dp,
    Color.White.copy(alpha = 0.18f)
)

@Composable
private fun glassTextFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedBorderColor = Color.White.copy(alpha = 0.30f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
    cursorColor = Color.White,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color.White.copy(alpha = 0.86f),
    unfocusedLabelColor = Color.White.copy(alpha = 0.62f)
)

private fun loadBackgroundMediaUri(context: Context): String? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(PREF_BG_MEDIA_URI, null)
        ?: prefs.getString(PREF_BG_VIDEO_URI, null)
}

private fun saveBackgroundMediaUri(context: Context, uri: String?) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
        .putString(PREF_BG_MEDIA_URI, uri)
        .putString(PREF_BG_VIDEO_URI, uri) // keep legacy key in sync
        .apply()
}

private fun backgroundStatusText(context: Context, uri: String?): String {
    val parsed = uri?.let(Uri::parse) ?: return "BG: Assets/Gradient"
    if (!isUriReadable(context, parsed)) {
        return "BG: Assets/Gradient"
    }
    return when (detectBackgroundSourceKind(context, parsed)) {
        BackgroundSourceKind.IMAGE -> "BG: Device Image"
        BackgroundSourceKind.VIDEO -> "BG: Device Video"
        BackgroundSourceKind.UNKNOWN -> "BG: Device Media"
    }
}

private fun isUriReadable(context: Context, uri: Uri): Boolean {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { }
        true
    }.getOrElse { false }
}

private fun detectBackgroundSourceKind(context: Context, uri: Uri): BackgroundSourceKind {
    val mime = runCatching { context.contentResolver.getType(uri).orEmpty() }.getOrElse { "" }
    if (mime.startsWith("image/")) return BackgroundSourceKind.IMAGE
    if (mime.startsWith("video/")) return BackgroundSourceKind.VIDEO

    val raw = uri.toString().lowercase()
    if (raw.endsWith(".jpg") || raw.endsWith(".jpeg") || raw.endsWith(".png") || raw.endsWith(".webp") || raw.endsWith(".bmp") || raw.endsWith(".gif")) {
        return BackgroundSourceKind.IMAGE
    }
    if (raw.endsWith(".mp4") || raw.endsWith(".mkv") || raw.endsWith(".webm") || raw.endsWith(".mov") || raw.endsWith(".3gp")) {
        return BackgroundSourceKind.VIDEO
    }
    return BackgroundSourceKind.UNKNOWN
}

private fun loadKeyTextMode(context: Context): KeyTextMode {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(PREF_TEXT_MODE, KeyTextMode.NORMAL.name) ?: KeyTextMode.NORMAL.name
    return runCatching { KeyTextMode.valueOf(raw) }.getOrElse { KeyTextMode.NORMAL }
}

private fun saveKeyTextMode(context: Context, mode: KeyTextMode) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(PREF_TEXT_MODE, mode.name).apply()
}

private fun loadRippleQualityMode(context: Context): RippleQualityMode {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(PREF_RIPPLE_MODE, RippleQualityMode.LOW.name) ?: RippleQualityMode.LOW.name
    return runCatching { RippleQualityMode.valueOf(raw) }.getOrElse { RippleQualityMode.LOW }
}

private fun saveRippleQualityMode(context: Context, mode: RippleQualityMode) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(PREF_RIPPLE_MODE, mode.name).apply()
}
