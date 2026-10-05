package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Profile : Screen("profile")
    data object Discovery : Screen("discovery")
    data object ChannelChat : Screen("channel_chat/{channelId}") {
        fun createRoute(channelId: String): String = "channel_chat/$channelId"
    }
    data object DirectChat : Screen("direct_chat/{conversationId}/{otherUserId}") {
        fun createRoute(conversationId: String, otherUserId: String): String = "direct_chat/$conversationId/$otherUserId"
    }
}
