package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserProfile
import com.example.ui.theme.AwayOrange
import com.example.ui.theme.BusyRed
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PulseBluePrimary
import com.example.ui.theme.PulseCyanAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDiscoveryScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDirectChat: (String, String) -> Unit
) {
    BackHandler { onNavigateBack() }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearchingUser.collectAsStateWithLifecycle()
    val searchResult by viewModel.searchResultUser.collectAsStateWithLifecycle()
    val searchNotice by viewModel.searchNotice.collectAsStateWithLifecycle()

    val acceptedConnections by viewModel.acceptedConnections.collectAsStateWithLifecycle()
    val outgoingRequests by viewModel.outgoingRequests.collectAsStateWithLifecycle()
    val incomingRequests by viewModel.incomingRequests.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = PulseCyanAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Find by Username",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("discovery_back_button")
                    ) {
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Username Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("discovery_search_input"),
                placeholder = { Text("Search by exact username (e.g. @alex)...") },
                prefix = { Text("@", color = PulseCyanAccent, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.AlternateEmail, contentDescription = "Search", tint = PulseCyanAccent)
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
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

            Spacer(modifier = Modifier.height(20.dp))

            // Content Area
            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PulseCyanAccent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Looking up username in database...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (searchResult != null) {
                // User Found! Display Card
                val user = searchResult!!
                val isConnected = acceptedConnections.any {
                    it.fromUserId == user.userId || it.toUserId == user.userId
                }
                val isOutgoingPending = outgoingRequests.any {
                    it.toUserId == user.userId && it.status == "PENDING"
                }
                val isIncomingPending = incomingRequests.any {
                    it.fromUserId == user.userId && it.status == "PENDING"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        UserAvatar(
                            name = user.displayName,
                            photoUrl = user.photoUrl,
                            size = 72.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Username Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PulseCyanAccent.copy(alpha = 0.15f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "@${user.username}",
                                color = PulseCyanAccent,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (!user.about.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = user.about,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Status Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (user.status) {
                                "Online" -> OnlineGreen.copy(alpha = 0.2f)
                                "Away" -> AwayOrange.copy(alpha = 0.2f)
                                "Busy" -> BusyRed.copy(alpha = 0.2f)
                                else -> Color.Gray.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = user.status,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when (user.status) {
                                    "Online" -> OnlineGreen
                                    "Away" -> AwayOrange
                                    "Busy" -> BusyRed
                                    else -> Color.Gray
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Direct Chat Button
                            Button(
                                onClick = {
                                    viewModel.startDirectChat(user)
                                    val convId = listOf(viewModel.currentUserId, user.userId).sorted().let { "conv_${it[0]}_${it[1]}" }
                                    onNavigateToDirectChat(convId, user.userId)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("user_card_chat_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PulseBluePrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat", fontWeight = FontWeight.Bold)
                            }

                            // Connection Request Button
                            if (isConnected) {
                                OutlinedButton(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = OnlineGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Connected", color = OnlineGreen)
                                }
                            } else if (isOutgoingPending) {
                                OutlinedButton(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pending")
                                }
                            } else if (isIncomingPending) {
                                Button(
                                    onClick = {
                                        val req = incomingRequests.firstOrNull { it.fromUserId == user.userId }
                                        if (req != null) viewModel.acceptConnectionRequest(req)
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen)
                                ) {
                                    Text("Accept", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.sendConnectionRequest(user) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("user_card_connect_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Connect")
                                }
                            }
                        }
                    }
                }
            } else if (searchNotice != null) {
                // Notice when user not found or searching self
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = searchNotice ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Usernames are exact. Check that the username is spelled correctly.",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Empty search prompt: Explains strict privacy
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PulseCyanAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = PulseCyanAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Strict Privacy Protected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To guarantee total privacy and eliminate spam, PulseChat does not maintain a public user directory.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ask your friend for their unique @username, type it in the search bar above, and start private direct messaging.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
