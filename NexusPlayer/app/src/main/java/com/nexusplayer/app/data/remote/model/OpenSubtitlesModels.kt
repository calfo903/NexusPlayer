package com.nexusplayer.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class SubtitleSearchResponse(
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("data") val data: List<SubtitleItemData> = emptyList()
)

data class SubtitleItemData(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("attributes") val attributes: SubtitleAttributes
)

data class SubtitleAttributes(
    @SerializedName("subtitle_id") val subtitleId: String,
    @SerializedName("language") val language: String,
    @SerializedName("download_count") val downloadCount: Int,
    @SerializedName("ratings") val ratings: Float,
    @SerializedName("from_trusted") val fromTrusted: Boolean,
    @SerializedName("release") val release: String,
    @SerializedName("files") val files: List<SubtitleFile> = emptyList()
)

data class SubtitleFile(
    @SerializedName("file_id") val fileId: Int,
    @SerializedName("file_name") val fileName: String
)

data class DownloadLinkRequest(
    @SerializedName("file_id") val fileId: Int
)

data class DownloadLinkResponse(
    @SerializedName("link") val link: String,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("requests") val requests: Int,
    @SerializedName("remaining") val remaining: Int
)
