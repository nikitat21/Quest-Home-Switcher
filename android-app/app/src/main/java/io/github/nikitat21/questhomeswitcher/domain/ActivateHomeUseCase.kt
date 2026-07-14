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
                expected=${'$'}(printf '%s' $expectedSceneHash | tr '[:upper:]' '[:lower:]')
                actual=${'$'}(unzip -p $escapedApkPath assets/scene.zip 2>/dev/null | sha256sum | cut -d ' ' -f 1 | tr '[:upper:]' '[:lower:]')
                [ "${'$'}actual" = "${'$'}expected" ] || { echo 'The Home APK changed after it was scanned.'; exit 4; }
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
                candidate_paths=${'$'}(
                  {
                    pm path --user 0 ${QuestHomeContract.TargetPackage}
                    pm path ${QuestHomeContract.TargetPackage}
                    cmd package path --user 0 ${QuestHomeContract.TargetPackage}
                    cmd package path ${QuestHomeContract.TargetPackage}
                  } 2>/dev/null | tr -d '\r' | sed -n 's/^package://p' | sort -u
                )
                readable_count=0
                backup_source=''
                scene_count=0
                for installed in ${'$'}candidate_paths; do
                  [ -r "${'$'}installed" ] || continue
                  readable_count=${'$'}((readable_count + 1))
                  if unzip -l "${'$'}installed" 2>/dev/null | grep -qE '[[:space:]]assets/scene\.zip${'$'}'; then
                    backup_source="${'$'}installed"
                    scene_count=${'$'}((scene_count + 1))
                  fi
                done
                if [ "${'$'}readable_count" -eq 1 ] && [ "${'$'}scene_count" -eq 1 ] && cp "${'$'}backup_source" $BACKUP_APK; then
                  echo 'Current Home backed up for automatic rollback.'
                elif [ "${'$'}readable_count" -gt 1 ]; then
                  echo 'Current Home uses multiple APK paths; skipped an incomplete APK backup. System restore will be used if needed.'
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
                ${installedSceneVerificationFunction(expectedSceneHash)}
                verify_installed_scene 40 0.5 20
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

        val reload = tryReloadHorizon(packageRunner, expectedSceneHash)
        appendStep("Reload Horizon Home", reload.output)
        if (reload.exitCode == RELOAD_INTEGRITY_FAILURE_EXIT_CODE) {
            val rollback = rollback(packageRunner)
            appendStep("Automatic rollback", rollback.output)
            return ActivationResult(
                success = false,
                needsReboot = rollback.success,
                log = log.appendLine(
                    if (rollback.success) {
                        "Home integrity changed during reload; the previous Home was restored. Restart the Quest once."
                    } else {
                        "Home integrity changed during reload and automatic rollback failed. Open the setup log before retrying."
                    },
                ).toString(),
            )
        }
        packageRunner.run("rm -f $BACKUP_APK")

        return ActivationResult(
            success = true,
            needsReboot = !reload.success,
            log = log.appendLine(
                if (reload.success) {
                    "Home applied successfully. Horizon Home was reloaded."
                } else {
                    "Home was installed and verified, but Horizon Home did not reload cleanly. Restart the Quest once."
                },
            ).toString(),
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

    private suspend fun tryReloadHorizon(
        runner: ShellRunner,
        expectedSceneHash: String,
    ): io.github.nikitat21.questhomeswitcher.shell.ShellResult {
        val command = """
            old_pid=${'$'}(pidof com.oculus.vrshell | awk '{print ${'$'}1}')
            am force-stop com.oculus.vrshell >/dev/null 2>&1 || exit 20
            am start --user 0 -W -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null 2>&1 || exit 21
            new_pid=''
            for attempt in 1 2 3 4 5 6 7 8; do
              new_pid=${'$'}(pidof com.oculus.vrshell | awk '{print ${'$'}1}')
              [ -n "${'$'}new_pid" ] && break
              sleep 1
            done
            ${installedSceneVerificationFunction(expectedSceneHash)}
            verify_installed_scene 16 0.5 8 || { echo 'Carrier changed during reload.'; exit $RELOAD_INTEGRITY_FAILURE_EXIT_CODE; }
            [ -n "${'$'}new_pid" ] || { echo 'VR Shell did not return.'; exit 22; }
            [ -z "${'$'}old_pid" ] || [ "${'$'}new_pid" != "${'$'}old_pid" ] || { echo 'VR Shell PID did not change.'; exit 24; }
            echo "Horizon Home reloaded (VrShell PID ${'$'}old_pid -> ${'$'}new_pid)."
        """.trimIndent()

        return runner.run(command)
    }

    private fun installedSceneVerificationFunction(expectedSceneHash: String): String {
        return """
            verify_installed_scene() {
              max_attempts="${'$'}1"
              retry_delay="${'$'}2"
              timeout_seconds="${'$'}3"
              expected_hash=${'$'}(printf '%s' $expectedSceneHash | tr '[:upper:]' '[:lower:]')
              started_at=${'$'}(date +%s)
              deadline=${'$'}((started_at + timeout_seconds))
              attempt=1
              last_paths='(none reported)'
              last_observed='(no readable assets/scene.zip found)'
              last_signature=''
              while [ "${'$'}attempt" -le "${'$'}max_attempts" ]; do
                candidate_paths=${'$'}(
                  {
                    pm path --user 0 ${QuestHomeContract.TargetPackage}
                    pm path ${QuestHomeContract.TargetPackage}
                    cmd package path --user 0 ${QuestHomeContract.TargetPackage}
                    cmd package path ${QuestHomeContract.TargetPackage}
                  } 2>/dev/null | tr -d '\r' | sed -n 's/^package://p' | sort -u
                )
                [ -n "${'$'}candidate_paths" ] && last_paths="${'$'}candidate_paths"
                signature=''
                for installed in ${'$'}candidate_paths; do
                  if [ -r "${'$'}installed" ]; then
                    apk_size=${'$'}(wc -c < "${'$'}installed" 2>/dev/null | tr -d ' ')
                    apk_modified=${'$'}(stat -c %Y "${'$'}installed" 2>/dev/null)
                    signature="${'$'}signature${'$'}installed:${'$'}apk_size:${'$'}apk_modified;"
                  else
                    signature="${'$'}signature${'$'}installed:unreadable;"
                  fi
                done
                if [ "${'$'}signature" != "${'$'}last_signature" ]; then
                  last_signature="${'$'}signature"
                  observed=''
                  for installed in ${'$'}candidate_paths; do
                    if [ ! -r "${'$'}installed" ]; then
                      observed="${'$'}observed${'$'}installed=unreadable; "
                      continue
                    fi
                    if ! unzip -l "${'$'}installed" 2>/dev/null | grep -qE '[[:space:]]assets/scene\.zip${'$'}'; then
                      observed="${'$'}observed${'$'}installed=no-scene; "
                      continue
                    fi
                    installed_hash=${'$'}(unzip -p "${'$'}installed" assets/scene.zip 2>/dev/null | sha256sum | cut -d ' ' -f 1 | tr '[:upper:]' '[:lower:]')
                    if [ -z "${'$'}installed_hash" ]; then
                      observed="${'$'}observed${'$'}installed=hash-failed; "
                      continue
                    fi
                    observed="${'$'}observed${'$'}installed=${'$'}installed_hash; "
                    if [ "${'$'}installed_hash" = "${'$'}expected_hash" ]; then
                      echo "Installed package and scene verified at ${'$'}installed (attempt ${'$'}attempt)."
                      return 0
                    fi
                  done
                  [ -n "${'$'}observed" ] && last_observed="${'$'}observed"
                fi
                now=${'$'}(date +%s)
                [ "${'$'}now" -ge "${'$'}deadline" ] && break
                [ "${'$'}attempt" -lt "${'$'}max_attempts" ] && sleep "${'$'}retry_delay"
                attempt=${'$'}((attempt + 1))
              done
              elapsed=${'$'}(( ${'$'}(date +%s) - started_at ))
              echo "Carrier verification stopped after ${'$'}elapsed seconds (limit ${'$'}timeout_seconds seconds)."
              echo "Expected scene SHA-256: ${'$'}expected_hash"
              echo "Observed scene: ${'$'}last_observed"
              echo "Package paths: ${'$'}last_paths"
              return 7
            }
        """.trimIndent()
    }

    private suspend fun rollback(runner: ShellRunner): io.github.nikitat21.questhomeswitcher.shell.ShellResult {
        return runner.run(
            """
                pm uninstall --user 0 ${QuestHomeContract.TargetPackage} >/dev/null 2>&1 || true
                if [ -r $BACKUP_APK ]; then
                  backup_size=${'$'}(wc -c < $BACKUP_APK | tr -d ' ')
                  cat $BACKUP_APK | pm install -S "${'$'}backup_size" -r -d -g --user 0
                  result=${'$'}?
                  if [ "${'$'}result" -eq 0 ]; then
                    rm -f $BACKUP_APK
                  else
                    echo 'Automatic rollback failed; the rollback APK was preserved.'
                  fi
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
        private const val RELOAD_INTEGRITY_FAILURE_EXIT_CODE = 23
    }
}
