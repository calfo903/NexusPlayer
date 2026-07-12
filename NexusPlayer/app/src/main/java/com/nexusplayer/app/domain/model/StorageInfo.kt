package com.nexusplayer.app.domain.model

data class StorageInfo(
    val folderName: String,
    val totalSizeBytes: Long,
    val videoCount: Int
)
