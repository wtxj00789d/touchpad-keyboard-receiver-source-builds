package com.example.fluxmic.model

enum class KeyboardLayoutMode(
    val storedValue: String,
    val assetName: String,
    val displayName: String
) {
    LAYOUT_60("layout_60", "default.json", "60%"),
    LAYOUT_68("layout_68", "default68.json", "68-Key");

    fun next(): KeyboardLayoutMode {
        return when (this) {
            LAYOUT_60 -> LAYOUT_68
            LAYOUT_68 -> LAYOUT_60
        }
    }

    companion object {
        fun fromStoredValue(value: String?): KeyboardLayoutMode {
            return entries.firstOrNull { it.storedValue == value } ?: LAYOUT_60
        }
    }
}
