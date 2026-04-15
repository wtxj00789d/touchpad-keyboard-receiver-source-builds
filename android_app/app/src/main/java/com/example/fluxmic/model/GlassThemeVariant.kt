package com.example.fluxmic.model

enum class GlassThemeVariant(
    val storedValue: String,
    val displayName: String
) {
    GLASS("glass", "Glass"),
    BORDERLESS("borderless", "Borderless");

    fun next(): GlassThemeVariant = when (this) {
        GLASS -> BORDERLESS
        BORDERLESS -> GLASS
    }

    companion object {
        fun fromStoredValue(raw: String?): GlassThemeVariant {
            return entries.firstOrNull { it.storedValue.equals(raw, ignoreCase = true) } ?: GLASS
        }
    }
}
