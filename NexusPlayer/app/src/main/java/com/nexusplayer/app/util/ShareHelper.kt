package com.nexusplayer.app.util

import android.content.Context
import android.content.Intent
import com.nexusplayer.app.domain.model.VideoItem

object ShareHelper {

    fun shareVideoTimestamp(context: Context, videoItem: VideoItem, positionMs: Long) {
        val timestamp = VideoItem.formatDuration(positionMs)
        val shareText = buildString {
            append("${videoItem.title}")
            append("\nWatching at $timestamp")
            if (!videoItem.isNetworkStream) {
                append("\nFile: ${videoItem.displayName}")
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, videoItem.title)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    fun shareVideoFile(context: Context, videoItem: VideoItem) {
        if (videoItem.isNetworkStream) {
            shareVideoTimestamp(context, videoItem, 0L)
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = videoItem.mimeType
            putExtra(Intent.EXTRA_STREAM, videoItem.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Video"))
    }

    fun openVideoExternally(context: Context, videoItem: VideoItem) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(videoItem.uri, videoItem.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
