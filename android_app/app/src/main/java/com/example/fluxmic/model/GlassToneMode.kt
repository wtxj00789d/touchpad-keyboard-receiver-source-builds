package com.example.fluxmic.model

enum class GlassToneMode(
    val storedValue: String,
    private vararg val legacyAliases: String
) {
    NORMAL(
        storedValue = "normal",
        "KEY_SURFACE_NORMAL",
        "KEY_TEXT_NORMAL",
        "KEYSURFACEMODE.NORMAL",
        "KEYTEXTMODE.NORMAL"
    ),
    LIGHT(
        storedValue = "light",
        "KEY_SURFACE_LIGHT",
        "KEY_TEXT_LIGHT",
        "KEYSURFACEMODE.LIGHT",
        "KEYTEXTMODE.LIGHT"
    ),
    DARK(
        storedValue = "dark",
        "KEY_SURFACE_DARK",
        "KEY_TEXT_DARK",
        "KEYSURFACEMODE.DARK",
        "KEYTEXTMODE.DARK"
    );

    fun next(): GlassToneMode {
        return when (this) {
            NORMAL -> LIGHT
            LIGHT -> DARK
            DARK -> NORMAL
        }
    }

    companion object {
        private val modeByLookupKey: Map<String, GlassToneMode> = buildMap {
            GlassToneMode.entries.forEach { mode ->
                put(normalize(mode.storedValue), mode)
                put(normalize(mode.name), mode)
                mode.legacyAliases.forEach { alias ->
                    put(normalize(alias), mode)
                }
            }
        }

        fun fromStoredValue(raw: String?): GlassToneMode {
            val normalized = raw
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let(::normalize)
                ?: return NORMAL

            return modeByLookupKey[normalized] ?: NORMAL
        }

        private fun normalize(value: String): String {
            return value
                .trim()
                .replace('-', '_')
                .replace(' ', '_')
                .uppercase()
        }
    }
}
