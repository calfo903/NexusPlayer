package com.nexusplayer.app.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Environment
import com.nexusplayer.app.domain.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ScreenshotHelper {

    suspend fun captureFrameAndSave(
        context: Context,
        videoItem: VideoItem,
        positionMs: Long
    ): File? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoItem.uri)
            // position in microseconds
            val bitmap = retriever.getFrameAtTime(positionMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (bitmap != null) {
                val picturesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "NexusPlayer")
                if (!picturesDir.exists()) picturesDir.mkdirs()

                val file = File(picturesDir, "nexus_frame_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                bitmap.recycle()
                return@withContext file
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
    }
}
