package com.youtubeapis

data class YouTubeResponse(
    val items: List<Item>
)

data class Item(
    val id: Id,
    val snippet: Snippet
)

data class Id(
    val videoId: String? = null,
    val channelId: String? = null
)

data class Snippet(
    val title: String,
    val publishedAt: String,
    val liveBroadcastContent: String
)

