package com.nexusplayer.app.player.subtitle

import java.io.File
import java.io.RandomAccessFile

object SubtitleHashMatcher {

    fun computeVideoHash(file: File): String? {
        if (!file.exists() || file.length() < 131072) return null

        try {
            RandomAccessFile(file, "r").use { raf ->
                val size = raf.length()
                var hash = size

                val buffer = ByteArray(8192)

                var bytesRead = 0L
                raf.seek(0)
                while (bytesRead < 65536) {
                    val toRead = minOf(buffer.size.toLong(), 65536 - bytesRead).toInt()
                    val read = raf.read(buffer, 0, toRead)
                    if (read == -1) break
                    for (i in 0 until read step 8) {
                        if (i + 8 <= read) {
                            var chunk = 0L
                            for (j in 0..7) {
                                chunk = chunk or (buffer[i + j].toLong() and 0xFF shl (j * 8))
                            }
                            hash += chunk
                        }
                    }
                    bytesRead += read
                }

                raf.seek(size - 65536)
                bytesRead = 0
                while (bytesRead < 65536) {
                    val toRead = minOf(buffer.size.toLong(), 65536 - bytesRead).toInt()
                    val read = raf.read(buffer, 0, toRead)
                    if (read == -1) break
                    for (i in 0 until read step 8) {
                        if (i + 8 <= read) {
                            var chunk = 0L
                            for (j in 0..7) {
                                chunk = chunk or (buffer[i + j].toLong() and 0xFF shl (j * 8))
                            }
                            hash += chunk
                        }
                    }
                    bytesRead += read
                }

                return String.format("%016x", hash)
            }
        } catch (e: Exception) {
            return null
        }
    }

    fun findMatchingSubtitles(videoFile: File): List<File> {
        val parentDir = videoFile.parentFile ?: return emptyList()
        val baseName = videoFile.nameWithoutExtension

        return parentDir.listFiles()
            ?.filter { file ->
                val ext = file.extension.lowercase()
                ext in listOf("srt", "vtt", "ass", "ssa") &&
                (file.nameWithoutExtension.startsWith(baseName, ignoreCase = true) ||
                 baseName.startsWith(file.nameWithoutExtension, ignoreCase = true))
            }
            ?.sortedBy { it.length() }
            ?: emptyList()
    }
}
