package io.github.nikitat21.questhomeswitcher.domain

import io.github.nikitat21.questhomeswitcher.shell.ShellRunner

class ActivateHomeUseCase(
    private val shellRunner: ShellRunner,
    private val rootRunner: ShellRunner,
) {
    suspend operator fun invoke(home: HomeEnvironment): ActivationResult {
        if (home.installed && home.sceneUri != null && rootRunner.isReady()) {
            return activateWithRoot(home)
        }
        if (!home.verifiedHomeApk || home.sceneHash.isNullOrBlank()) {
            return ActivationResult(false, false, "The selected file is not a verified Quest Home APK.")
        }
        if (home.packageName != null && home.packageName != QuestHomeContract.TargetPackage) {
            return ActivationResult(
                false,
                false,
                "This APK is not a NoRoot-Spoof Home. Its package is ${home.packageName}.",
            )
        }
        val packageRunner = when {
            rootRunner.isReady() -> rootRunner
            shellRunner.isReady() -> shellRunner
            else -> return ActivationResult(false, false, "Neither root nor Shizuku is ready.")
        }
        val log = StringBuilder()

        fun appendStep(title: String, output: String) {
            log.appendLine("== $title ==")
            if (output.isBlank()) {
                log.appendLine("(no output)")
            } else {
                log.appendLine(output.trim())
            }
            log.appendLine()
        }

        val escapedApkPath = home.apkPath.shellQuote()
        val expectedSceneHash = requireNotNull(home.sceneHash).shellQuote()
        val validate = packageRunner.run(
            """
                [ -f $escapedApkPath ] || { echo 'APK file not found.'; exit 2; }
                unzip -l $escapedApkPath 2>/dev/null | grep -qE '[[:space:]]assets/scene\.zip${'$'}' || { echo 'assets/scene.zip is missing.'; exit 3; }
                actual=${'$'}(unzip -p $escapedApkPath assets/scene.zip 2>/dev/null | sha256sum | cut -d ' ' -f 1)
                [ "${'$'}actual" = $expectedSceneHash ] || { echo 'The Home APK changed after it was scanned.'; exit 4; }
                echo 'Home APK and scene verified.'
            """.trimIndent(),
        )
        appendStep("Validate ${home.displayName}", validate.output)
        if (!validate.success) {
            return ActivationResult(false, false, log.appendLine("Validation failed; the current Home was not changed.").toString())
        }

        val backup = packageRunner.run(
            """
                rm -f $BACKUP_APK
                installed=${'$'}(pm path --user 0 ${QuestHomeContract.TargetPackage} 2>/dev/null | sed -n 's/^package://p' | head -n 1)
                if [ -n "${'$'}installed" ] && [ -r "${'$'}installed" ] && cp "${'$'}installed" $BACKUP_APK; then
                  echo 'Current Home backed up for automatic rollback.'
                else
                  echo 'No readable current Home APK; system restore will be used if needed.'
                fi
            """.trimIndent(),
        )
        appendStep("Create rollback point", backup.output)

        val uninstall = packageRunner.run("pm uninstall --user 0 ${QuestHomeContract.TargetPackage}")
        appendStep("Prepare Home slot", uninstall.output)

        val install = packageRunner.run("cat $escapedApkPath | pm install -S ${home.sizeBytes} -r -d -g --user 0")
        appendStep("Install ${home.displayName}", install.output)
        if (!install.success) {
            val rollback = rollback(packageRunner)
            appendStep("Automatic rollback", rollback.output)
            return ActivationResult(
                success = false,
                needsReboot = false,
                log = log.appendLine(
                    if (rollback.success) "Install failed; the previous Home was restored."
                    else "Install and automatic rollback failed. Open the setup log before retrying.",
                ).toString(),
            )
        }

        val verify = packageRunner.run(
            """
                installed=${'$'}(pm path --user 0 ${QuestHomeContract.TargetPackage} 2>/dev/null | sed -n 's/^package://p' | head -n 1)
                [ -n "${'$'}installed" ] && [ -r "${'$'}installed" ] || { echo 'Target Home package is missing.'; exit 5; }
                unzip -l "${'$'}installed" 2>/dev/null | grep -qE '[[:space:]]assets/scene\.zip${'$'}' || { echo 'Installed package has no scene.'; exit 6; }
                installed_hash=${'$'}(unzip -p "${'$'}installed" assets/scene.zip 2>/dev/null | sha256sum | cut -d ' ' -f 1)
                [ "${'$'}installed_hash" = $expectedSceneHash ] || { echo 'Installed scene does not match the selection.'; exit 7; }
                echo 'Installed package and scene verified.'
            """.trimIndent(),
        )
        appendStep("Verify installed Home", verify.output)
        if (!verify.success) {
            val rollback = rollback(packageRunner)
            appendStep("Automatic rollback", rollback.output)
            return ActivationResult(
                success = false,
                needsReboot = false,
                log = log.appendLine(
                    if (rollback.success) "Verification failed; the previous Home was restored."
                    else "Verification and automatic rollback failed. Open the setup log before retrying.",
                ).toString(),
            )
        }

        val reload = tryReloadHorizon(packageRunner)
        appendStep("Reload Horizon Home", reload.output)
        packageRunner.run("rm -f $BACKUP_APK")

        return ActivationResult(
            success = true,
            needsReboot = false,
            log = log.appendLine("Home applied successfully. Horizon Home is reloading.").toString(),
        )
    }

    private suspend fun activateWithRoot(home: HomeEnvironment): ActivationResult {
        val uri = requireNotNull(home.sceneUri).shellQuote()
        val preferenceCommands = when (home.type) {
            HomeEnvironmentType.ENVIRONMENT -> """
                oculuspreferences --setc environment_selected $uri || exit 10
                oculuspreferences --setc environment_default $uri || exit 11
                oculuspreferences --setc resolved_environment $uri || exit 12
            """.trimIndent()
            HomeEnvironmentType.VISTA -> """
                oculuspreferences --setc default_vista $uri || exit 20
                oculuspreferences --setc resolved_vista $uri || exit 21
                oculuspreferences --setc environment_vista_selected $uri || exit 22
            """.trimIndent()
            HomeEnvironmentType.FOOTPRINT ->
                "oculuspreferences --setc default_footprint $uri || exit 30"
        }
        val command = """
            command -v oculuspreferences >/dev/null 2>&1 || exit 127
            $preferenceCommands
            am force-stop com.oculus.vrshell
        """.trimIndent()
        val result = rootRunner.run(command)
        return ActivationResult(
            success = result.success,
            needsReboot = false,
            log = if (result.success) "Root mode: preferences updated and VR Shell reloaded." else result.output,
        )
    }

    private suspend fun tryReloadHorizon(runner: ShellRunner): io.github.nikitat21.questhomeswitcher.shell.ShellResult {
        val command = """
            am force-stop com.oculus.vrshell >/dev/null 2>&1
            am force-stop com.oculus.shellenv >/dev/null 2>&1
            cmd activity broadcast -a android.intent.action.PACKAGE_CHANGED -d package:${QuestHomeContract.TargetPackage} >/dev/null 2>&1
        """.trimIndent()

        return runner.run(command)
    }

    private suspend fun rollback(runner: ShellRunner): io.github.nikitat21.questhomeswitcher.shell.ShellResult {
        return runner.run(
            """
                pm uninstall --user 0 ${QuestHomeContract.TargetPackage} >/dev/null 2>&1 || true
                if [ -r $BACKUP_APK ]; then
                  backup_size=${'$'}(wc -c < $BACKUP_APK | tr -d ' ')
                  cat $BACKUP_APK | pm install -S "${'$'}backup_size" -r -d -g --user 0
                  result=${'$'}?
                  rm -f $BACKUP_APK
                  exit ${'$'}result
                fi
                cmd package install-existing --user 0 ${QuestHomeContract.TargetPackage}
            """.trimIndent(),
        )
    }

    private fun String.shellQuote(): String {
        return "'" + replace("'", "'\\''") + "'"
    }

    companion object {
        private const val BACKUP_APK = "/data/local/tmp/quest_home_switcher_previous.apk"
    }
}
