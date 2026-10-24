package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("أسرار النيل", appName)
  }

  @Test
  fun `verify level progression and unlock rules`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.data.GameRepository(context)
    val progress = repository.progress.value

    // Level 1-1 is unlocked by default
    assertTrue(repository.isLevelUnlocked("1-1", progress))

    // Level 1-2 is locked until 1-1 has stars and puzzle is solved
    assertFalse(repository.isLevelUnlocked("1-2", progress))

    // Complete 1-1 and solve its puzzle
    repository.recordLevelCompletion("1-1", stars = 3, score = 1500)
    repository.recordPuzzleSolved("1-1")

    val updatedProgress = repository.progress.value
    assertTrue(repository.isLevelUnlocked("1-2", updatedProgress))
    assertEquals(3, updatedProgress.totalStars)
  }

  @Test
  fun `verify chapters exceed 5000 and have dynamic level generation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.data.GameRepository(context)

    assertTrue("Chapters must exceed 5000", com.example.data.GameRepository.MAX_CHAPTERS > 5000)
    assertEquals(5200, com.example.data.GameRepository.MAX_CHAPTERS)
    assertEquals(31200, com.example.data.GameRepository.TOTAL_STAGES)

    // Verify Chapter 5000 can be retrieved cleanly
    val chapter5000 = repository.getChapter(5000)
    assertNotNull(chapter5000)
    assertEquals(5000, chapter5000.id)
    assertEquals(6, chapter5000.levels.size)
    assertEquals("5000-1", chapter5000.levels[0].id)
    assertEquals("5000-6", chapter5000.levels[5].id)
    assertTrue(chapter5000.levels[5].isBoss)

    // Verify Chapter 5200 (boundary)
    val chapter5200 = repository.getChapter(5200)
    assertEquals(5200, chapter5200.id)
    assertEquals(6, chapter5200.levels.size)
  }

  @Test
  fun `verify AdMobManager components and test unit ids`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val adManager = com.example.ads.AdMobManager(context)
    assertNotNull(adManager)
    assertEquals("ca-app-pub-5782293160806349/6965177176", com.example.ads.AdMobManager.BANNER_ID)
    assertEquals("ca-app-pub-5782293160806349/6602215096", com.example.ads.AdMobManager.INTERSTITIAL_ID)
    assertEquals("ca-app-pub-5782293160806349/1465438484", com.example.ads.AdMobManager.REWARDED_ID)
  }
}
