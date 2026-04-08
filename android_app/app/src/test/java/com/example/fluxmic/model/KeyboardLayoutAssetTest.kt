package com.example.fluxmic.model

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutAssetTest {
    private val layoutFile = File("src/main/assets/layouts/default68.json")

    @Test
    fun default68KeepsUpArrowAnchoredOverDownArrowColumn() {
        val json = layoutFile.readText()

        assertTrue(
            "68-key layout should include a right Ctrl before the arrow cluster so Up aligns over Down",
            json.contains("{\"id\":\"k_rctrl\",\"label\":\"Ctrl\",\"w\":1.25,\"action\":{\"kind\":\"KEY\",\"payload\":\"RCTRL\"}}")
        )
    }
}
