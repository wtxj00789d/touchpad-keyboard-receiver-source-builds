package com.example.fluxmic.ui.viewmodel

import com.example.fluxmic.model.ExternalConnectCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UsbConnectCommandTest {
    @Test
    fun usbConnectCommandUpdatesAddressAndStartsConnecting() {
        val viewModel = MainViewModel(RuntimeEnvironment.getApplication())

        viewModel.toggleAddressEditor()
        viewModel.handleExternalConnectCommand(
            ExternalConnectCommand("ws://127.0.0.1:8765", autoConnect = true)
        )

        val uiState = viewModel.uiState.value
        assertEquals("ws://127.0.0.1:8765", uiState.serverUrl)
        assertEquals("127.0.0.1:8765", uiState.serverAddressDraft)
        assertFalse(uiState.addressEditorExpanded)
        assertTrue(uiState.connecting)
    }
}
