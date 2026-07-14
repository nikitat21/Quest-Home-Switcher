package io.github.nikitat21.questhomeswitcher.domain

import io.github.nikitat21.questhomeswitcher.shell.ShellResult
import io.github.nikitat21.questhomeswitcher.shell.ShellRunner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivateHomeUseCaseTest {
    @Test
    fun unverifiedFileNeverTouchesPackageManager() = runBlocking {
        val shizuku = FakeShellRunner(ready = true)
        val result = ActivateHomeUseCase(shizuku, FakeShellRunner(ready = false))(home(verified = false))

        assertFalse(result.success)
        assertTrue(shizuku.commands.isEmpty())
    }

    @Test
    fun failedInstallRunsAutomaticRollback() = runBlocking {
        val shizuku = FakeShellRunner(
            ready = true,
            results = ArrayDeque(
                listOf(
                    ShellResult(0, "Home APK and scene verified."),
                    ShellResult(0, "Current Home backed up for automatic rollback."),
                    ShellResult(0, "Success"),
                    ShellResult(1, "Failure [INSTALL_FAILED_INVALID_APK]"),
                    ShellResult(0, "Success"),
                ),
            ),
        )
        val result = ActivateHomeUseCase(shizuku, FakeShellRunner(ready = false))(home())

        assertFalse(result.success)
        assertTrue(result.log.contains("previous Home was restored"))
        assertTrue(shizuku.commands.any { it.contains("quest_home_switcher_previous.apk") })
    }

    @Test
    fun verifiedInstallCompletesWithoutRebootPrompt() = runBlocking {
        val shizuku = FakeShellRunner(
            ready = true,
            results = ArrayDeque(
                listOf(
                    ShellResult(0, "Home APK and scene verified."),
                    ShellResult(0, "Current Home backed up for automatic rollback."),
                    ShellResult(0, "Success"),
                    ShellResult(0, "Success"),
                    ShellResult(0, "Installed package and scene verified."),
                    ShellResult(0, "Horizon Home reloaded"),
                    ShellResult(0, ""),
                ),
            ),
        )
        val result = ActivateHomeUseCase(shizuku, FakeShellRunner(ready = false))(home())

        assertTrue(result.success)
        assertFalse(result.needsReboot)
        assertTrue(shizuku.commands.any { it.contains("android.intent.category.HOME") })
        assertTrue(shizuku.commands.any { it.contains("pidof com.oculus.vrshell") })
        assertFalse(shizuku.commands.any { it.contains("PACKAGE_CHANGED") })
        assertFalse(shizuku.commands.any { it.contains("com.oculus.shellenv") })
    }

    @Test
    fun packagePublicationTimeoutRunsAutomaticRollback() = runBlocking {
        val shizuku = FakeShellRunner(
            ready = true,
            results = ArrayDeque(
                listOf(
                    ShellResult(0, "Home APK and scene verified."),
                    ShellResult(0, "Current Home backed up for automatic rollback."),
                    ShellResult(0, "Success"),
                    ShellResult(0, "Success"),
                    ShellResult(
                        7,
                        "Carrier verification stopped after 20 seconds (limit 20 seconds).\n" +
                            "Expected scene SHA-256: ${"a".repeat(64)}\n" +
                            "Observed scene: (no readable assets/scene.zip found)",
                    ),
                    ShellResult(0, "Success"),
                ),
            ),
        )

        val result = ActivateHomeUseCase(shizuku, FakeShellRunner(ready = false))(home())

        assertFalse(result.success)
        assertTrue(result.log.contains("previous Home was restored"))
        val verificationCommand = shizuku.commands.first { it.contains("verify_installed_scene 40 0.5 20") }
        assertTrue(verificationCommand.contains("pm path --user 0"))
        assertTrue(verificationCommand.contains("pm path ${QuestHomeContract.TargetPackage}"))
        assertTrue(verificationCommand.contains("cmd package path --user 0"))
        assertTrue(verificationCommand.contains("for installed in"))
        assertTrue(verificationCommand.contains("tr '[:upper:]' '[:lower:]'"))
        assertTrue(verificationCommand.contains("Expected scene SHA-256"))
        assertTrue(verificationCommand.contains("Observed scene"))
        assertTrue(verificationCommand.contains("Package paths"))
        assertFalse(verificationCommand.contains("head -n 1"))
        assertTrue(verificationCommand.contains("deadline="))
        assertTrue(verificationCommand.contains("last_signature"))
        assertTrue(verificationCommand.contains("stat -c %Y"))
        assertTrue(verificationCommand.contains("signature\" != \"${'$'}last_signature"))
    }

    @Test
    fun rollbackBackupUsesAllPackagePathFallbacksAndRejectsSplitBackup() = runBlocking {
        val shizuku = FakeShellRunner(
            ready = true,
            results = ArrayDeque(listOf(ShellResult(1, "Validation failed"))),
        )

        ActivateHomeUseCase(shizuku, FakeShellRunner(ready = false))(home())
        assertTrue(shizuku.commands.size == 1)

        val successfulRunner = FakeShellRunner(
            ready = true,
            results = ArrayDeque(
                listOf(
                    ShellResult(0, "Home APK and scene verified."),
                    ShellResult(0, "No readable current Home APK"),
                    ShellResult(1, "stop"),
                    ShellResult(0, "restored"),
                ),
            ),
        )
        ActivateHomeUseCase(successfulRunner, FakeShellRunner(ready = false))(home())

        val backupCommand = successfulRunner.commands[1]
        assertTrue(backupCommand.contains("pm path --user 0"))
        assertTrue(backupCommand.contains("pm path ${QuestHomeContract.TargetPackage}"))
        assertTrue(backupCommand.contains("cmd package path --user 0"))
        assertTrue(backupCommand.contains("cmd package path ${QuestHomeContract.TargetPackage}"))
        assertTrue(backupCommand.contains("readable_count"))
        assertTrue(backupCommand.contains("scene_count"))
        assertTrue(backupCommand.contains("multiple APK paths"))
        assertFalse(backupCommand.contains("head -n 1"))
    }

    @Test
    fun reloadFailureKeepsVerifiedHomeAndRequestsRestart() = runBlocking {
        val shizuku = FakeShellRunner(
            ready = true,
            results = ArrayDeque(
                listOf(
                    ShellResult(0, "Home APK and scene verified."),
                    ShellResult(0, "Current Home backed up for automatic rollback."),
                    ShellResult(0, "Success"),
                    ShellResult(0, "Success"),
                    ShellResult(0, "Installed package and scene verified."),
                    ShellResult(22, "VR Shell did not return."),
                    ShellResult(0, ""),
                ),
            ),
        )

        val result = ActivateHomeUseCase(shizuku, FakeShellRunner(ready = false))(home())

        assertTrue(result.success)
        assertTrue(result.needsReboot)
        assertTrue(result.log.contains("Restart the Quest once"))
        assertTrue(shizuku.commands.count { it.contains("pm uninstall --user 0") } == 1)
    }

    @Test
    fun carrierIntegrityFailureRunsRollbackAndReturnsFailure() = runBlocking {
        val shizuku = FakeShellRunner(
            ready = true,
            results = ArrayDeque(
                listOf(
                    ShellResult(0, "Home APK and scene verified."),
                    ShellResult(0, "Current Home backed up for automatic rollback."),
                    ShellResult(0, "Success"),
                    ShellResult(0, "Success"),
                    ShellResult(0, "Installed package and scene verified."),
                    ShellResult(23, "Carrier changed during reload."),
                    ShellResult(0, "Success"),
                ),
            ),
        )

        val result = ActivateHomeUseCase(shizuku, FakeShellRunner(ready = false))(home())

        assertFalse(result.success)
        assertTrue(result.needsReboot)
        assertTrue(result.log.contains("previous Home was restored"))
        assertEquals(2, shizuku.commands.count { it.contains("pm uninstall --user 0") })
        assertTrue(shizuku.commands.last().contains("rollback APK was preserved"))
    }

    @Test
    fun failedIntegrityRollbackKeepsBackupAndDoesNotOfferRestart() = runBlocking {
        val shizuku = FakeShellRunner(
            ready = true,
            results = ArrayDeque(
                listOf(
                    ShellResult(0, "Home APK and scene verified."),
                    ShellResult(0, "Current Home backed up for automatic rollback."),
                    ShellResult(0, "Success"),
                    ShellResult(0, "Success"),
                    ShellResult(0, "Installed package and scene verified."),
                    ShellResult(23, "Carrier changed during reload."),
                    ShellResult(1, "Rollback failed."),
                ),
            ),
        )

        val result = ActivateHomeUseCase(shizuku, FakeShellRunner(ready = false))(home())

        assertFalse(result.success)
        assertFalse(result.needsReboot)
        assertTrue(result.log.contains("automatic rollback failed"))
        assertTrue(shizuku.commands.last().contains("rollback APK was preserved"))
    }

    @Test
    fun unchangedVrShellPidRequestsRestartInsteadOfReportingCleanReload() = runBlocking {
        val shizuku = FakeShellRunner(
            ready = true,
            results = ArrayDeque(
                listOf(
                    ShellResult(0, "Home APK and scene verified."),
                    ShellResult(0, "Current Home backed up for automatic rollback."),
                    ShellResult(0, "Success"),
                    ShellResult(0, "Success"),
                    ShellResult(0, "Installed package and scene verified."),
                    ShellResult(24, "VR Shell PID did not change."),
                    ShellResult(0, ""),
                ),
            ),
        )

        val result = ActivateHomeUseCase(shizuku, FakeShellRunner(ready = false))(home())

        assertTrue(result.success)
        assertTrue(result.needsReboot)
        assertTrue(result.log.contains("VR Shell PID did not change"))
        assertTrue(shizuku.commands.any { it.contains("new_pid") && it.contains("!=") && it.contains("old_pid") })
    }

    @Test
    fun installedEnvironmentUsesRootPreferencesWithoutShizuku() = runBlocking {
        val shizuku = FakeShellRunner(ready = true)
        val root = FakeShellRunner(
            ready = true,
            results = ArrayDeque(listOf(ShellResult(0, "preferences updated"))),
        )
        val installed = home().copy(
            apkPath = "/data/app/example/base.apk",
            packageName = "com.environment.example",
            installed = true,
            sceneUri = "apk://com.environment.example/assets/scene.zip",
        )

        val result = ActivateHomeUseCase(shizuku, root)(installed)

        assertTrue(result.success)
        assertTrue(shizuku.commands.isEmpty())
        assertTrue(root.commands.single().contains("oculuspreferences --setc environment_selected"))
        assertTrue(root.commands.single().contains("am force-stop com.oculus.vrshell"))
    }

    @Test
    fun failedRootPreferenceUpdateReturnsFailureWithoutFallbackInstall() = runBlocking {
        val shizuku = FakeShellRunner(ready = true)
        val root = FakeShellRunner(
            ready = true,
            results = ArrayDeque(listOf(ShellResult(10, "preference write failed"))),
        )
        val installed = home().copy(
            apkPath = "/data/app/example/base.apk",
            packageName = "com.environment.example",
            installed = true,
            sceneUri = "apk://com.environment.example/assets/scene.zip",
        )

        val result = ActivateHomeUseCase(shizuku, root)(installed)

        assertFalse(result.success)
        assertTrue(result.log.contains("preference write failed"))
        assertTrue(shizuku.commands.isEmpty())
        assertTrue(root.commands.size == 1)
    }

    private fun home(verified: Boolean = true) = HomeEnvironment(
        displayName = "Test Home",
        apkPath = "/sdcard/Download/Test Home.apk",
        previewPath = null,
        sizeBytes = 1234L,
        lastModifiedMillis = 1L,
        packageName = QuestHomeContract.TargetPackage,
        verifiedHomeApk = verified,
        sceneHash = "a".repeat(64),
    )

    private class FakeShellRunner(
        private val ready: Boolean,
        private val results: ArrayDeque<ShellResult> = ArrayDeque(),
    ) : ShellRunner {
        val commands = mutableListOf<String>()

        override suspend fun isReady(): Boolean = ready
        override suspend fun requestPermissionIfNeeded(requestCode: Int) = Unit
        override suspend fun run(command: String): ShellResult {
            commands += command
            return if (results.isEmpty()) ShellResult(0, "") else results.removeFirst()
        }
    }
}
