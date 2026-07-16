package io.github.nikitat21.questhomeswitcher.shell

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RootShellRunner internal constructor(
    private val commandTimeoutMillis: Long,
    private val rootCheckTimeoutMillis: Long,
    private val processFactory: (String) -> Process,
) : ShellRunner {
    constructor() : this(
        commandTimeoutMillis = DEFAULT_COMMAND_TIMEOUT_MS,
        rootCheckTimeoutMillis = ROOT_CHECK_TIMEOUT_MS,
        processFactory = { command -> ProcessBuilder("su", "-c", command).start() },
    )

    override suspend fun isReady(): Boolean = withContext(Dispatchers.IO) {
        val result = runDirect("id", rootCheckTimeoutMillis)
        result.success && ROOT_UID_PATTERN.containsMatchIn(result.output)
    }

    override suspend fun requestPermissionIfNeeded(requestCode: Int) {
        // Running su triggers the Magisk prompt when approval is still missing.
        isReady()
    }

    override suspend fun run(command: String): ShellResult = withContext(Dispatchers.IO) {
        runDirect(command, commandTimeoutMillis)
    }

    private fun runDirect(command: String, timeoutMillis: Long): ShellResult {
        var process: Process? = null
        return try {
            process = processFactory(command)
            executeProcess(process, timeoutMillis)
        } catch (interrupted: InterruptedException) {
            process?.let(::destroySafely)
            Thread.currentThread().interrupt()
            ShellResult(INTERRUPTED_EXIT_CODE, "Root command was interrupted.")
        } catch (error: Exception) {
            process?.let(::destroySafely)
            ShellResult(-1, "Root unavailable: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun executeProcess(process: Process, timeoutMillis: Long): ShellResult {
        val processExecutor = Executors.newFixedThreadPool(3) { runnable ->
            Thread(runnable, PROCESS_THREAD_NAME).apply { isDaemon = true }
        }
        val stdout = processExecutor.submit<String> {
            BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
        }
        val stderr = processExecutor.submit<String> {
            BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
        }
        val waitForExit = processExecutor.submit<Int> { process.waitFor() }

        return try {
            val exitCode = try {
                waitForExit.get(timeoutMillis, TimeUnit.MILLISECONDS)
            } catch (_: TimeoutException) {
                null
            }

            if (exitCode == null) {
                destroySafely(process)
                ShellResult(
                    exitCode = COMMAND_TIMEOUT_EXIT_CODE,
                    output = combineOutput(
                        readCompletedStream(stdout),
                        readCompletedStream(stderr),
                        "Root command timed out after ${formatTimeout(timeoutMillis)}.",
                    ),
                )
            } else {
                ShellResult(
                    exitCode = exitCode,
                    output = combineOutput(readCompletedStream(stdout), readCompletedStream(stderr)),
                )
            }
        } finally {
            destroySafely(process)
            waitForExit.cancel(true)
            stdout.cancel(true)
            stderr.cancel(true)
            processExecutor.shutdownNow()
        }
    }

    private fun destroySafely(process: Process) {
        try {
            if (!process.isAlive) return
            process.destroy()
            if (process.isAlive && !process.waitFor(PROCESS_DESTROY_GRACE_MS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
            }
        } catch (interrupted: InterruptedException) {
            runCatching { process.destroyForcibly() }
            Thread.currentThread().interrupt()
        } catch (_: Exception) {
            runCatching { process.destroyForcibly() }
        }
    }

    private fun readCompletedStream(stream: Future<String>): String = runCatching {
        stream.get(STREAM_DRAIN_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    }.getOrDefault("")

    private fun combineOutput(vararg values: String): String =
        values.filter { it.isNotBlank() }.joinToString("\n") { it.trim() }

    private fun formatTimeout(timeoutMillis: Long): String = when {
        timeoutMillis < 1_000L -> "$timeoutMillis ms"
        timeoutMillis % 1_000L == 0L -> "${timeoutMillis / 1_000L} seconds"
        else -> "${timeoutMillis / 1_000.0} seconds"
    }

    private companion object {
        private const val PROCESS_THREAD_NAME = "qhs-root-process"
        private const val DEFAULT_COMMAND_TIMEOUT_MS = 120_000L
        private const val ROOT_CHECK_TIMEOUT_MS = 900L
        private const val PROCESS_DESTROY_GRACE_MS = 500L
        private const val STREAM_DRAIN_TIMEOUT_MS = 2_000L
        private const val COMMAND_TIMEOUT_EXIT_CODE = 124
        private const val INTERRUPTED_EXIT_CODE = 130
        private val ROOT_UID_PATTERN = Regex("(?:^|\\s)uid=0(?:\\(|\\s|$)")
    }
}
