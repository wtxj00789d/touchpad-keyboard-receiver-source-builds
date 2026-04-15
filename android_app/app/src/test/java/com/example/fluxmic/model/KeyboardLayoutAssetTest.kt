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
        assertTrue(
            "68-key layout should keep a 1.75u right Shift so the Up arrow can sit over the Down arrow column once rows stop stretching",
            json.contains("{\"id\":\"k_rshift\",\"label\":\"Shift\",\"w\":1.75,\"action\":{\"kind\":\"KEY\",\"payload\":\"RSHIFT\"}}")
        )
    }
}
