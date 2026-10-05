package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PulseBluePrimary
import com.example.ui.theme.PulseCyanAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimUsernameScreen(
    viewModel: ChatViewModel,
    onSignOutRequested: () -> Unit
) {
    var rawInput by remember { mutableStateOf("") }
    val isChecking by viewModel.isCheckingUsername.collectAsStateWithLifecycle()
    val isAvailable by viewModel.isUsernameAvailable.collectAsStateWithLifecycle()
    val checkMessage by viewModel.usernameCheckMessage.collectAsStateWithLifecycle()
    var isSubmitting by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(rawInput) {
        if (rawInput.isNotBlank()) {
            viewModel.checkUsername(rawInput)
        }
    }

    val clean = rawInput.trim().lowercase().removePrefix("@")

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Claim Unique Username",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
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
                .padding(24.dp),
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
                        imageVector = Icons.Default.AlternateEmail,
                        contentDescription = null,
                        tint = PulseCyanAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Choose Your PulseChat Handle",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "PulseChat enforces strict privacy. There is no public user directory; other users can only find and message you using your unique @username.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = rawInput,
                        onValueChange = {
                            rawInput = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '@' }
                            localError = null
                        },
                        label = { Text("Choose Username") },
                        prefix = { Text("@", color = PulseCyanAccent, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = PulseCyanAccent)
                        },
                        trailingIcon = {
                            if (isChecking) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else if (isAvailable == true) {
                                Icon(Icons.Default.Check, contentDescription = "Available", tint = OnlineGreen)
                            } else if (isAvailable == false && clean.length >= 3) {
                                Icon(Icons.Default.Close, contentDescription = "Taken", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        supportingText = {
                            if (checkMessage != null) {
                                Text(
                                    text = checkMessage ?: "",
                                    color = when (isAvailable) {
                                        true -> OnlineGreen
                                        false -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text("3 to 30 characters (letters, numbers, underscores)", fontSize = 11.sp)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("claim_username_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isAvailable == true) OnlineGreen else PulseCyanAccent,
                            unfocusedBorderColor = if (isAvailable == false && clean.length >= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            if (localError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = localError ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (clean.length < 3 || !clean.matches(Regex("^[a-z0-9_]{3,30}$"))) {
                        localError = "Please enter a valid username (3-30 letters, numbers, or underscores)."
                        return@Button
                    }
                    if (isAvailable != true) {
                        localError = "Please choose an available username."
                        return@Button
                    }
                    isSubmitting = true
                    viewModel.claimUsername(clean) { success ->
                        isSubmitting = false
                        if (!success) {
                            localError = "Failed to claim username. Please try again."
                        }
                    }
                },
                enabled = !isSubmitting && isAvailable == true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("claim_username_submit_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PulseBluePrimary,
                    contentColor = Color.White
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Claim @$clean & Continue",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onSignOutRequested,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Sign Out", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
