package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ChatRepositoryRuleTest : FirestoreEmulatorTestBase() {

    companion object {
        const val ALICE_EMAIL = "alice_rule_test@example.com"
        const val FLOW_TIMEOUT_MS = 8000L
    }

    @Test
    fun saveUserProfile_authenticatedUser_savesAndObserves() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = ChatRepository(firestore)

        val profile = UserProfile(
            userId = uid,
            username = "alicewonder",
            displayName = "Alice Wonder",
            email = ALICE_EMAIL,
            status = "Online",
            about = "Coding in Kotlin Compose"
        )
        val saveResult = repo.saveUserProfile(profile)
        assertTrue(saveResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).filterNotNull().first()
        }
        assertEquals("Alice Wonder", observed.displayName)
        assertEquals("alicewonder", observed.username)
    }

    @Test
    fun claimUsername_and_searchByUsername_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = ChatRepository(firestore)

        val testUsername = "alex_${UUID.randomUUID().toString().replace("-", "").take(6)}"

        // 1. Check available
        val availableResult = repo.checkUsernameAvailable(testUsername)
        assertTrue(availableResult.isSuccess)
        assertTrue(availableResult.getOrThrow())

        // 2. Save base profile
        repo.saveUserProfile(
            UserProfile(userId = uid, displayName = "Alex", email = ALICE_EMAIL)
        )

        // 3. Claim username
        val claimResult = repo.claimUsername(testUsername, uid)
        assertTrue(claimResult.isSuccess)

        // 4. Now search by username
        val searchResult = repo.searchUserByUsername(testUsername)
        assertTrue(searchResult.isSuccess)
        val foundUser = searchResult.getOrThrow()
        assertNotNull(foundUser)
        assertEquals(uid, foundUser?.userId)
    }

    @Test
    fun startConversation_andSendMessage_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = ChatRepository(firestore)

        val bobEmail = "bob_${UUID.randomUUID().toString().replace("-", "").take(6)}@example.com"
        val bobUid = signInTestUser(bobEmail)

        // Switch back to Alice
        signInTestUser(ALICE_EMAIL)
        val convResult = repo.getOrCreateConversation(bobUid)
        if (convResult.isFailure) {
            System.err.println("DEBUG_TEST_ERROR: " + convResult.exceptionOrNull())
            convResult.exceptionOrNull()?.printStackTrace()
        }
        assertTrue(convResult.isSuccess)
        val convId = convResult.getOrThrow()

        val msgResult = repo.sendConversationMessage(
            conversationId = convId,
            senderName = "Alice",
            text = "Hello Bob! Strictly private message.",
            participantUids = listOf(aliceUid, bobUid).sorted()
        )
        assertTrue(msgResult.isSuccess)

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeConversationMessages(convId).first { it.isNotEmpty() }
        }
        assertEquals(1, messages.size)
        assertEquals("Hello Bob! Strictly private message.", messages[0].text)
        assertEquals(aliceUid, messages[0].senderId)
    }

    @Test
    fun sendConnectionRequest_andAccept_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = ChatRepository(firestore)

        repo.saveUserProfile(
            UserProfile(userId = aliceUid, displayName = "Alice", email = ALICE_EMAIL)
        )

        val bobEmail = "bob_${UUID.randomUUID().toString().replace("-", "").take(6)}@example.com"
        val bobUid = signInTestUser(bobEmail)
        repo.saveUserProfile(
            UserProfile(userId = bobUid, displayName = "Bob", email = bobEmail)
        )

        // Switch back to Alice
        signInTestUser(ALICE_EMAIL)
        val reqResult = repo.sendConnectionRequest(toUserId = bobUid, toUserName = "Bob")
        assertTrue(reqResult.isSuccess)
        val reqId = reqResult.getOrThrow()

        // Switch to Bob and accept
        signInTestUser(bobEmail)
        val acceptResult = repo.acceptConnectionRequest(reqId)
        assertTrue(acceptResult.isSuccess)
    }

    @Test
    fun unauthenticatedAccess_failsGracefully() = runBlocking {
        auth.signOut()
        val repo = ChatRepository(firestore)

        val result = repo.getOrCreateConversation("some_user")
        assertTrue(result.isFailure)
    }
}
