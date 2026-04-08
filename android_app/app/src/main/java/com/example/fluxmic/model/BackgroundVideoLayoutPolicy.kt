package com.example.fluxmic.model

data class BackgroundVideoLayoutState(
    val hasValidVideoSize: Boolean = false,
    val firstFrameRendered: Boolean = false
)

data class BackgroundVideoLayoutResult(
    val state: BackgroundVideoLayoutState,
    val shouldRequestLayout: Boolean
)

object BackgroundVideoLayoutPolicy {
    fun reduce(
        previousState: BackgroundVideoLayoutState,
        videoWidth: Int,
        videoHeight: Int,
        firstFrameRendered: Boolean
    ): BackgroundVideoLayoutResult {
        val hasValidVideoSize = videoWidth > 0 && videoHeight > 0
        val nextState = previousState.copy(
            hasValidVideoSize = previousState.hasValidVideoSize || hasValidVideoSize,
            firstFrameRendered = previousState.firstFrameRendered || firstFrameRendered
        )
        val shouldRequestLayout =
            (!previousState.hasValidVideoSize && nextState.hasValidVideoSize) ||
                (!previousState.firstFrameRendered && nextState.firstFrameRendered && nextState.hasValidVideoSize)

        return BackgroundVideoLayoutResult(
            state = nextState,
            shouldRequestLayout = shouldRequestLayout
        )
    }
}
