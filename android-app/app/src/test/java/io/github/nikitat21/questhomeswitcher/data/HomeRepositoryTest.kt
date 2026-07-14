package io.github.nikitat21.questhomeswitcher.data

import io.github.nikitat21.questhomeswitcher.domain.HomeEnvironment
import io.github.nikitat21.questhomeswitcher.domain.QuestHomeContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRepositoryTest {
    @Test
    fun activeHomeLookupChecksEveryPackagePathWithBoundedRetries() {
        val command = activeHomeLookupCommand()

        assertTrue(command.contains("pm path --user 0 ${QuestHomeContract.TargetPackage}"))
        assertTrue(command.contains("pm path ${QuestHomeContract.TargetPackage}"))
        assertTrue(command.contains("cmd package path --user 0 ${QuestHomeContract.TargetPackage}"))
        assertTrue(command.contains("cmd package path ${QuestHomeContract.TargetPackage}"))
        assertTrue(command.contains("sort -u"))
        assertTrue(command.contains("for installed in"))
        assertTrue(command.contains("max_attempts=10"))
        assertTrue(command.contains("retry_delay=0.5"))
        assertTrue(command.contains("tr '[:upper:]' '[:lower:]'"))
        assertTrue(command.contains("grep -Eq '^[0-9a-f]{64}${'$'}'"))
        assertTrue(command.contains("ACTIVE_SCENE_SHA256="))
        assertFalse(command.contains("head -n 1"))
    }

    @Test
    fun activeHomeOutputMatchesNormalizedExactSceneHash() {
        val expectedHash = "a1".repeat(32)
        val expectedHome = home("Expected", expectedHash.uppercase())
        val homes = listOf(home("Other", "b2".repeat(32)), expectedHome)
        val output = """
            diagnostic ${expectedHash.uppercase()}
            ACTIVE_SCENE_SHA256=${expectedHash.uppercase()}
        """.trimIndent()

        assertEquals(expectedHome, findHomeForActiveSceneOutput(output, homes))
    }

    @Test
    fun activeHomeOutputCanMatchAnySceneBearingPackagePath() {
        val firstHash = "c3".repeat(32)
        val secondHash = "d4".repeat(32)
        val secondHome = home("Second split", secondHash)
        val output = """
            ACTIVE_SCENE_SHA256=$firstHash
            ACTIVE_SCENE_SHA256=$secondHash
        """.trimIndent()

        assertEquals(secondHome, findHomeForActiveSceneOutput(output, listOf(secondHome)))
    }

    @Test
    fun activeHomeOutputRejectsUnprefixedOrMalformedHashes() {
        val expectedHash = "e5".repeat(32)
        val homes = listOf(home("Expected", expectedHash))

        assertNull(findHomeForActiveSceneOutput(expectedHash, homes))
        assertNull(findHomeForActiveSceneOutput("ACTIVE_SCENE_SHA256=${expectedHash}00", homes))
        assertNull(findHomeForActiveSceneOutput("ACTIVE_SCENE_SHA256=not-a-hash", homes))
    }

    private fun home(name: String, sceneHash: String) = HomeEnvironment(
        displayName = name,
        apkPath = "/sdcard/Download/$name.apk",
        previewPath = null,
        sizeBytes = 1L,
        lastModifiedMillis = 1L,
        packageName = QuestHomeContract.TargetPackage,
        verifiedHomeApk = true,
        sceneHash = sceneHash,
    )
}
