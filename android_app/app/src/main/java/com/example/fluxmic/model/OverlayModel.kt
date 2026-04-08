package com.example.fluxmic.model

data class OverlayAction(
    val id: String,
    val title: String
)

data class OverlayModel(
    val windowTitle: String,
    val contextActions: List<OverlayAction>,
    val showVolumeCapsule: Boolean,
    val usingFallbackActions: Boolean,
    val volumeLevel: Float
) {
    companion object {
        fun fromState(
            activeWindow: String,
            windowControls: WindowControlState,
            connected: Boolean,
            muted: Boolean,
            volume: Float
        ): OverlayModel {
            val windowActions = buildList {
                if (windowControls.available && windowControls.canMinimize) {
                    add(OverlayAction(id = "minimize", title = "Min"))
                }
                if (windowControls.available && windowControls.canMaximize) {
                    add(
                        OverlayAction(
                            id = "maximize",
                            title = windowControls.maximizeButtonLabel()
                        )
                    )
                }
                if (windowControls.available && windowControls.canClose) {
                    add(OverlayAction(id = "close", title = "Close"))
                }
            }

            val usingFallbackActions = windowActions.isEmpty()
            val fallbackActions = listOf(
                OverlayAction(
                    id = "connection",
                    title = if (connected) "Disconnect" else "Connect"
                ),
                OverlayAction(
                    id = "mute",
                    title = if (muted) "Unmute" else "Mute"
                ),
                OverlayAction(id = "volume", title = "Volume")
            )

            return OverlayModel(
                windowTitle = activeWindow.ifBlank { "Desktop" },
                contextActions = if (usingFallbackActions) fallbackActions else windowActions,
                showVolumeCapsule = usingFallbackActions,
                usingFallbackActions = usingFallbackActions,
                volumeLevel = volume.coerceIn(0f, 1f)
            )
        }
    }
}
