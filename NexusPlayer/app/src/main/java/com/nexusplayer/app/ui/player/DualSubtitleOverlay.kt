package com.nexusplayer.app.ui.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusplayer.app.domain.model.SubtitleSettings

@Composable
fun DualSubtitleOverlay(
    primaryText: String?,
    secondaryText: String?,
    settings: SubtitleSettings,
    modifier: Modifier = Modifier
) {
    if (primaryText.isNullOrEmpty() && secondaryText.isNullOrEmpty()) return

    val primaryColor = Color(settings.textColorHex)
    val secondaryColor = primaryColor.copy(alpha = 0.8f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(settings.backgroundColorHex)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (!primaryText.isNullOrEmpty()) {
                Text(
                    text = primaryText,
                    color = primaryColor,
                    fontSize = settings.fontSizeSp.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = (settings.fontSizeSp * 1.4f).sp
                )
            }
            if (!secondaryText.isNullOrEmpty()) {
                Text(
                    text = secondaryText,
                    color = secondaryColor,
                    fontSize = (settings.fontSizeSp * 0.85f).sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = (settings.fontSizeSp * 1.2f).sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
