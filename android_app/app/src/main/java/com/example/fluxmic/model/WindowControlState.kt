package com.example.fluxmic.model

data class WindowControlState(
    val available: Boolean = false,
    val canMinimize: Boolean = false,
    val canMaximize: Boolean = false,
    val canClose: Boolean = false,
    val isMaximized: Boolean = false
) {
    val hasAnyAction: Boolean
        get() = available && (canMinimize || canMaximize || canClose)

    fun maximizeButtonLabel(): String {
        return if (isMaximized) "Restore" else "Maximize"
    }

    fun maximizeControlOp(): String {
        return if (isMaximized) "window_restore" else "window_maximize"
    }
}
