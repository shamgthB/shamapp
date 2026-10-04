package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Channel
import com.example.data.model.ChannelMessage
import com.example.data.model.Conversation
import com.example.data.model.ConversationMessage
import com.example.data.model.UserProfile
import com.example.ui.theme.AwayOrange
import com.example.ui.theme.BusyRed
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PulseBluePrimary
import com.example.ui.theme.PulseCyanAccent
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen(
    viewModel: ChatViewModel,
    onSignOutRequested: () -> Unit
) {
    val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
    val selectedConversation by viewModel.selectedConversation.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isCreateChannelOpen by viewModel.isCreateChannelDialogOpen.collectAsStateWithLifecycle()
    val isProfileOpen by viewModel.isProfileDialogOpen.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }

    // If a channel or conversation is active, render the Chat Detail View
    if (selectedChannel != null) {
        BackHandler { viewModel.clearChatSelection() }
        ChannelChatDetail(
            channel = selectedChannel!!,
            viewModel = viewModel,
            onBack = { viewModel.clearChatSelection() }
        )
        return
    }

    if (selectedConversation != null) {
        BackHandler { viewModel.clearChatSelection() }
        val otherUid = selectedConversation!!.participantUids.firstOrNull { it != viewModel.currentUserId } ?: ""
        val otherUser = allUsers.firstOrNull { it.userId == otherUid }
        DirectChatDetail(
            conversation = selectedConversation!!,
            otherUser = otherUser,
            viewModel = viewModel,
            onBack = { viewModel.clearChatSelection() }
        )
        return
    }

    // Dashboard View (Tabs: Channels, DMs, People)
    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PulseCyanAccent,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = Color(0xFF003549),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PulseChat",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = userProfile?.displayName ?: "Online",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnlineGreen
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openProfileDialog() },
                        modifier = Modifier.testTag("profile_button")
                    ) {
                        UserAvatar(
                            name = userProfile?.displayName ?: "Me",
                            photoUrl = userProfile?.photoUrl,
                            size = 36.dp
                        )
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Profile & Status") },
                            onClick = {
                                showMenu = false
                                viewModel.openProfileDialog()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sign Out") },
                            onClick = {
                                showMenu = false
                                onSignOutRequested()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == ChatTab.CHANNELS) {
                FloatingActionButton(
                    onClick = { viewModel.openCreateChannelDialog() },
                    containerColor = PulseBluePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("create_channel_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Channel"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_input"),
                placeholder = { Text("Search channels, chats or people...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PulseCyanAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // Tabs Header
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PulseBluePrimary
            ) {
                Tab(
                    selected = selectedTab == ChatTab.CHANNELS,
                    onClick = { viewModel.selectTab(ChatTab.CHANNELS) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Channels")
                        }
                    },
                    modifier = Modifier.testTag("tab_channels")
                )
                Tab(
                    selected = selectedTab == ChatTab.DIRECT_MESSAGES,
                    onClick = { viewModel.selectTab(ChatTab.DIRECT_MESSAGES) },
                    text = {
                        BadgedBox(badge = {
                            if (conversations.isNotEmpty()) {
                                Badge(containerColor = PulseCyanAccent) {
                                    Text("${conversations.size}")
                                }
                            }
                        }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Direct")
                            }
                        }
                    },
                    modifier = Modifier.testTag("tab_direct")
                )
                Tab(
                    selected = selectedTab == ChatTab.PEOPLE,
                    onClick = { viewModel.selectTab(ChatTab.PEOPLE) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("People")
                        }
                    },
                    modifier = Modifier.testTag("tab_people")
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    ChatTab.CHANNELS -> {
                        val filteredChannels = channels.filter {
                            searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.description.contains(searchQuery, ignoreCase = true)
                        }
                        if (filteredChannels.isEmpty()) {
                            EmptyStateNotice(
                                title = "No Channels Found",
                                description = "Create your own channel with the + button below!"
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredChannels, key = { it.channelId }) { ch ->
                                    ChannelCard(
                                        channel = ch,
                                        onClick = { viewModel.selectChannel(ch) }
                                    )
                                }
                            }
                        }
                    }

                    ChatTab.DIRECT_MESSAGES -> {
                        val filteredConvs = conversations.filter {
                            if (searchQuery.isBlank()) true else {
                                it.lastMessage.contains(searchQuery, ignoreCase = true)
                            }
                        }
                        if (filteredConvs.isEmpty()) {
                            EmptyStateNotice(
                                title = "No Direct Chats Yet",
                                description = "Browse the People tab to find a member and start a conversation!"
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredConvs, key = { it.conversationId }) { conv ->
                                    val otherUid = conv.participantUids.firstOrNull { it != viewModel.currentUserId } ?: ""
                                    val otherUser = allUsers.firstOrNull { it.userId == otherUid }
                                    ConversationCard(
                                        conversation = conv,
                                        otherUser = otherUser,
                                        onClick = { viewModel.selectConversation(conv) }
                                    )
                                }
                            }
                        }
                    }

                    ChatTab.PEOPLE -> {
                        val otherUsers = allUsers.filter { it.userId != viewModel.currentUserId && (
                                searchQuery.isBlank() || it.displayName.contains(searchQuery, ignoreCase = true) ||
                                        (it.about ?: "").contains(searchQuery, ignoreCase = true)
                                )}
                        if (otherUsers.isEmpty()) {
                            EmptyStateNotice(
                                title = "No Community Members Found",
                                description = "Invite friends to sign in to start chatting!"
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(otherUsers, key = { it.userId }) { user ->
                                    UserContactCard(
                                        user = user,
                                        onMessageClick = { viewModel.startDirectChat(user) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Channel Dialog
    if (isCreateChannelOpen) {
        CreateChannelDialog(
            onDismiss = { viewModel.closeCreateChannelDialog() },
            onCreate = { name, desc -> viewModel.createChannel(name, desc) }
        )
    }

    // Profile & Status Dialog
    if (isProfileOpen && userProfile != null) {
        ProfileStatusDialog(
            profile = userProfile!!,
            onDismiss = { viewModel.closeProfileDialog() },
            onSave = { status, about -> viewModel.updateStatus(status, about) },
            onSignOut = {
                viewModel.closeProfileDialog()
                onSignOutRequested()
            }
        )
    }

    // Error Alert
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Error") },
            text = { Text(errorMessage ?: "An unexpected error occurred.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("OK")
                }
            }
        )
    }
}

// --- SUB-COMPONENTS ---

@Composable
private fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("channel_item_${channel.channelId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = PulseCyanAccent.copy(alpha = 0.2f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = PulseCyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${channel.memberCount} members",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = channel.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: Conversation,
    otherUser: UserProfile?,
    onClick: () -> Unit
) {
    val name = otherUser?.displayName?.ifBlank { "Member" } ?: "Member"
    val status = otherUser?.status ?: "Online"
    val statusColor = when (status) {
        "Online" -> OnlineGreen
        "Away" -> AwayOrange
        "Busy" -> BusyRed
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("conversation_item_${conversation.conversationId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                UserAvatar(name = name, photoUrl = otherUser?.photoUrl, size = 48.dp)
                Surface(
                    shape = CircleShape,
                    color = statusColor,
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                ) {}
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatTimestamp(conversation.lastMessageAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = conversation.lastMessage.ifBlank { "Started conversation" },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UserContactCard(
    user: UserProfile,
    onMessageClick: () -> Unit
) {
    val statusColor = when (user.status) {
        "Online" -> OnlineGreen
        "Away" -> AwayOrange
        "Busy" -> BusyRed
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                UserAvatar(name = user.displayName, photoUrl = user.photoUrl, size = 46.dp)
                Surface(
                    shape = CircleShape,
                    color = statusColor,
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                ) {}
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = user.about ?: user.status,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onMessageClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PulseCyanAccent.copy(alpha = 0.15f),
                    contentColor = PulseCyanAccent
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("chat_user_${user.userId}")
            ) {
                Text(text = "Chat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// --- CHAT CONVERSATION DETAIL SCREENS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelChatDetail(
    channel: Channel,
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.channelMessages.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "# ${channel.name}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = channel.description,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ChatBottomInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    viewModel.sendChannelMessage(inputText)
                    inputText = ""
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Welcome to #${channel.name}!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "This is the start of this channel. Say hello! 👋",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.messageId }) { msg ->
                        val isMe = msg.senderId == viewModel.currentUserId
                        MessageBubble(
                            isMe = isMe,
                            senderName = msg.senderName,
                            text = msg.text,
                            timestamp = msg.createdAt
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectChatDetail(
    conversation: Conversation,
    otherUser: UserProfile?,
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.conversationMessages.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val titleName = otherUser?.displayName ?: "Direct Chat"

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(name = titleName, photoUrl = otherUser?.photoUrl, size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = titleName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = otherUser?.status ?: "Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnlineGreen
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ChatBottomInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    viewModel.sendConversationMessage(inputText)
                    inputText = ""
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Chatting with $titleName",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Direct messages are real-time & private.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.messageId }) { msg ->
                        val isMe = msg.senderId == viewModel.currentUserId
                        MessageBubble(
                            isMe = isMe,
                            senderName = msg.senderName,
                            text = msg.text,
                            timestamp = msg.createdAt
                        )
                    }
                }
            }
        }
    }
}

// Message Bubble
@Composable
private fun MessageBubble(
    isMe: Boolean,
    senderName: String,
    text: String,
    timestamp: Timestamp?
) {
    val bubbleColor = if (isMe) PulseBluePrimary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val alignment = if (isMe) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = alignment
    ) {
        if (!isMe) {
            Text(
                text = senderName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = PulseCyanAccent,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            ),
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(timestamp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = textColor.copy(alpha = 0.7f)
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Delivered",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// Chat Bottom Input Bar with quick reply chips & emojis
@Composable
private fun ChatBottomInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val quickReplies = listOf("Sounds good! 👍", "On my way! 🚀", "Let's do it! 🔥", "Haha 😂", "Thanks! 🙏")
    val emojis = listOf("👍", "❤️", "🔥", "🚀", "🎉", "👏")

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Quick reply chips row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(quickReplies) { reply ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { onTextChange(reply) }
                    ) {
                        Text(
                            text = reply,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    placeholder = { Text("Type a message...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PulseCyanAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (text.isNotBlank()) PulseBluePrimary else Color.Gray.copy(alpha = 0.3f))
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// User Avatar helper
@Composable
fun UserAvatar(
    name: String,
    photoUrl: String?,
    size: androidx.compose.ui.unit.Dp
) {
    val initial = name.firstOrNull()?.uppercase() ?: "U"
    Surface(
        shape = CircleShape,
        color = PulseBluePrimary,
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initial,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
}

// Dialog: Create Channel
@Composable
private fun CreateChannelDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Channel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Channel Name") },
                    placeholder = { Text("e.g. android-devs") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("channel_name_input")
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    placeholder = { Text("What is this channel about?") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, desc) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("create_channel_confirm_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Dialog: Profile & Presence Status
@Composable
private fun ProfileStatusDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onSignOut: () -> Unit
) {
    val statuses = listOf("Online", "Away", "Busy", "Offline")
    var selectedStatus by remember { mutableStateOf(profile.status) }
    var aboutText by remember { mutableStateOf(profile.about ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(name = profile.displayName, photoUrl = profile.photoUrl, size = 42.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = profile.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = profile.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Status & Presence", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    statuses.forEach { st ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedStatus == st,
                                onClick = { selectedStatus = st }
                            )
                            Text(text = st, fontSize = 12.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = aboutText,
                    onValueChange = { aboutText = it },
                    label = { Text("Custom Status / About") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onSave(selectedStatus, aboutText) }) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun EmptyStateNotice(
    title: String,
    description: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = PulseCyanAccent.copy(alpha = 0.15f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = PulseCyanAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val date = timestamp.toDate()
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(date)
}
