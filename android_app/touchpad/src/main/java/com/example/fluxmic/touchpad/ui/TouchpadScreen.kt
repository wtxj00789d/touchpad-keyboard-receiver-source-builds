package com.example.fluxmic.touchpad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fluxmic.touchpad.model.TouchpadUiState
import com.example.fluxmic.touchpad.ui.viewmodel.TouchpadViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TouchpadScreen(viewModel: TouchpadViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val gestureScope = rememberCoroutineScope()
    var urlInput by rememberSaveable { mutableStateOf(uiState.serverUrl) }

    LaunchedEffect(uiState.serverUrl, uiState.connecting) {
        if (!uiState.connecting) {
            urlInput = uiState.serverUrl
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B1623), Color(0xFF0F2132), Color(0xFF123047))
                )
            )
            .padding(12.dp)
    ) {
        ConnectionPanel(
            uiState = uiState,
            urlInput = urlInput,
            onUrlChanged = {
                urlInput = it
                viewModel.setServerUrl(it)
            },
            onConnectToggle = {
                viewModel.setServerUrl(urlInput)
                if (uiState.connected) viewModel.disconnect() else viewModel.connect()
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        TouchpadArea(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            connected = uiState.connected,
            gestureScope = gestureScope,
            onMove = viewModel::onMove,
            onScroll = viewModel::onScroll,
            onLeftClick = viewModel::leftClick,
            onRightClick = viewModel::rightClick,
            onDoubleClick = viewModel::doubleClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        ActionBar(
            connected = uiState.connected,
            dragging = uiState.dragging,
            onLeftClick = viewModel::leftClick,
            onRightClick = viewModel::rightClick,
            onDoubleClick = viewModel::doubleClick,
            onDragStart = viewModel::beginDrag,
            onDragEnd = viewModel::endDrag
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Gesture: tap=left click, double tap=double click, long press=right click, two-finger scroll/tap",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun ConnectionPanel(
    uiState: TouchpadUiState,
    urlInput: String,
    onUrlChanged: (String) -> Unit,
    onConnectToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "FluxMic Touchpad",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Server URL") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onConnectToggle, enabled = !uiState.connecting) {
                    Text(if (uiState.connected) "Disconnect" else "Connect")
                }
                Text(
                    text = buildStatusText(uiState),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            if (uiState.lastEvent.isNotBlank()) {
                Text(
                    text = uiState.lastEvent,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.74f)
                )
            }
        }
    }
}

private fun buildStatusText(uiState: TouchpadUiState): String {
    val rtt = uiState.rttMs?.let { "RTT ${it}ms" } ?: "RTT -"
    val jitter = uiState.jitterMs?.let { "Jitter ${it}ms" } ?: "Jitter -"
    return "${uiState.statusText} | $rtt | $jitter"
}

@Composable
private fun TouchpadArea(
    modifier: Modifier,
    connected: Boolean,
    gestureScope: CoroutineScope,
    onMove: (Float, Float) -> Unit,
    onScroll: (Int) -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    val shape = remember { RoundedCornerShape(16.dp) }

    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.07f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .pointerInput(connected) {
                if (!connected) {
                    return@pointerInput
                }

                var pendingTapJob: Job? = null
                var pendingTapTime = 0L
                var pendingTapPos = Offset.Unspecified

                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val startPos = firstDown.position
                    val downTime = firstDown.uptimeMillis
                    var activePointerId = firstDown.id
                    var lastSinglePos = firstDown.position
                    var lastTwoFingerAverageY: Float? = null
                    var maxPointerCount = 1
                    var moved = false
                    var scrolled = false
                    var longPressTriggered = false
                    var gestureEndTime = downTime
                    var inMultiTouch = false

                    var longPressJob: Job? = gestureScope.launch {
                        delay(LONG_PRESS_TIMEOUT_MS)
                        if (!moved && maxPointerCount == 1) {
                            longPressTriggered = true
                            onRightClick()
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (event.changes.isNotEmpty()) {
                            gestureEndTime = event.changes.maxOf { it.uptimeMillis }
                        }
                        if (pressed.isEmpty()) {
                            break
                        }

                        if (pressed.size > maxPointerCount) {
                            maxPointerCount = pressed.size
                        }

                        if (pressed.size >= 2) {
                            longPressJob?.cancel()
                            longPressJob = null
                            inMultiTouch = true

                            var sumY = 0f
                            pressed.forEach { sumY += it.position.y }
                            val avgY = sumY / pressed.size
                            val prevY = lastTwoFingerAverageY
                            if (prevY != null) {
                                val delta = prevY - avgY
                                if (abs(delta) >= 0.8f) {
                                    onScroll(delta.roundToInt())
                                    scrolled = true
                                }
                            }
                            lastTwoFingerAverageY = avgY
                            pressed.forEach { it.consume() }
                            continue
                        }

                        lastTwoFingerAverageY = null
                        val pointer = pressed.firstOrNull { it.id == activePointerId } ?: pressed.first()
                        activePointerId = pointer.id

                        // After a two-finger gesture, reset the single-finger baseline to avoid
                        // a large synthetic delta on the first frame back in single-touch mode.
                        if (inMultiTouch) {
                            inMultiTouch = false
                            lastSinglePos = pointer.position
                            pointer.consume()
                            continue
                        }

                        val delta = pointer.position - lastSinglePos
                        if (abs(delta.x) >= 0.2f || abs(delta.y) >= 0.2f) {
                            onMove(delta.x, delta.y)
                        }
                        if (!moved && isBeyondTapSlop(startPos, pointer.position)) {
                            moved = true
                            longPressJob?.cancel()
                            longPressJob = null
                        }

                        lastSinglePos = pointer.position
                        pointer.consume()
                    }

                    longPressJob?.cancel()

                    if (longPressTriggered) {
                        pendingTapJob?.cancel()
                        pendingTapJob = null
                        pendingTapTime = 0L
                        pendingTapPos = Offset.Unspecified
                        return@awaitEachGesture
                    }

                    if (maxPointerCount >= 2) {
                        if (!scrolled && gestureEndTime - downTime <= TWO_FINGER_TAP_MAX_MS) {
                            onRightClick()
                        }
                        return@awaitEachGesture
                    }

                    if (moved) {
                        return@awaitEachGesture
                    }

                    val isDoubleTap = pendingTapJob != null &&
                        pendingTapPos != Offset.Unspecified &&
                        gestureEndTime - pendingTapTime <= DOUBLE_TAP_TIMEOUT_MS &&
                        isWithinDoubleTapDistance(pendingTapPos, startPos)

                    if (isDoubleTap) {
                        pendingTapJob?.cancel()
                        pendingTapJob = null
                        pendingTapTime = 0L
                        pendingTapPos = Offset.Unspecified
                        onDoubleClick()
                    } else {
                        pendingTapJob?.cancel()
                        pendingTapTime = gestureEndTime
                        pendingTapPos = startPos
                        pendingTapJob = gestureScope.launch {
                            delay(DOUBLE_TAP_TIMEOUT_MS)
                            onLeftClick()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (connected) "Touchpad Active" else "Connect to activate touchpad",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.84f),
            fontWeight = FontWeight.Medium
        )
    }
}

private fun isBeyondTapSlop(start: Offset, current: Offset): Boolean {
    return abs(current.x - start.x) > TAP_SLOP_PX || abs(current.y - start.y) > TAP_SLOP_PX
}

private fun isWithinDoubleTapDistance(previous: Offset, current: Offset): Boolean {
    return abs(current.x - previous.x) <= DOUBLE_TAP_SLOP_PX &&
        abs(current.y - previous.y) <= DOUBLE_TAP_SLOP_PX
}

private const val TAP_SLOP_PX = 10f
private const val DOUBLE_TAP_SLOP_PX = 44f
private const val LONG_PRESS_TIMEOUT_MS = 420L
private const val DOUBLE_TAP_TIMEOUT_MS = 260L
private const val TWO_FINGER_TAP_MAX_MS = 240L

@Composable
private fun ActionBar(
    connected: Boolean,
    dragging: Boolean,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onLeftClick,
            enabled = connected,
            modifier = Modifier.weight(1f)
        ) {
            Text("Left")
        }

        OutlinedButton(
            onClick = onRightClick,
            enabled = connected,
            modifier = Modifier.weight(1f)
        ) {
            Text("Right")
        }

        OutlinedButton(
            onClick = onDoubleClick,
            enabled = connected,
            modifier = Modifier.weight(1f)
        ) {
            Text("Double")
        }

        DragHoldButton(
            enabled = connected,
            dragging = dragging,
            onDragStart = onDragStart,
            onDragEnd = onDragEnd,
            modifier = Modifier.weight(1.25f)
        )
    }
}

@Composable
private fun DragHoldButton(
    enabled: Boolean,
    dragging: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = remember { RoundedCornerShape(12.dp) }
    val containerColor = if (dragging) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    }
    val contentColor = if (dragging) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        Color.White
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .background(
                color = if (enabled) containerColor else Color.White.copy(alpha = 0.2f),
                shape = shape
            )
            .border(1.dp, Color.White.copy(alpha = 0.2f), shape)
            .pointerInput(enabled) {
                if (!enabled) {
                    return@pointerInput
                }

                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onDragStart()
                    waitForUpOrCancellation()
                    onDragEnd()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (dragging) "Dragging..." else "Hold To Drag",
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
