package com.nexusplayer.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nexusplayer.app.domain.model.EqualizerState

@Composable
fun EqualizerDialog(
    state: EqualizerState,
    volumeBoostGainMb: Int = 0,
    onEnableToggle: (Boolean) -> Unit,
    onBandLevelChanged: (Short, Short) -> Unit,
    onBassBoostChanged: (Short) -> Unit,
    onVirtualizerChanged: (Short) -> Unit,
    onVolumeBoostChanged: (Int) -> Unit = {},
    onPresetSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var expandedPresets by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header & Enable Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Studio Equalizer & Effects",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.isEnabled) "DSP Engine Active (AudioSession)" else "Effects Disabled",
                            color = if (state.isEnabled) Color(0xFF10B981) else Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = onEnableToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF3B82F6),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF1E293B)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Presets Dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Preset Mode:", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .clickable { expandedPresets = true }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            val presetName = if (state.currentPresetIndex in state.presets.indices) {
                                state.presets[state.currentPresetIndex]
                            } else "Custom / Manual"
                            Text(
                                text = presetName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        DropdownMenu(
                            expanded = expandedPresets,
                            onDismissRequest = { expandedPresets = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            state.presets.forEachIndexed { idx, name ->
                                DropdownMenuItem(
                                    text = { Text(name, color = Color.White) },
                                    onClick = {
                                        expandedPresets = false
                                        onPresetSelected(idx)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bands Sliders
                Text(
                    text = "Frequency Bands (±15dB)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                state.bands.forEach { band ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = band.formattedFreq, color = Color(0xFF60A5FA), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            val db = band.currentLevelMillibels / 100f
                            Text(
                                text = "${if (db >= 0) "+" else ""}${String.format("%.1f", db)} dB",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                        Slider(
                            value = band.currentLevelMillibels.toFloat(),
                            onValueChange = { valShort ->
                                onBandLevelChanged(band.index, valShort.toInt().toShort())
                            },
                            valueRange = band.minLevelMillibels.toFloat()..band.maxLevelMillibels.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF3B82F6),
                                activeTrackColor = Color(0xFF3B82F6),
                                inactiveTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bass Boost & Virtualizer
                Text(
                    text = "Acoustic Enhancement",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Bass Boost", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Text(text = "${(state.bassBoostStrength / 10)}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = state.bassBoostStrength.toFloat(),
                        onValueChange = { valShort -> onBassBoostChanged(valShort.toInt().toShort()) },
                        valueRange = 0f..1000f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF3B82F6), activeTrackColor = Color(0xFF3B82F6))
                    )
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Virtualizer / 3D Surround", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Text(text = "${(state.virtualizerStrength / 10)}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = state.virtualizerStrength.toFloat(),
                        onValueChange = { valShort -> onVirtualizerChanged(valShort.toInt().toShort()) },
                        valueRange = 0f..1000f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF3B82F6), activeTrackColor = Color(0xFF3B82F6))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // VLC-Style 200% Volume Boost
                Text(
                    text = "VLC-Style 200% Digital Volume Boost",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B))
                        .padding(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            val perc = 100 + (volumeBoostGainMb / 20)
                            Text(
                                text = "Loudness Gain: ${perc}% Normal",
                                color = if (perc > 100) Color(0xFFF59E0B) else Color(0xFF60A5FA),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "+${String.format("%.1f", volumeBoostGainMb / 100f)} dB hardware digital gain",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        if (volumeBoostGainMb > 0) {
                            Text(
                                text = "Reset 100%",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onVolumeBoostChanged(0) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Slider(
                        value = volumeBoostGainMb.toFloat(),
                        onValueChange = { valFloat -> onVolumeBoostChanged(valFloat.toInt()) },
                        valueRange = 0f..2000f,
                        colors = SliderDefaults.colors(
                            thumbColor = if (volumeBoostGainMb > 1000) Color(0xFFF59E0B) else Color(0xFF3B82F6),
                            activeTrackColor = if (volumeBoostGainMb > 1000) Color(0xFFF59E0B) else Color(0xFF3B82F6)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text(text = "Done & Apply", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
