package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Channel
import com.example.data.model.ChannelMessage
import com.example.data.model.Conversation
import com.example.data.model.ConversationMessage
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    CHANNELS,
    DIRECT_MESSAGES,
    PEOPLE
}

class ChatViewModel(
    private val repository: ChatRepository,
    val currentUserId: String
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ChatTab.CHANNELS)
    val selectedTab: StateFlow<ChatTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedChannel = MutableStateFlow<Channel?>(null)
    val selectedChannel: StateFlow<Channel?> = _selectedChannel.asStateFlow()

    private val _selectedConversation = MutableStateFlow<Conversation?>(null)
    val selectedConversation: StateFlow<Conversation?> = _selectedConversation.asStateFlow()

    private val _isCreateChannelDialogOpen = MutableStateFlow(false)
    val isCreateChannelDialogOpen: StateFlow<Boolean> = _isCreateChannelDialogOpen.asStateFlow()

    private val _isProfileDialogOpen = MutableStateFlow(false)
    val isProfileDialogOpen: StateFlow<Boolean> = _isProfileDialogOpen.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Real-time streams
    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { e ->
            Log.e(TAG, "Error observing user profile", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val channels: StateFlow<List<Channel>> = repository.observeChannels()
        .catch { e ->
            Log.e(TAG, "Error observing channels", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val conversations: StateFlow<List<Conversation>> = repository.observeConversations(currentUserId)
        .catch { e ->
            Log.e(TAG, "Error observing conversations", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allUsers: StateFlow<List<UserProfile>> = repository.observeAllUsers()
        .catch { e ->
            Log.e(TAG, "Error observing all users", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val channelMessages: StateFlow<List<ChannelMessage>> = _selectedChannel
        .flatMapLatest { channel ->
            if (channel != null) {
                repository.observeChannelMessages(channel.channelId)
            } else {
                flowOf(emptyList())
            }
        }
        .catch { e ->
            Log.e(TAG, "Error observing channel messages", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

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
        // Automatically sync initial profile & default channels
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            if (user != null) {
                val profile = UserProfile(
                    userId = user.uid,
                    displayName = user.displayName ?: "Chatter_${user.uid.take(4)}",
                    email = user.email ?: "user@chat.com",
                    photoUrl = user.photoUrl?.toString(),
                    status = "Online",
                    about = "Active on PulseChat 🚀"
                )
                repository.saveUserProfile(profile)
                repository.ensureDefaultChannelsExist()
            }
        }
    }

    fun selectTab(tab: ChatTab) {
        _selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectChannel(channel: Channel) {
        _selectedConversation.value = null
        _selectedChannel.value = channel
    }

    fun selectConversation(conversation: Conversation) {
        _selectedChannel.value = null
        _selectedConversation.value = conversation
    }

    fun clearChatSelection() {
        _selectedChannel.value = null
        _selectedConversation.value = null
    }

    fun openCreateChannelDialog() {
        _isCreateChannelDialogOpen.value = true
    }

    fun closeCreateChannelDialog() {
        _isCreateChannelDialogOpen.value = false
    }

    fun openProfileDialog() {
        _isProfileDialogOpen.value = true
    }

    fun closeProfileDialog() {
        _isProfileDialogOpen.value = false
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun createChannel(name: String, description: String) {
        if (name.isBlank()) {
            _errorMessage.value = "Channel name cannot be empty"
            return
        }
        viewModelScope.launch {
            val result = repository.createChannel(name, description)
            if (result.isSuccess) {
                closeCreateChannelDialog()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to create channel"
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
                selectConversation(conv)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to start direct chat"
            }
        }
    }

    fun sendChannelMessage(text: String) {
        val channel = _selectedChannel.value ?: return
        if (text.isBlank()) return
        val profile = userProfile.value
        val senderName = profile?.displayName?.ifBlank { "User" } ?: "User"
        val photoUrl = profile?.photoUrl

        viewModelScope.launch {
            val result = repository.sendChannelMessage(
                channelId = channel.channelId,
                senderName = senderName,
                senderPhotoUrl = photoUrl,
                text = text
            )
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to send message"
            }
        }
    }

    fun sendConversationMessage(text: String) {
        val conv = _selectedConversation.value ?: return
        if (text.isBlank()) return
        val profile = userProfile.value
        val senderName = profile?.displayName?.ifBlank { "User" } ?: "User"

        viewModelScope.launch {
            val result = repository.sendConversationMessage(
                conversationId = conv.conversationId,
                senderName = senderName,
                text = text,
                participantUids = conv.participantUids
            )
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to send message"
            }
        }
    }

    fun updateStatus(status: String, about: String) {
        val current = userProfile.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                status = status,
                about = about
            )
            val result = repository.saveUserProfile(updated)
            if (result.isSuccess) {
                closeProfileDialog()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update profile"
            }
        }
    }
}
