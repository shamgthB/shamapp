package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ChatRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveUserProfile_authenticatedUser_savesAndObserves() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = ChatRepository(firestore)

        val profile = UserProfile(
            userId = uid,
            displayName = "Alice Wonder",
            email = ALICE_EMAIL,
            status = "Online",
            about = "Coding in Kotlin Compose"
        )
        val saveResult = repo.saveUserProfile(profile)
        assertTrue(saveResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).first { it != null }
        }
        assertNotNull(observed)
        assertEquals("Alice Wonder", observed?.displayName)
    }

    @Test
    fun createChannelAndSendMessage_authenticatedUser_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = ChatRepository(firestore)

        val channelName = "General_${UUID.randomUUID().toString().take(6)}"
        val channelResult = repo.createChannel(channelName, "Discussion room")
        assertTrue(channelResult.isSuccess)
        val channelId = channelResult.getOrThrow()

        val msgResult = repo.sendChannelMessage(
            channelId = channelId,
            senderName = "Alice",
            senderPhotoUrl = null,
            text = "Welcome to the new channel!"
        )
        assertTrue(msgResult.isSuccess)

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeChannelMessages(channelId).first { list -> list.isNotEmpty() }
        }
        assertEquals(1, messages.size)
        assertEquals("Welcome to the new channel!", messages[0].text)
        assertEquals(uid, messages[0].senderId)
    }

    @Test
    fun unauthenticatedAccess_failsGracefully() = runBlocking {
        auth.signOut()
        val repo = ChatRepository(firestore)

        val result = repo.createChannel("HackerRoom", "Should fail")
        assertTrue(result.isFailure)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_test@example.com"
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
