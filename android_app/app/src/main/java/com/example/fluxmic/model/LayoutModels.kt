package com.example.fluxmic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LayoutConfig(
    val name: String,
    val pages: List<PageConfig>
)

@Serializable
data class PageConfig(
    val id: String,
    val title: String,
    val rows: Int = 0,
    val cols: Int = 0,
    val keys: List<KeyConfig> = emptyList(),
    @SerialName("key_rows") val keyRows: List<List<KeyConfig>>? = null
)

@Serializable
data class KeyConfig(
    val id: String,
    val label: String? = null,
    val icon: String? = null,
    val action: ActionSpec,
    @SerialName("hold_action") val holdAction: ActionSpec? = null,
    @SerialName("state_key") val stateKey: String? = null,
    @SerialName("w") val width: Float = 1f
)

@Serializable
data class ActionSpec(
    val kind: ActionKind,
    val payload: JsonElement? = null
)

@Serializable
enum class ActionKind {
    KEY,
    TEXT,
    MACRO,
    MOUSE,
    CMD,
    TOGGLE
}
