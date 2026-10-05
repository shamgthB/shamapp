package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AwayOrange
import com.example.ui.theme.BusyRed
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PulseBluePrimary
import com.example.ui.theme.PulseCyanAccent
import com.example.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onSignOutRequested: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val acceptedConnections by viewModel.acceptedConnections.collectAsStateWithLifecycle()

    var displayName by remember(userProfile) { mutableStateOf(userProfile?.displayName ?: "") }
    var bio by remember(userProfile) { mutableStateOf(userProfile?.about ?: "") }
    var selectedStatus by remember(userProfile) { mutableStateOf(userProfile?.status ?: "Online") }
    var selectedAvatar by remember(userProfile) { mutableStateOf(userProfile?.photoUrl) }

    val snackbarHostState = remember { SnackbarHostState() }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    // Modern Zero-Permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingPhoto = true
            scope.launch(Dispatchers.IO) {
                val bytes = ImageUtil.compressUriToJpegBytes(context, uri)
                val base64Fallback = ImageUtil.convertUriToBase64(context, uri)

                if (bytes != null) {
                    viewModel.uploadProfileImage(bytes) { storageUrl ->
                        isUploadingPhoto = false
                        if (storageUrl != null) {
                            selectedAvatar = storageUrl
                            scope.launch {
                                snackbarHostState.showSnackbar("Profile photo uploaded to Firebase Storage! Click Save Changes.")
                            }
                        } else if (base64Fallback != null) {
                            selectedAvatar = base64Fallback
                            scope.launch {
                                snackbarHostState.showSnackbar("Profile photo selected! Click Save Changes to apply.")
                            }
                        }
                    }
                } else if (base64Fallback != null) {
                    withContext(Dispatchers.Main) {
                        isUploadingPhoto = false
                        selectedAvatar = base64Fallback
                        snackbarHostState.showSnackbar("Profile photo selected! Click Save Changes to apply.")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isUploadingPhoto = false
                        snackbarHostState.showSnackbar("Failed to process selected image.")
                    }
                }
            }
        }
    }

    val statuses = listOf("Online", "Away", "Busy", "Offline")
    val presetAvatars = listOf(
        "cat" to "🐱",
        "robot" to "🤖",
        "astronaut" to "👨‍🚀",
        "wizard" to "🧙‍♂️",
        "artist" to "🎨",
        "fire" to "🔥"
    )

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile & Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("profile_back_button")
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Profile Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Photo with Camera Badge Button
                    Box(contentAlignment = Alignment.BottomEnd) {
                        UserAvatar(
                            name = displayName.ifBlank { "User" },
                            photoUrl = selectedAvatar,
                            size = 88.dp
                        )
                        FilledIconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("upload_profile_photo_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = PulseCyanAccent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Upload Profile Picture",
                                tint = Color(0xFF003549),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = displayName.ifBlank { "User Profile" },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Immutable unique username handle
                    val usernameText = userProfile?.username?.takeIf { it.isNotBlank() } ?: "unclaimed"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PulseCyanAccent.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AlternateEmail,
                                contentDescription = null,
                                tint = PulseCyanAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = usernameText,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PulseCyanAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = userProfile?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${acceptedConnections.size} Connections",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                color = PulseCyanAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when (selectedStatus) {
                                "Online" -> OnlineGreen.copy(alpha = 0.2f)
                                "Away" -> AwayOrange.copy(alpha = 0.2f)
                                "Busy" -> BusyRed.copy(alpha = 0.2f)
                                else -> Color.Gray.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = selectedStatus,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                color = when (selectedStatus) {
                                    "Online" -> OnlineGreen
                                    "Away" -> AwayOrange
                                    "Busy" -> BusyRed
                                    else -> Color.Gray
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Edit Fields Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Edit Profile Details",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PulseCyanAccent
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_display_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PulseCyanAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio / Status Message") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_bio_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PulseCyanAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    // Presence status selector
                    Text(
                        text = "Presence Status",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
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

                    // Avatar Picker Button & Preset Badges
                    Text(
                        text = "Profile Picture / Avatar",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Custom Photo from Gallery")
                    }

                    Text(
                        text = "Or choose a preset avatar badge:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        presetAvatars.forEach { (key, emoji) ->
                            val isSelected = selectedAvatar == key
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) PulseCyanAccent else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clickable { selectedAvatar = if (isSelected) null else key }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    viewModel.updateProfileDetails(displayName, bio, selectedStatus, selectedAvatar)
                    scope.launch {
                        snackbarHostState.showSnackbar("Profile updated successfully!")
                    }
                },
                enabled = displayName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_profile_page_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PulseBluePrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Sign Out Button
            OutlinedButton(
                onClick = onSignOutRequested,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("profile_sign_out_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }
        }
    }
}
