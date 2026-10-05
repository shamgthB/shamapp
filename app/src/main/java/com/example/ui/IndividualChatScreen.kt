package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Conversation
import com.example.data.model.ConversationMessage
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PulseBluePrimary
import com.example.ui.theme.PulseCyanAccent
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualDirectChatScreen(
    conversationId: String,
    otherUserId: String,
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val knownUsers by viewModel.knownUsers.collectAsStateWithLifecycle()
    val otherUser = knownUsers[otherUserId]

    LaunchedEffect(conversationId) {
        val conv = Conversation(
            conversationId = conversationId,
            participantUids = listOf(viewModel.currentUserId, otherUserId).sorted()
        )
        viewModel.selectConversation(conv)
    }

    val messages by viewModel.conversationMessages.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val titleName = otherUser?.displayName ?: "Direct Chat"
    val usernameHandle = otherUser?.username?.takeIf { it.isNotBlank() }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(name = titleName, photoUrl = otherUser?.photoUrl, size = 38.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = titleName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (usernameHandle != null) {
                                    Text(
                                        text = "@$usernameHandle",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PulseCyanAccent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = OnlineGreen,
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = otherUser?.status ?: "Online",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("direct_chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Private Chat",
                        tint = PulseCyanAccent,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(20.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            IndividualChatInputBar(
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PulseCyanAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = PulseCyanAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Private Chat with $titleName",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Messages are completely private and visible only to the two of you.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                        IndividualMessageBubble(
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

// Individual Message Bubble
@Composable
private fun IndividualMessageBubble(
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
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMessageTime(timestamp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = textColor.copy(alpha = 0.7f)
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Delivered",
                            tint = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// Input Bar with Quick Replies
@Composable
private fun IndividualChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val quickReplies = listOf("Hey! 👋", "Got it! 👍", "Sounds good! ✨", "Call me? 📞", "Thanks! 🙏")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Quick Reply Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickReplies) { chip ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { onTextChange(chip) }
                ) {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Input Field & Send Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Write a message...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PulseCyanAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = if (text.isNotBlank()) PulseBluePrimary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(enabled = text.isNotBlank(), onClick = onSend)
                    .testTag("chat_send_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (text.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatMessageTime(timestamp: Timestamp?): String {
    if (timestamp == null) return "Just now"
    val date = timestamp.toDate()
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(date)
}
