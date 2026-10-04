package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Channel
import com.example.data.model.ChannelMessage
import com.example.data.model.Conversation
import com.example.data.model.ConversationMessage
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- USER PROFILES ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection("users")
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = requireUserId()
            val path = "users/$uid"
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(profile.copy(userId = uid).toCreateMap()).await()
            } else {
                docRef.update(profile.toUpdateMap()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users")
            Result.failure(e)
        }
    }

    // --- CHANNELS ---

    fun observeChannels(): Flow<List<Channel>> = flow {
        val path = "channels"
        emitAll(
            db.collection("channels")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Channel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createChannel(name: String, description: String): Result<String> = withContext(Dispatchers.IO) {
        val channelId = "ch_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "channels/$channelId"
        try {
            val uid = requireUserId()
            val channel = Channel(
                channelId = channelId,
                name = name.trim(),
                description = description.trim(),
                createdBy = uid,
                memberCount = 1
            )
            db.collection("channels").document(channelId).set(channel.toCreateMap()).await()
            Result.success(channelId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun ensureDefaultChannelsExist(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = requireUserId()
            val defaults = listOf(
                Pair("general", "General conversations and team pulse"),
                Pair("tech-talk", "Development, Kotlin, Compose, architecture"),
                Pair("casual-lounge", "Hangout, memes, coffee breaks, life"),
                Pair("announcements", "Latest news, updates and milestones")
            )
            for ((slug, desc) in defaults) {
                val docRef = db.collection("channels").document(slug)
                val doc = docRef.get().await()
                if (!doc.exists()) {
                    val channel = Channel(
                        channelId = slug,
                        name = slug.replace("-", " ").replaceFirstChar { it.uppercase() },
                        description = desc,
                        createdBy = uid,
                        memberCount = 1
                    )
                    docRef.set(channel.toCreateMap()).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "channels")
            Result.failure(e)
        }
    }

    fun observeChannelMessages(channelId: String): Flow<List<ChannelMessage>> = flow {
        val path = "channels/$channelId/messages"
        emitAll(
            db.collection("channels").document(channelId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ChannelMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun sendChannelMessage(
        channelId: String,
        senderName: String,
        senderPhotoUrl: String?,
        text: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val path = "channels/$channelId/messages/$messageId"
        try {
            val uid = requireUserId()
            val message = ChannelMessage(
                messageId = messageId,
                channelId = channelId,
                senderId = uid,
                senderName = senderName,
                senderPhotoUrl = senderPhotoUrl,
                text = text.trim()
            )
            db.collection("channels").document(channelId).collection("messages")
                .document(messageId)
                .set(message.toCreateMap())
                .await()
            Result.success(messageId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- DIRECT CONVERSATIONS ---

    fun observeConversations(userId: String): Flow<List<Conversation>> = flow {
        val path = "conversations"
        emitAll(
            db.collection("conversations")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getOrCreateConversation(otherUserId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val myUid = requireUserId()
            val sortedUids = listOf(myUid, otherUserId).sorted()
            val conversationId = "conv_${sortedUids[0]}_${sortedUids[1]}"
            val docRef = db.collection("conversations").document(conversationId)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                val conv = Conversation(
                    conversationId = conversationId,
                    participantUids = sortedUids,
                    lastMessage = "Started a conversation",
                    lastMessageSenderId = myUid
                )
                docRef.set(conv.toCreateMap()).await()
            }
            Result.success(conversationId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "conversations")
            Result.failure(e)
        }
    }

    fun observeConversationMessages(conversationId: String): Flow<List<ConversationMessage>> = flow {
        val path = "conversations/$conversationId/messages"
        emitAll(
            db.collection("conversations").document(conversationId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ConversationMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun sendConversationMessage(
        conversationId: String,
        senderName: String,
        text: String,
        participantUids: List<String>
    ): Result<String> = withContext(Dispatchers.IO) {
        val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val path = "conversations/$conversationId/messages/$messageId"
        try {
            val uid = requireUserId()
            val message = ConversationMessage(
                messageId = messageId,
                conversationId = conversationId,
                senderId = uid,
                senderName = senderName,
                text = text.trim(),
                participantUids = participantUids
            )
            val batch = db.batch()
            val msgRef = db.collection("conversations").document(conversationId).collection("messages").document(messageId)
            batch.set(msgRef, message.toCreateMap())

            val convRef = db.collection("conversations").document(conversationId)
            batch.update(
                convRef,
                mapOf(
                    "lastMessage" to text.trim().take(100),
                    "lastMessageSenderId" to uid,
                    "lastMessageAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(messageId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
