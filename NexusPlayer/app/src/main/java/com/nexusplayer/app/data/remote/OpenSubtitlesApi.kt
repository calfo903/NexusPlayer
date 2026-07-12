package com.nexusplayer.app.data.remote

import com.nexusplayer.app.data.remote.model.DownloadLinkRequest
import com.nexusplayer.app.data.remote.model.DownloadLinkResponse
import com.nexusplayer.app.data.remote.model.SubtitleSearchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface OpenSubtitlesApi {
    @GET("subtitles")
    suspend fun searchSubtitles(
        @Header("Api-Key") apiKey: String = "mock_open_subtitles_api_key_nexus_2026",
        @Query("query") query: String,
        @Query("languages") languages: String = "en,es,fr,de,ja",
        @Query("order_by") orderBy: String = "download_count"
    ): Response<SubtitleSearchResponse>

    @POST("download")
    suspend fun getDownloadLink(
        @Header("Api-Key") apiKey: String = "mock_open_subtitles_api_key_nexus_2026",
        @Body request: DownloadLinkRequest
    ): Response<DownloadLinkResponse>
}
