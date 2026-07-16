package io.github.nikitat21.questhomeswitcher.data

import io.github.nikitat21.questhomeswitcher.domain.HomeEnvironment
import io.github.nikitat21.questhomeswitcher.domain.HomeEnvironmentSource
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

    @Test
    fun rootInstalledScanExcludesCarrierAndHashesRealHomeScenes() {
        val command = installedHomeLookupCommand()

        assertTrue(command.contains("[ \"${'$'}pkg\" = \"${QuestHomeContract.TargetPackage}\" ] && continue"))
        assertTrue(command.contains("unzip -p \"${'$'}path\" assets/scene.zip"))
        assertTrue(command.contains("sha256sum"))
        assertTrue(command.contains("printf '%s\\t%s\\t%s\\t%s\\n'"))
    }

    @Test
    fun officialLibraryCopyReplacesPersonalCopyWithoutLosingActiveHashAlias() {
        val personalHash = "11".repeat(32)
        val libraryHash = "22".repeat(32)
        val personal = home(
            name = "Cyber City",
            sceneHash = personalHash,
            path = "/sdcard/Download/Quest Homes/Cyber City.apk",
            source = HomeEnvironmentSource.PERSONAL_IMPORT,
            officialHomeId = "cyber-city",
        )
        val library = home(
            name = "Cyber City",
            sceneHash = libraryHash,
            path = "/sdcard/Download/Quest Homes/Official Library/Cyber City.apk",
            source = HomeEnvironmentSource.OFFICIAL_LIBRARY,
            officialHomeId = "cyber-city",
        )

        val merged = mergeHomeCandidates(listOf(personal, library))

        assertEquals(1, merged.size)
        assertEquals(HomeEnvironmentSource.OFFICIAL_LIBRARY, merged.single().source)
        assertEquals(setOf(personalHash, libraryHash), merged.single().knownSceneHashes)
        assertEquals(
            merged.single(),
            findHomeForActiveSceneOutput("ACTIVE_SCENE_SHA256=$personalHash", merged),
        )
    }

    @Test
    fun rootInstalledOfficialHomeWinsOverStoredCopies() {
        val personal = home(
            name = "Lakeside Peak",
            sceneHash = "33".repeat(32),
            source = HomeEnvironmentSource.PERSONAL_IMPORT,
            officialHomeId = "lakeside-peak",
        )
        val library = home(
            name = "Lakeside Peak",
            sceneHash = "44".repeat(32),
            source = HomeEnvironmentSource.OFFICIAL_LIBRARY,
            officialHomeId = "lakeside-peak",
        )
        val rootInstalled = home(
            name = "Lakeside Peak",
            sceneHash = "55".repeat(32),
            path = "/data/app/lakeside/base.apk",
            source = HomeEnvironmentSource.ROOT_INSTALLED,
            officialHomeId = "lakeside-peak",
            installed = true,
        )

        val merged = mergeHomeCandidates(listOf(library, rootInstalled, personal))

        assertEquals(1, merged.size)
        assertEquals(HomeEnvironmentSource.ROOT_INSTALLED, merged.single().source)
        assertTrue(merged.single().installed)
        assertEquals(3, merged.single().knownSceneHashes.size)
    }

    @Test
    fun unknownCustomHomesOnlyMergeByExactSceneHash() {
        val sharedHash = "66".repeat(32)
        val imported = home(
            name = "Custom City",
            sceneHash = sharedHash,
            source = HomeEnvironmentSource.PERSONAL_IMPORT,
        )
        val libraryFolderDuplicate = home(
            name = "Renamed Custom City",
            sceneHash = sharedHash,
            path = "/sdcard/Download/Quest Homes/Official Library/Custom City.apk",
            source = HomeEnvironmentSource.OFFICIAL_LIBRARY,
        )
        val differentScene = home(
            name = "Custom City",
            sceneHash = "77".repeat(32),
            path = "/sdcard/Download/Quest Homes/Custom City 2.apk",
            source = HomeEnvironmentSource.PERSONAL_IMPORT,
        )

        val merged = mergeHomeCandidates(listOf(imported, libraryFolderDuplicate, differentScene))

        assertEquals(2, merged.size)
        assertTrue(merged.any { it.source == HomeEnvironmentSource.OFFICIAL_LIBRARY })
        assertTrue(merged.any { it.sceneHash == "77".repeat(32) })
    }

    @Test
    fun mergeResultIsStableWhenInputOrderChanges() {
        val first = home(
            name = "Rockquarry",
            sceneHash = "88".repeat(32),
            path = "/sdcard/Download/Quest Homes/Rockquarry.apk",
            source = HomeEnvironmentSource.PERSONAL_IMPORT,
            officialHomeId = "rockquarry",
        )
        val second = home(
            name = "Rockquarry",
            sceneHash = "99".repeat(32),
            path = "/sdcard/Download/Quest Homes/Official Library/Rockquarry.apk",
            source = HomeEnvironmentSource.OFFICIAL_LIBRARY,
            officialHomeId = "rockquarry",
        )

        assertEquals(
            mergeHomeCandidates(listOf(first, second)),
            mergeHomeCandidates(listOf(second, first)),
        )
    }

    @Test
    fun sourceDetectionSeparatesManagedLibraryFromPersonalImports() {
        assertEquals(
            HomeEnvironmentSource.OFFICIAL_LIBRARY,
            homeSourceForPath("/sdcard/Download/Quest Homes/Official Library/Cyber City.apk"),
        )
        assertEquals(
            HomeEnvironmentSource.PERSONAL_IMPORT,
            homeSourceForPath("/sdcard/Download/Quest Homes/Cyber City.apk"),
        )
    }

    @Test
    fun currentLibrarySceneHashesResolveToCanonicalOfficialNames() {
        assertEquals(
            "Cyber City",
            OfficialHomeCatalog.identify(
                packageName = null,
                sceneHash = "09FB7EBE0703C9054C6401DAA552905D6486867DC7F48BC877A57D37B2E4FE19",
            )?.displayName,
        )
        assertEquals(
            "Lakeside Peak",
            OfficialHomeCatalog.identify(
                packageName = null,
                sceneHash = "553587B431FF523458B1EAE63FAC736992C85C6538852FC9FEE3397D178EF228",
            )?.displayName,
        )
        assertEquals(
            "Rockquarry",
            OfficialHomeCatalog.identify(
                packageName = null,
                sceneHash = "D81D1E062A42377371FAC4EBAAA39264C58D3C76ABBC8CDF752653AA3651D529",
            )?.displayName,
        )
        assertEquals(
            "Rockquarry",
            OfficialHomeCatalog.identify(
                packageName = "com.meta.environment.prod.rockquarry",
                sceneHash = null,
            )?.displayName,
        )
    }

    @Test
    fun officialHomeCarriesEveryKnownSceneAliasWithoutDuplicateFilesPresent() {
        val currentHash = "09FB7EBE0703C9054C6401DAA552905D6486867DC7F48BC877A57D37B2E4FE19"
        val olderCarrierHash = "34288D09200710F6CE53610192FB7EB07E4849ACAA37A85DA9B6C13AE8821662"
        val identity = OfficialHomeCatalog.identify(packageName = null, sceneHash = currentHash)
            ?: error("Cyber City must be in the official catalog")
        val onlyStoredCopy = home(
            name = identity.displayName,
            sceneHash = currentHash,
            source = HomeEnvironmentSource.OFFICIAL_LIBRARY,
            officialHomeId = identity.id,
        ).copy(knownSceneHashes = identity.sceneHashes)

        assertEquals(
            onlyStoredCopy,
            findHomeForActiveSceneOutput("ACTIVE_SCENE_SHA256=$olderCarrierHash", listOf(onlyStoredCopy)),
        )
    }

    private fun home(
        name: String,
        sceneHash: String,
        path: String = "/sdcard/Download/$name.apk",
        source: HomeEnvironmentSource = HomeEnvironmentSource.PERSONAL_IMPORT,
        officialHomeId: String? = null,
        installed: Boolean = false,
    ) = HomeEnvironment(
        displayName = name,
        apkPath = path,
        previewPath = null,
        sizeBytes = 1L,
        lastModifiedMillis = 1L,
        packageName = QuestHomeContract.TargetPackage,
        installed = installed,
        source = source,
        officialHomeId = officialHomeId,
        verifiedHomeApk = true,
        sceneHash = sceneHash,
        knownSceneHashes = setOf(sceneHash.lowercase()),
    )
}
