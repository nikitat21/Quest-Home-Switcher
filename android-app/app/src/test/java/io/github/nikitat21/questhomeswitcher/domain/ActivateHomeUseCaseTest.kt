package io.github.nikitat21.questhomeswitcher.domain

import io.github.nikitat21.questhomeswitcher.shell.ShellResult
import io.github.nikitat21.questhomeswitcher.shell.ShellRunner
import kotlinx.coroutines.runBlocking
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
