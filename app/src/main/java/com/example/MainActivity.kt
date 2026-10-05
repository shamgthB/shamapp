package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.ChatRepository
import com.example.ui.AuthScreen
import com.example.ui.ChatViewModel
import com.example.ui.ClaimUsernameScreen
import com.example.ui.IndividualDirectChatScreen
import com.example.ui.MainChatScreen
import com.example.ui.ProfileScreen
import com.example.ui.UserDiscoveryScreen
import com.example.ui.navigation.Screen
import com.example.ui.signOutUser
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        // Unauthenticated: Login & Registration screen with real-time unique username checking
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        // Authenticated session: Feature ViewModel keyed by user.uid
        val chatViewModel: ChatViewModel = viewModel(
            key = user.uid,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY])
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    ChatViewModel(ChatRepository(db), user.uid)
                }
            }
        )

        val profile by chatViewModel.userProfile.collectAsStateWithLifecycle()

        // If user is authenticated but has not yet claimed a unique username (e.g. Google Sign-In)
        if (profile != null && profile?.username.isNullOrBlank()) {
            ClaimUsernameScreen(
                viewModel = chatViewModel,
                onSignOutRequested = {
                    signOutUser(
                        context = context,
                        onSignOutComplete = {
                            currentUser = null
                        },
                        scope = coroutineScope
                    )
                }
            )
        } else {
            // Main Navigation
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route
            ) {
                // Dashboard: Active Chats & Connections
                composable(Screen.Dashboard.route) {
                    MainChatScreen(
                        viewModel = chatViewModel,
                        onNavigateToProfile = {
                            navController.navigate(Screen.Profile.route)
                        },
                        onNavigateToDiscovery = {
                            navController.navigate(Screen.Discovery.route)
                        },
                        onNavigateToDirectChat = { convId, otherUid ->
                            navController.navigate(Screen.DirectChat.createRoute(convId, otherUid))
                        },
                        onSignOutRequested = {
                            signOutUser(
                                context = context,
                                onSignOutComplete = {
                                    currentUser = null
                                },
                                scope = coroutineScope
                            )
                        }
                    )
                }

                // Profile Settings Screen
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        viewModel = chatViewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onSignOutRequested = {
                            signOutUser(
                                context = context,
                                onSignOutComplete = {
                                    currentUser = null
                                },
                                scope = coroutineScope
                            )
                        }
                    )
                }

                // Username Search Screen (Strict Privacy)
                composable(Screen.Discovery.route) {
                    UserDiscoveryScreen(
                        viewModel = chatViewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToDirectChat = { convId, otherUid ->
                            navController.navigate(Screen.DirectChat.createRoute(convId, otherUid))
                        }
                    )
                }

                // Individual 1-on-1 Direct Chat Screen (Strict Chat Privacy)
                composable(
                    route = Screen.DirectChat.route,
                    arguments = listOf(
                        navArgument("conversationId") { type = NavType.StringType },
                        navArgument("otherUserId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                    val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
                    IndividualDirectChatScreen(
                        conversationId = conversationId,
                        otherUserId = otherUserId,
                        viewModel = chatViewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
