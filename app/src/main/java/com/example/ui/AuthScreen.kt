package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.model.UsernameRegistration
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PulseBluePrimary
import com.example.ui.theme.PulseCyanAccent
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "AuthScreen"

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Tab state: 0 = Sign In, 1 = Create Account
    var selectedAuthTab by remember { mutableIntStateOf(0) }

    // Form inputs
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Real-time username verification state
    var isCheckingUsername by remember { mutableStateOf(false) }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var usernameCheckMessage by remember { mutableStateOf<String?>(null) }
    var usernameDebounceJob by remember { mutableStateOf<Job?>(null) }

    val databaseId = remember { context.getString(R.string.firestore_database_id) }
    val db = remember { FirebaseFirestore.getInstance(databaseId) }

    // Real-time check whenever usernameInput changes
    LaunchedEffect(usernameInput) {
        val clean = usernameInput.trim().lowercase().removePrefix("@")
        usernameDebounceJob?.cancel()

        if (clean.isEmpty()) {
            isCheckingUsername = false
            isUsernameAvailable = null
            usernameCheckMessage = null
            return@LaunchedEffect
        }

        if (clean.length < 3) {
            isCheckingUsername = false
            isUsernameAvailable = null
            usernameCheckMessage = "Username must be at least 3 characters"
            return@LaunchedEffect
        }

        if (!clean.matches(Regex("^[a-z0-9_]{3,30}$"))) {
            isCheckingUsername = false
            isUsernameAvailable = false
            usernameCheckMessage = "Letters, numbers, and underscores only"
            return@LaunchedEffect
        }

        isCheckingUsername = true
        usernameCheckMessage = null

        usernameDebounceJob = coroutineScope.launch(Dispatchers.IO) {
            delay(350)
            try {
                val doc = db.collection("usernames").document(clean).get().await()
                withContext(Dispatchers.Main) {
                    isCheckingUsername = false
                    val available = !doc.exists()
                    isUsernameAvailable = available
                    usernameCheckMessage = if (available) {
                        "✅ @$clean is available!"
                    } else {
                        "❌ @$clean is already taken"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isCheckingUsername = false
                    isUsernameAvailable = null
                    usernameCheckMessage = "Could not check availability"
                }
            }
        }
    }

    // Attempt silent auto-sign in once on entry
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = coroutineScope
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Hero Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.chat_hero_1791136254165),
                        contentDescription = "PulseChat connection illustration",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xDD0D1B2A))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PulseCyanAccent,
                            modifier = Modifier.size(36.dp)
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
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Private, end-to-end direct messaging",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Error banner if any
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Auth Tabs: Sign In / Create Account
            TabRow(
                selectedTabIndex = selectedAuthTab,
                containerColor = Color.Transparent,
                contentColor = PulseCyanAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedAuthTab]),
                        color = PulseCyanAccent,
                        height = 3.dp
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedAuthTab == 0,
                    onClick = {
                        selectedAuthTab = 0
                        errorMessage = null
                    },
                    text = {
                        Text(
                            text = "Sign In",
                            fontWeight = if (selectedAuthTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                )
                Tab(
                    selected = selectedAuthTab == 1,
                    onClick = {
                        selectedAuthTab = 1
                        errorMessage = null
                    },
                    text = {
                        Text(
                            text = "Create Account",
                            fontWeight = if (selectedAuthTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Registration Fields
            if (selectedAuthTab == 1) {
                // Display Name
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Display Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PulseCyanAccent) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PulseCyanAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Unique Username Field with Live Database Availability Check
                val cleanUname = usernameInput.trim().lowercase().removePrefix("@")
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = {
                        // Keep lowercase alphanumeric + underscore
                        usernameInput = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '@' }
                    },
                    label = { Text("Unique Username (e.g. @alex)") },
                    prefix = { Text("@", color = PulseCyanAccent, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = PulseCyanAccent) },
                    trailingIcon = {
                        if (isCheckingUsername) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else if (isUsernameAvailable == true) {
                            Icon(Icons.Default.Check, contentDescription = "Available", tint = OnlineGreen)
                        } else if (isUsernameAvailable == false && cleanUname.length >= 3) {
                            Icon(Icons.Default.Close, contentDescription = "Taken", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    supportingText = {
                        if (usernameCheckMessage != null) {
                            Text(
                                text = usernameCheckMessage ?: "",
                                color = when (isUsernameAvailable) {
                                    true -> OnlineGreen
                                    false -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isUsernameAvailable != null) FontWeight.SemiBold else FontWeight.Normal
                            )
                        } else {
                            Text("Must be unique; used by others to find and chat with you", fontSize = 11.sp)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_username_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isUsernameAvailable == true) OnlineGreen else PulseCyanAccent,
                        unfocusedBorderColor = if (isUsernameAvailable == false && cleanUname.length >= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Email input
            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PulseCyanAccent) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PulseCyanAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password input
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text(if (selectedAuthTab == 1) "Password (min 6 chars)" else "Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PulseCyanAccent) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PulseCyanAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Auth Button (Sign In or Sign Up)
            Button(
                onClick = {
                    val cleanUsername = usernameInput.trim().lowercase().removePrefix("@")
                    if (emailInput.isBlank() || !emailInput.contains("@")) {
                        errorMessage = "Please enter a valid email address."
                        return@Button
                    }
                    if (passwordInput.length < 6) {
                        errorMessage = "Password must be at least 6 characters."
                        return@Button
                    }
                    if (selectedAuthTab == 1) {
                        if (nameInput.isBlank()) {
                            errorMessage = "Please enter your display name."
                            return@Button
                        }
                        if (cleanUsername.length < 3 || !cleanUsername.matches(Regex("^[a-z0-9_]{3,30}$"))) {
                            errorMessage = "Username must be 3-30 characters (letters, numbers, underscores only)."
                            return@Button
                        }
                        if (isUsernameAvailable == false) {
                            errorMessage = "The username @$cleanUsername is already taken. Please choose another."
                            return@Button
                        }
                    }

                    isLoading = true
                    errorMessage = null

                    coroutineScope.launch {
                        try {
                            if (selectedAuthTab == 1) {
                                // 1. Double check username availability right before creation
                                val doc = withContext(Dispatchers.IO) {
                                    db.collection("usernames").document(cleanUsername).get().await()
                                }
                                if (doc.exists()) {
                                    isLoading = false
                                    errorMessage = "Username @$cleanUsername was just claimed by someone else. Please pick another."
                                    return@launch
                                }

                                // 2. Create account in Firebase Auth
                                val authResult = Firebase.auth.createUserWithEmailAndPassword(
                                    emailInput.trim(),
                                    passwordInput
                                ).await()
                                val user = authResult.user ?: throw IllegalStateException("User creation failed")

                                // 3. Update Firebase Auth displayName
                                val profileUpdates = userProfileChangeRequest {
                                    displayName = nameInput.trim()
                                }
                                user.updateProfile(profileUpdates).await()

                                // 4. Atomically claim username & create user profile in Firestore
                                withContext(Dispatchers.IO) {
                                    val batch = db.batch()
                                    val unameRef = db.collection("usernames").document(cleanUsername)
                                    val userRef = db.collection("users").document(user.uid)

                                    batch.set(
                                        unameRef,
                                        UsernameRegistration(username = cleanUsername, userId = user.uid).toMap()
                                    )
                                    val initialProfile = UserProfile(
                                        userId = user.uid,
                                        username = cleanUsername,
                                        displayName = nameInput.trim(),
                                        email = emailInput.trim(),
                                        status = "Online",
                                        about = "Hey there! I am using PulseChat."
                                    )
                                    batch.set(userRef, initialProfile.toCreateMap())
                                    batch.commit().await()
                                }

                                isLoading = false
                                onAuthSuccess()
                            } else {
                                // Sign in with email/password
                                Firebase.auth.signInWithEmailAndPassword(
                                    emailInput.trim(),
                                    passwordInput
                                ).await()
                                isLoading = false
                                onAuthSuccess()
                            }
                        } catch (e: FirebaseAuthWeakPasswordException) {
                            isLoading = false
                            errorMessage = "The password is too weak. Please use at least 6 characters."
                        } catch (e: FirebaseAuthUserCollisionException) {
                            isLoading = false
                            errorMessage = "An account already exists with this email. Try signing in instead."
                        } catch (e: FirebaseAuthInvalidCredentialsException) {
                            isLoading = false
                            errorMessage = "Invalid email or password. Please verify your credentials."
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = e.localizedMessage ?: "Authentication failed. Please try again."
                            Log.e(TAG, "Email auth error", e)
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("auth_submit_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PulseBluePrimary,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = if (selectedAuthTab == 1) "Create Account" else "Sign In",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Divider OR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "  OR  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Google Sign-In Button
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { err ->
                            isLoading = false
                            errorMessage = err
                        },
                        scope = coroutineScope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_sign_in_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "G",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = PulseBluePrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy highlights
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureRow(
                    icon = Icons.Default.Security,
                    title = "Strict Privacy by Design",
                    desc = "No public user directory. People can only reach you by your @username."
                )
                FeatureRow(
                    icon = Icons.Default.Speed,
                    title = "Real-Time 1-on-1 Chats",
                    desc = "Direct messages are private and accessible only to participants."
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PulseCyanAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PulseCyanAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type returned")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign-in failed. Please try again.")
        }
    }
}

fun signOutUser(
    context: Context,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    val credentialManager = CredentialManager.create(context)
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}
