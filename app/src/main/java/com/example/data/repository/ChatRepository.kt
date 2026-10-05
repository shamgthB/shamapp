package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ConnectionRequest
import com.example.data.model.Conversation
import com.example.data.model.ConversationMessage
import com.example.data.model.UserProfile
import com.example.data.model.UsernameRegistration
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.storage.FirebaseStorage
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    // --- USERNAME REGISTRATION & AVAILABILITY ---

    suspend fun checkUsernameAvailable(rawUsername: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val clean = rawUsername.trim().lowercase().removePrefix("@")
        if (!clean.matches(Regex("^[a-z0-9_]{3,30}$"))) {
            return@withContext Result.success(false)
        }
        try {
            val doc = db.collection("usernames").document(clean).get().await()
            Result.success(!doc.exists())
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "usernames/$clean")
            Result.failure(e)
        }
    }

    suspend fun claimUsername(rawUsername: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val clean = rawUsername.trim().lowercase().removePrefix("@")
        if (!clean.matches(Regex("^[a-z0-9_]{3,30}$"))) {
            return@withContext Result.failure(
                IllegalArgumentException("Username must be 3-30 characters (letters, numbers, underscores only)")
            )
        }
        try {
            val batch = db.batch()
            val usernameRef = db.collection("usernames").document(clean)
            val userRef = db.collection("users").document(userId)

            batch.set(usernameRef, UsernameRegistration(username = clean, userId = userId).toMap())
            batch.set(
                userRef,
                mapOf(
                    "username" to clean,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "usernames/$clean")
            Result.failure(e)
        }
    }

    suspend fun searchUserByUsername(rawUsername: String): Result<UserProfile?> = withContext(Dispatchers.IO) {
        val clean = rawUsername.trim().lowercase().removePrefix("@")
        if (!clean.matches(Regex("^[a-z0-9_]{3,30}$"))) {
            return@withContext Result.success(null)
        }
        try {
            val usernameDoc = db.collection("usernames").document(clean).get().await()
            if (!usernameDoc.exists()) {
                return@withContext Result.success(null)
            }
            val targetUid = usernameDoc.getString("userId") ?: return@withContext Result.success(null)
            val userDoc = db.collection("users").document(targetUid).get().await()
            val profile = userDoc.toObject(UserProfile::class.java)
            Result.success(profile)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "usernames/$clean")
            Result.failure(e)
        }
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

    suspend fun getUserProfile(userId: String): Result<UserProfile?> = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection("users").document(userId).get().await()
            Result.success(doc.toObject(UserProfile::class.java))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$userId")
            Result.failure(e)
        }
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

    suspend fun uploadProfilePicture(userId: String, imageBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val storageRef = FirebaseStorage.getInstance().reference.child("profile_pictures/$userId.jpg")
            storageRef.putBytes(imageBytes).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.w("ChatRepository", "Firebase Storage upload failed: ${e.message}")
            Result.failure(e)
        }
    }

    // --- CONNECTION REQUESTS (PRIVACY-COMPLIANT) ---

    fun observeIncomingRequests(userId: String): Flow<List<ConnectionRequest>> = flow {
        val path = "connection_requests"
        emitAll(
            db.collection("connection_requests")
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", "PENDING")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ConnectionRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeOutgoingRequests(userId: String): Flow<List<ConnectionRequest>> = flow {
        val path = "connection_requests"
        emitAll(
            db.collection("connection_requests")
                .whereEqualTo("fromUserId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ConnectionRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAcceptedConnections(userId: String): Flow<List<ConnectionRequest>> {
        val incomingAccepted = db.collection("connection_requests")
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", "ACCEPTED")
            .snapshots()
            .map { it.toObjects(ConnectionRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

        val outgoingAccepted = db.collection("connection_requests")
            .whereEqualTo("fromUserId", userId)
            .whereEqualTo("status", "ACCEPTED")
            .snapshots()
            .map { it.toObjects(ConnectionRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

        return incomingAccepted.combine(outgoingAccepted) { inc, out ->
            (inc + out).distinctBy { it.requestId }
        }.catch { error ->
            if (error is Exception) handleFirestoreError(error, OperationType.LIST, "connection_requests")
            throw error
        }
    }

    suspend fun sendConnectionRequest(
        toUserId: String,
        toUserName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val requestId = "req_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val path = "connection_requests/$requestId"
        try {
            val uid = requireUserId()
            val myProfile = db.collection("users").document(uid).get().await()
                .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val myName = myProfile?.displayName ?: "PulseChat User"
            val myPhoto = myProfile?.photoUrl

            val request = ConnectionRequest(
                requestId = requestId,
                fromUserId = uid,
                fromUserName = myName,
                fromUserPhotoUrl = myPhoto,
                toUserId = toUserId,
                toUserName = toUserName,
                status = "PENDING"
            )
            db.collection("connection_requests").document(requestId).set(request.toCreateMap()).await()
            Result.success(requestId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun acceptConnectionRequest(requestId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "connection_requests/$requestId"
        try {
            requireUserId()
            db.collection("connection_requests").document(requestId).update(
                mapOf(
                    "status" to "ACCEPTED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun declineConnectionRequest(requestId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "connection_requests/$requestId"
        try {
            requireUserId()
            db.collection("connection_requests").document(requestId).update(
                mapOf(
                    "status" to "DECLINED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- DIRECT 1-ON-1 CONVERSATIONS (TOTAL CHAT PRIVACY) ---

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
        senderPhotoUrl: String? = null,
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
                senderPhotoUrl = senderPhotoUrl,
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
