package com.nexusplayer.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusplayer.app.domain.model.SubtitleSettings

@Composable
fun SubtitleRendererOverlay(
    subtitleText: String?,
    settings: SubtitleSettings,
    modifier: Modifier = Modifier
) {
    if (!settings.isEnabled || subtitleText.isNullOrEmpty()) return

    val textColor = Color(settings.textColorHex)
    val bgColor = Color(settings.backgroundColorHex)
    val strokeColor = Color(settings.strokeColorHex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = settings.bottomMarginDp.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = !subtitleText.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = subtitleText,
                    color = textColor,
                    fontSize = settings.fontSizeSp.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = strokeColor,
                            blurRadius = settings.strokeWidthPx * 2f
                        )
                    )
                )
            }
        }
    }
}
