package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.PlayerProgress
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class ProgressRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveAndObserveProgress_authenticatedUser_succeeds() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = FirestoreSyncRepository(firestore, auth)

    val progress = PlayerProgress(
      stars = mapOf("1-1" to 3, "1-2" to 2),
      scores = mapOf("1-1" to 1500, "1-2" to 1200),
      solvedPuzzles = setOf("1-1", "1-2"),
      storiesSeen = setOf(1)
    )

    withTimeout(5000L) {
      repository.saveProgress(progress)
    }

    val fetched = withTimeout(5000L) {
      repository.fetchProgress()
    }

    assertNotNull(fetched)
    assertEquals(3, fetched?.stars?.get("1-1"))
    assertEquals(5, fetched?.totalStars)
  }

  @Test
  fun crossUserAccess_bobCannotReadAliceProgress_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = FirestoreSyncRepository(firestore, auth)

    val progress = PlayerProgress(stars = mapOf("1-1" to 3))
    withTimeout(5000L) {
      aliceRepo.saveProgress(progress)
    }

    // Switch to Bob
    signInTestUser(BOB_EMAIL)

    try {
      withTimeout(5000L) {
        firestore.collection("users").document(aliceUid).collection("progress").document("data").get().await()
      }
      fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }

  @Test
  fun unauthenticatedAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    auth.signOut()

    try {
      withTimeout(5000L) {
        firestore.collection("users").document(aliceUid).collection("progress").document("data").get().await()
      }
      fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val BOB_EMAIL = "bob@test.com"
  }
}
