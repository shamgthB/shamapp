package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class UserProfile(
    val userId: String = "",
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
        photoUrl?.let { map["photoUrl"] = it }
        about?.let { map["about"] = it }
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
    val text: String = "",
    val participantUids: List<String> = emptyList(),
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "messageId" to messageId,
            "conversationId" to conversationId,
            "senderId" to senderId,
            "senderName" to senderName,
            "text" to text,
            "participantUids" to participantUids,
            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}
