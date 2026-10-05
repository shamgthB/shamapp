package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConnectionRequest
import com.example.data.model.Conversation
import com.example.data.model.ConversationMessage
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "ChatViewModel"

enum class ChatTab {
    CHATS,
    CONNECTIONS
}

class ChatViewModel(
    val repository: ChatRepository,
    val currentUserId: String
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ChatTab.CHATS)
    val selectedTab: StateFlow<ChatTab> = _selectedTab.asStateFlow()

    private val _selectedConversation = MutableStateFlow<Conversation?>(null)
    val selectedConversation: StateFlow<Conversation?> = _selectedConversation.asStateFlow()

    private val _selectedUserProfile = MutableStateFlow<UserProfile?>(null)
    val selectedUserProfile: StateFlow<UserProfile?> = _selectedUserProfile.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Cache of user profiles for participants in conversations & connections (Strict Privacy)
    private val _knownUsers = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val knownUsers: StateFlow<Map<String, UserProfile>> = _knownUsers.asStateFlow()

    // Username search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchingUser = MutableStateFlow(false)
    val isSearchingUser: StateFlow<Boolean> = _isSearchingUser.asStateFlow()

    private val _searchResultUser = MutableStateFlow<UserProfile?>(null)
    val searchResultUser: StateFlow<UserProfile?> = _searchResultUser.asStateFlow()

    private val _searchNotice = MutableStateFlow<String?>(null)
    val searchNotice: StateFlow<String?> = _searchNotice.asStateFlow()

    // In-app username setup state (for Google Sign-In or profile setup)
    private val _isCheckingUsername = MutableStateFlow(false)
    val isCheckingUsername: StateFlow<Boolean> = _isCheckingUsername.asStateFlow()

    private val _isUsernameAvailable = MutableStateFlow<Boolean?>(null)
    val isUsernameAvailable: StateFlow<Boolean?> = _isUsernameAvailable.asStateFlow()

    private val _usernameCheckMessage = MutableStateFlow<String?>(null)
    val usernameCheckMessage: StateFlow<String?> = _usernameCheckMessage.asStateFlow()

    // Real-time user profile stream
    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { e ->
            Log.e(TAG, "Error observing user profile", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    // Real-time conversations stream (strictly only conversations where current user is a participant)
    val conversations: StateFlow<List<Conversation>> = repository.observeConversations(currentUserId)
        .catch { e ->
            Log.e(TAG, "Error observing conversations", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Real-time connection requests streams
    val incomingRequests: StateFlow<List<ConnectionRequest>> = repository.observeIncomingRequests(currentUserId)
        .catch { e ->
            Log.e(TAG, "Error observing incoming requests", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val outgoingRequests: StateFlow<List<ConnectionRequest>> = repository.observeOutgoingRequests(currentUserId)
        .catch { e ->
            Log.e(TAG, "Error observing outgoing requests", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val acceptedConnections: StateFlow<List<ConnectionRequest>> = repository.observeAcceptedConnections(currentUserId)
        .catch { e ->
            Log.e(TAG, "Error observing connections", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Real-time messages for active conversation
    @OptIn(ExperimentalCoroutinesApi::class)
    val conversationMessages: StateFlow<List<ConversationMessage>> = _selectedConversation
        .flatMapLatest { conv ->
            if (conv != null) {
                repository.observeConversationMessages(conv.conversationId)
            } else {
                flowOf(emptyList())
            }
        }
        .catch { e ->
            Log.e(TAG, "Error observing conversation messages", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        // Ensure user profile document exists
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            if (user != null) {
                val existing = repository.getUserProfile(user.uid).getOrNull()
                if (existing == null) {
                    val initial = UserProfile(
                        userId = user.uid,
                        username = "",
                        displayName = user.displayName ?: "User_${user.uid.take(4)}",
                        email = user.email ?: "user@pulsechat.io",
                        photoUrl = user.photoUrl?.toString(),
                        status = "Online",
                        about = "Hey there! I am using PulseChat."
                    )
                    repository.saveUserProfile(initial)
                }
            }
        }

        // On-demand fetch user profiles for active conversations
        viewModelScope.launch {
            conversations.collect { convList ->
                val neededUids = convList.flatMap { it.participantUids }
                    .filter { it != currentUserId && !_knownUsers.value.containsKey(it) }
                    .distinct()

                neededUids.forEach { uid ->
                    fetchUserProfile(uid)
                }
            }
        }

        // On-demand fetch user profiles for connection requests
        viewModelScope.launch {
            incomingRequests.collect { reqList ->
                reqList.map { it.fromUserId }
                    .filter { it != currentUserId && !_knownUsers.value.containsKey(it) }
                    .distinct()
                    .forEach { fetchUserProfile(it) }
            }
        }

        viewModelScope.launch {
            outgoingRequests.collect { reqList ->
                reqList.map { it.toUserId }
                    .filter { it != currentUserId && !_knownUsers.value.containsKey(it) }
                    .distinct()
                    .forEach { fetchUserProfile(it) }
            }
        }
    }

    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            val result = repository.getUserProfile(uid)
            if (result.isSuccess && result.getOrNull() != null) {
                _knownUsers.value = _knownUsers.value + (uid to result.getOrThrow()!!)
            }
        }
    }

    fun selectTab(tab: ChatTab) {
        _selectedTab.value = tab
    }

    fun selectConversation(conversation: Conversation) {
        _selectedConversation.value = conversation
    }

    fun clearChatSelection() {
        _selectedConversation.value = null
    }

    fun viewUserProfileDetail(user: UserProfile) {
        _selectedUserProfile.value = user
    }

    fun clearUserProfileDetail() {
        _selectedUserProfile.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissSuccess() {
        _successMessage.value = null
    }

    // --- REAL-TIME USERNAME SEARCH (STRICT PRIVACY) ---

    private var searchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        val clean = query.trim().lowercase().removePrefix("@")
        if (clean.isBlank()) {
            _searchResultUser.value = null
            _searchNotice.value = null
            _isSearchingUser.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // debounce typing
            executeUsernameSearch(clean)
        }
    }

    fun executeUsernameSearch(cleanQuery: String = _searchQuery.value.trim().lowercase().removePrefix("@")) {
        if (cleanQuery.isBlank()) return
        _isSearchingUser.value = true
        _searchNotice.value = null
        _searchResultUser.value = null

        viewModelScope.launch {
            val result = repository.searchUserByUsername(cleanQuery)
            _isSearchingUser.value = false
            if (result.isSuccess) {
                val found = result.getOrNull()
                if (found != null) {
                    if (found.userId == currentUserId) {
                        _searchNotice.value = "That is your own username! Search for another member."
                        _searchResultUser.value = null
                    } else {
                        _searchResultUser.value = found
                        _searchNotice.value = null
                        // add to known cache
                        _knownUsers.value = _knownUsers.value + (found.userId to found)
                    }
                } else {
                    _searchResultUser.value = null
                    _searchNotice.value = "No user found with username '@$cleanQuery'"
                }
            } else {
                _searchNotice.value = "Search error: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    // --- USERNAME AVAILABILITY CHECK & CLAIM ---

    private var usernameCheckJob: Job? = null

    fun checkUsername(rawUsername: String) {
        val clean = rawUsername.trim().lowercase().removePrefix("@")
        usernameCheckJob?.cancel()

        if (clean.length < 3) {
            _isCheckingUsername.value = false
            _isUsernameAvailable.value = null
            _usernameCheckMessage.value = "Username must be at least 3 characters"
            return
        }

        if (!clean.matches(Regex("^[a-z0-9_]{3,30}$"))) {
            _isCheckingUsername.value = false
            _isUsernameAvailable.value = false
            _usernameCheckMessage.value = "Only letters, numbers, and underscores allowed"
            return
        }

        _isCheckingUsername.value = true
        _usernameCheckMessage.value = null

        usernameCheckJob = viewModelScope.launch {
            delay(350)
            val result = repository.checkUsernameAvailable(clean)
            _isCheckingUsername.value = false
            if (result.isSuccess) {
                val available = result.getOrThrow()
                _isUsernameAvailable.value = available
                _usernameCheckMessage.value = if (available) {
                    "Username '@$clean' is available!"
                } else {
                    "Username '@$clean' is already taken"
                }
            } else {
                _isUsernameAvailable.value = null
                _usernameCheckMessage.value = "Could not verify username availability"
            }
        }
    }

    fun claimUsername(rawUsername: String, onComplete: (Boolean) -> Unit) {
        val clean = rawUsername.trim().lowercase().removePrefix("@")
        viewModelScope.launch {
            val result = repository.claimUsername(clean, currentUserId)
            if (result.isSuccess) {
                _successMessage.value = "Username @$clean claimed successfully!"
                onComplete(true)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to claim username"
                onComplete(false)
            }
        }
    }

    // --- MESSAGING & CONNECTIONS ---

    fun sendConversationMessage(text: String) {
        val conv = _selectedConversation.value ?: return
        if (text.isBlank()) return

        val profile = userProfile.value
        val myName = profile?.displayName ?: "Me"
        val myPhoto = profile?.photoUrl

        viewModelScope.launch {
            val result = repository.sendConversationMessage(
                conversationId = conv.conversationId,
                senderName = myName,
                senderPhotoUrl = myPhoto,
                text = text,
                participantUids = conv.participantUids
            )
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to send message"
            }
        }
    }

    fun startDirectChat(otherUser: UserProfile) {
        viewModelScope.launch {
            val result = repository.getOrCreateConversation(otherUser.userId)
            if (result.isSuccess) {
                val convId = result.getOrThrow()
                val conv = Conversation(
                    conversationId = convId,
                    participantUids = listOf(currentUserId, otherUser.userId).sorted()
                )
                clearUserProfileDetail()
                selectConversation(conv)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to start direct chat"
            }
        }
    }

    fun sendConnectionRequest(targetUser: UserProfile) {
        viewModelScope.launch {
            val result = repository.sendConnectionRequest(targetUser.userId, targetUser.displayName)
            if (result.isSuccess) {
                _successMessage.value = "Connection request sent to @${targetUser.username.ifBlank { targetUser.displayName }}!"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to send request"
            }
        }
    }

    fun acceptConnectionRequest(request: ConnectionRequest) {
        viewModelScope.launch {
            val result = repository.acceptConnectionRequest(request.requestId)
            if (result.isSuccess) {
                _successMessage.value = "Connected with ${request.fromUserName}!"
                // Automatically create conversation
                repository.getOrCreateConversation(request.fromUserId)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to accept request"
            }
        }
    }

    fun declineConnectionRequest(request: ConnectionRequest) {
        viewModelScope.launch {
            val result = repository.declineConnectionRequest(request.requestId)
            if (result.isSuccess) {
                _successMessage.value = "Request declined"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to decline request"
            }
        }
    }

    // --- PROFILE SETTINGS ---

    fun uploadProfileImage(imageBytes: ByteArray, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.uploadProfilePicture(currentUserId, imageBytes)
            if (result.isSuccess) {
                onComplete(result.getOrNull())
            } else {
                onComplete(null)
            }
        }
    }

    fun updateProfileDetails(name: String, bio: String, status: String, avatarUrl: String?) {
        val current = userProfile.value ?: return
        if (name.isBlank()) {
            _errorMessage.value = "Display name cannot be empty"
            return
        }
        viewModelScope.launch {
            val updated = current.copy(
                displayName = name.trim(),
                about = bio.trim(),
                status = status,
                photoUrl = avatarUrl
            )
            val result = repository.saveUserProfile(updated)
            if (result.isSuccess) {
                _successMessage.value = "Profile updated successfully!"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update profile"
            }
        }
    }
}
