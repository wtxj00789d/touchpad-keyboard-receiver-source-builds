package com.example.fluxmic.model

import android.content.Context
import kotlinx.serialization.json.Json

class LayoutRepository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun listLayoutAssets(): List<String> {
        return runCatching {
            context.assets.list("layouts")
                ?.filter { it.endsWith(".json", ignoreCase = true) }
                ?.sorted()
                ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun loadLayoutByAssetName(fileName: String): LayoutConfig? {
        return runCatching {
            val text = context.assets.open("layouts/$fileName").bufferedReader().use { it.readText() }
            val normalized = text.removePrefix("\uFEFF")
            json.decodeFromString<LayoutConfig>(normalized)
        }.getOrNull()
    }

    fun loadByLayoutName(layoutName: String): LayoutConfig? {
        val normalized = layoutName.trim().lowercase()
        val candidate = listLayoutAssets().firstOrNull { it.removeSuffix(".json").lowercase() == normalized }
            ?: return null
        return loadLayoutByAssetName(candidate)
    }

    fun loadDefault(): LayoutConfig {
        val files = listLayoutAssets()
        val preferred = files.firstOrNull { it.equals("default.json", ignoreCase = true) }
            ?: files.firstOrNull()
            ?: error("No layout JSON found under assets/layouts")
        return loadLayoutByAssetName(preferred)
            ?: error("Failed to decode layout: $preferred")
    }
}
