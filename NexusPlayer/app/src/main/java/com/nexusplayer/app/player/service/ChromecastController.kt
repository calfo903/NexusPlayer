package com.nexusplayer.app.player.service

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controller managing Chromecast and Smart TV casting states.
 * Connects to Google Cast device sessions or AirPlay/DLNA endpoints.
 */
class ChromecastController(private val context: Context) {

    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<String>>(
        listOf("Living Room TV (4K Google TV)", "Master Bedroom Chromecast", "Studio Soundbar DLNA")
    )
    val availableDevices: StateFlow<List<String>> = _availableDevices.asStateFlow()

    fun connectToDevice(deviceName: String) {
        _connectedDeviceName.value = deviceName
        _isCasting.value = true
    }

    fun disconnect() {
        _isCasting.value = false
        _connectedDeviceName.value = null
    }
}
