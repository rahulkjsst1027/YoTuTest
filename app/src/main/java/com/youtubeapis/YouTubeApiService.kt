package com.youtubeapis

import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {
    @GET("search")
    suspend fun searchChannelByName(
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "channel",
        @Query("q") query: String,
        @Query("key") apiKey: String,
        @Query("maxResults") maxResults: Int = 1,
        @Query("fields") fields: String = "items(id/channelId)"
    ): YouTubeResponse

    @GET("search")
    suspend fun searchVideosByChannel(
        @Query("part") part: String = "snippet",
        @Query("channelId") channelId: String,
        @Query("order") order: String = "date",
        @Query("type") type: String = "video",
        @Query("eventType") eventType: String? = null,
        @Query("maxResults") maxResults: Int = 1,
        @Query("key") apiKey: String,
        @Query("fields") fields: String = "items(id/videoId,snippet/title,snippet/publishedAt,snippet/liveBroadcastContent)"
    ): YouTubeResponse
}
