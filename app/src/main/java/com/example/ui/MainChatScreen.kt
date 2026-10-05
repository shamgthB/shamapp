package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ConnectionRequest
import com.example.data.model.Conversation
import com.example.data.model.UserProfile
import com.example.ui.theme.AwayOrange
import com.example.ui.theme.BusyRed
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PulseBluePrimary
import com.example.ui.theme.PulseCyanAccent
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen(
    viewModel: ChatViewModel,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToDiscovery: () -> Unit = {},
    onNavigateToDirectChat: (String, String) -> Unit = { _, _ -> },
    onSignOutRequested: () -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val knownUsers by viewModel.knownUsers.collectAsStateWithLifecycle()
    val incomingRequests by viewModel.incomingRequests.collectAsStateWithLifecycle()
    val outgoingRequests by viewModel.outgoingRequests.collectAsStateWithLifecycle()
    val acceptedConnections by viewModel.acceptedConnections.collectAsStateWithLifecycle()

    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage!!)
            viewModel.dismissSuccess()
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.dismissError()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PulseCyanAccent,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = Color(0xFF003549),
                                    modifier = Modifier.size(20.dp)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (userProfile?.status) {
                                        "Online" -> OnlineGreen
                                        "Away" -> AwayOrange
                                        "Busy" -> BusyRed
                                        else -> Color.Gray
                                    },
                                    modifier = Modifier.size(7.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "@${userProfile?.username?.ifBlank { "me" } ?: "me"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PulseCyanAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Search by Username button
                    IconButton(
                        onClick = onNavigateToDiscovery,
                        modifier = Modifier.testTag("discovery_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search by Username",
                            tint = PulseCyanAccent
                        )
                    }

                    // Profile Settings button
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier.testTag("profile_button")
                    ) {
                        UserAvatar(
                            name = userProfile?.displayName ?: "Me",
                            photoUrl = userProfile?.photoUrl,
                            size = 36.dp
                        )
                    }

                    // Overflow Menu
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
                            text = { Text("Profile & Settings") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onNavigateToProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Search by Username") },
                            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onNavigateToDiscovery()
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
            ExtendedFloatingActionButton(
                onClick = onNavigateToDiscovery,
                containerColor = PulseBluePrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("New Chat") },
                modifier = Modifier.testTag("new_chat_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Tabs: Chats & Connections
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PulseCyanAccent
            ) {
                Tab(
                    selected = selectedTab == ChatTab.CHATS,
                    onClick = { viewModel.selectTab(ChatTab.CHATS) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Direct Chats (${conversations.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == ChatTab.CONNECTIONS,
                    onClick = { viewModel.selectTab(ChatTab.CONNECTIONS) },
                    text = {
                        BadgedBox(
                            badge = {
                                if (incomingRequests.isNotEmpty()) {
                                    Badge { Text("${incomingRequests.size}") }
                                }
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connections", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
            }

            // Tab Content
            when (selectedTab) {
                ChatTab.CHATS -> {
                    if (conversations.isEmpty()) {
                        EmptyChatsView(onSearchClick = onNavigateToDiscovery)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(conversations, key = { it.conversationId }) { conv ->
                                val otherUid = conv.participantUids.firstOrNull { it != viewModel.currentUserId } ?: ""
                                val otherUser = knownUsers[otherUid]
                                ConversationCard(
                                    conversation = conv,
                                    otherUser = otherUser,
                                    otherUid = otherUid,
                                    onClick = {
                                        viewModel.selectConversation(conv)
                                        onNavigateToDirectChat(conv.conversationId, otherUid)
                                    }
                                )
                            }
                        }
                    }
                }

                ChatTab.CONNECTIONS -> {
                    ConnectionsTabContent(
                        incomingRequests = incomingRequests,
                        outgoingRequests = outgoingRequests,
                        acceptedConnections = acceptedConnections,
                        knownUsers = knownUsers,
                        currentUserId = viewModel.currentUserId,
                        onAccept = { viewModel.acceptConnectionRequest(it) },
                        onDecline = { viewModel.declineConnectionRequest(it) },
                        onChat = { targetUser ->
                            viewModel.startDirectChat(targetUser)
                            val convId = listOf(viewModel.currentUserId, targetUser.userId).sorted().let { "conv_${it[0]}_${it[1]}" }
                            onNavigateToDirectChat(convId, targetUser.userId)
                        },
                        onSearchUser = onNavigateToDiscovery
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChatsView(onSearchClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = PulseCyanAccent.copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = PulseCyanAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Strictly Private Chats",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "PulseChat has no public user list. You only see chats with people you connect with.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSearchClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PulseBluePrimary)
            ) {
                Icon(Icons.Default.AlternateEmail, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Search by Username")
            }
        }
    }
}

@Composable
fun ConversationCard(
    conversation: Conversation,
    otherUser: UserProfile?,
    otherUid: String,
    onClick: () -> Unit
) {
    val displayName = otherUser?.displayName ?: "Chat with $otherUid"
    val username = otherUser?.username?.takeIf { it.isNotBlank() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("conversation_card_${conversation.conversationId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = displayName,
                photoUrl = otherUser?.photoUrl,
                size = 48.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimestamp(conversation.lastMessageAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (username != null) {
                    Text(
                        text = "@$username",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseCyanAccent
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = conversation.lastMessage.ifBlank { "Start a conversation" },
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
private fun ConnectionsTabContent(
    incomingRequests: List<ConnectionRequest>,
    outgoingRequests: List<ConnectionRequest>,
    acceptedConnections: List<ConnectionRequest>,
    knownUsers: Map<String, UserProfile>,
    currentUserId: String,
    onAccept: (ConnectionRequest) -> Unit,
    onDecline: (ConnectionRequest) -> Unit,
    onChat: (UserProfile) -> Unit,
    onSearchUser: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Incoming Requests Section
        if (incomingRequests.isNotEmpty()) {
            item {
                Text(
                    text = "Incoming Requests (${incomingRequests.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PulseCyanAccent
                )
            }
            items(incomingRequests, key = { it.requestId }) { req ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(name = req.fromUserName, photoUrl = req.fromUserPhotoUrl, size = 42.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(req.fromUserName, fontWeight = FontWeight.Bold)
                            val userObj = knownUsers[req.fromUserId]
                            if (userObj?.username?.isNotBlank() == true) {
                                Text("@${userObj.username}", style = MaterialTheme.typography.labelSmall, color = PulseCyanAccent)
                            }
                        }
                        IconButton(onClick = { onAccept(req) }) {
                            Icon(Icons.Default.Check, contentDescription = "Accept", tint = OnlineGreen)
                        }
                        IconButton(onClick = { onDecline(req) }) {
                            Icon(Icons.Default.Close, contentDescription = "Decline", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Outgoing Requests Section
        if (outgoingRequests.isNotEmpty()) {
            item {
                Text(
                    text = "Sent Requests (${outgoingRequests.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(outgoingRequests, key = { it.requestId }) { req ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(req.toUserName, fontWeight = FontWeight.SemiBold)
                            Text(req.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Connected Friends Section
        item {
            Text(
                text = "Connected Friends (${acceptedConnections.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (acceptedConnections.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No connections yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Search by username to send your first connection request.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(onClick = onSearchUser, shape = RoundedCornerShape(10.dp)) {
                            Text("Find by Username")
                        }
                    }
                }
            }
        } else {
            items(acceptedConnections, key = { it.requestId }) { conn ->
                val friendUid = if (conn.fromUserId == currentUserId) conn.toUserId else conn.fromUserId
                val friendName = if (conn.fromUserId == currentUserId) conn.toUserName else conn.fromUserName
                val friendProfile = knownUsers[friendUid]

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            name = friendProfile?.displayName ?: friendName,
                            photoUrl = friendProfile?.photoUrl ?: conn.fromUserPhotoUrl,
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(friendProfile?.displayName ?: friendName, fontWeight = FontWeight.Bold)
                            if (friendProfile?.username?.isNotBlank() == true) {
                                Text("@${friendProfile.username}", style = MaterialTheme.typography.labelSmall, color = PulseCyanAccent)
                            }
                        }
                        Button(
                            onClick = {
                                val userObj = friendProfile ?: UserProfile(userId = friendUid, displayName = friendName)
                                onChat(userObj)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PulseBluePrimary)
                        ) {
                            Text("Chat")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserAvatar(
    name: String,
    photoUrl: String? = null,
    size: Dp = 40.dp
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = PulseBluePrimary.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, PulseCyanAccent.copy(alpha = 0.5f))
    ) {
        if (!photoUrl.isNullOrBlank()) {
            if (photoUrl.startsWith("http") || photoUrl.startsWith("data:image")) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                val emoji = when (photoUrl) {
                    "cat" -> "🐱"
                    "robot" -> "🤖"
                    "astronaut" -> "👨‍🚀"
                    "wizard" -> "🧙‍♂️"
                    "artist" -> "🎨"
                    "fire" -> "🔥"
                    else -> photoUrl
                }
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = (size.value * 0.45).sp)
                }
            }
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.take(1).uppercase(),
                    color = PulseCyanAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.42).sp
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val date = timestamp.toDate()
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(date)
}
