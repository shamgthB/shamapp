package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class UsernameRegistration(
    val username: String = "",
    val userId: String = "",
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "username" to username,
        "userId" to userId,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val status: String = "Online",
    val about: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "displayName" to displayName,
            "email" to email,
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (username.isNotBlank()) map["username"] = username.lowercase()
        photoUrl?.let { map["photoUrl"] = it }
        about?.let { map["about"] = it }
        return map
    }

    fun toUpdateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "displayName" to displayName,
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (username.isNotBlank()) map["username"] = username.lowercase()
        photoUrl?.let { map["photoUrl"] = it }
        about?.let { map["about"] = it }
        return map
    }
}

data class ConnectionRequest(
    val requestId: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserPhotoUrl: String? = null,
    val toUserId: String = "",
    val toUserName: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, DECLINED
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "requestId" to requestId,
            "fromUserId" to fromUserId,
            "fromUserName" to fromUserName,
            "toUserId" to toUserId,
            "toUserName" to toUserName,
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp()
        )
        fromUserPhotoUrl?.let { map["fromUserPhotoUrl"] = it }
        return map
    }
}

data class Channel(
    val channelId: String = "",
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val memberCount: Int = 1
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "channelId" to channelId,
            "name" to name,
            "description" to description,
            "createdBy" to createdBy,
            "createdAt" to FieldValue.serverTimestamp(),
            "memberCount" to memberCount
        )
    }
}

data class ChannelMessage(
    val messageId: String = "",
    val channelId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String? = null,
    val text: String = "",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "messageId" to messageId,
            "channelId" to channelId,
            "senderId" to senderId,
            "senderName" to senderName,
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )
        senderPhotoUrl?.let { map["senderPhotoUrl"] = it }
        return map
    }
}

data class Conversation(
    val conversationId: String = "",
    val participantUids: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageAt: Timestamp? = null,
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "conversationId" to conversationId,
            "participantUids" to participantUids,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (lastMessage.isNotEmpty()) {
            map["lastMessage"] = lastMessage
            map["lastMessageSenderId"] = lastMessageSenderId
            map["lastMessageAt"] = FieldValue.serverTimestamp()
        }
        return map
    }
}

data class ConversationMessage(
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String? = null,
    val text: String = "",
    val participantUids: List<String> = emptyList(),
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "messageId" to messageId,
            "conversationId" to conversationId,
            "senderId" to senderId,
            "senderName" to senderName,
            "text" to text,
            "participantUids" to participantUids,
            "createdAt" to FieldValue.serverTimestamp()
        )
        senderPhotoUrl?.let { map["senderPhotoUrl"] = it }
        return map
    }
}
