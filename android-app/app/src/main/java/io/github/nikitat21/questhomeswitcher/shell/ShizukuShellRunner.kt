package io.github.nikitat21.questhomeswitcher.shell

import android.content.pm.PackageManager
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class ShizukuShellRunner(
    private val commandTimeoutMillis: Long = DEFAULT_COMMAND_TIMEOUT_MS,
) : ShellRunner {
    fun isBinderAvailable(): Boolean = runCatching {
        Shizuku.pingBinder()
    }.onFailure {
        Log.w(TAG, "Binder check failed", it)
    }.getOrDefault(false)

    fun isPermissionGranted(binderAvailable: Boolean = isBinderAvailable()): Boolean = runCatching {
        binderAvailable && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.onFailure {
        Log.w(TAG, "Permission check failed", it)
    }.getOrDefault(false)

    override suspend fun isReady(): Boolean {
        return runCatching {
            val ping = isBinderAvailable()
            val permission = if (ping) Shizuku.checkSelfPermission() else Int.MIN_VALUE
            val ready = ping && permission == PackageManager.PERMISSION_GRANTED
            Log.i(TAG, "isReady ping=$ping permission=$permission ready=$ready")
            ready
        }.onFailure {
            Log.w(TAG, "isReady failed", it)
        }.getOrDefault(false)
    }

    override suspend fun requestPermissionIfNeeded(requestCode: Int) {
        runCatching {
            val ping = isBinderAvailable()
            val permission = if (ping) Shizuku.checkSelfPermission() else Int.MIN_VALUE
            Log.i(TAG, "requestPermissionIfNeeded ping=$ping permission=$permission")
            if (ping && permission != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(requestCode)
            }
        }.onFailure {
            Log.w(TAG, "requestPermissionIfNeeded failed", it)
        }
    }

    override suspend fun run(command: String): ShellResult = withContext(Dispatchers.IO) {
        if (!isReady()) {
            return@withContext ShellResult(1, "Shizuku is not ready or permission is missing.")
        }

        var process: Process? = null
        try {
            process = createShizukuProcess(command)
            executeProcess(process)
        } catch (cancelled: CancellationException) {
            process?.let(::destroySafely)
            throw cancelled
        } catch (error: Throwable) {
            process?.let(::destroySafely)
            Log.e(TAG, "Shizuku command failed", error)
            ShellResult(1, "Shizuku command failed: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun executeProcess(process: Process): ShellResult {
        val processExecutor = Executors.newFixedThreadPool(3) { runnable ->
            Thread(runnable, STREAM_THREAD_NAME).apply { isDaemon = true }
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
                waitForExit.get(commandTimeoutMillis, TimeUnit.MILLISECONDS)
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
                        "Command timed out after ${commandTimeoutMillis / 1_000} seconds.",
                    ),
                )
            } else {
                ShellResult(
                    exitCode = exitCode,
                    output = combineOutput(readCompletedStream(stdout), readCompletedStream(stderr)),
                )
            }
        } finally {
            waitForExit.cancel(true)
            stdout.cancel(true)
            stderr.cancel(true)
            processExecutor.shutdownNow()
        }
    }

    private fun destroySafely(process: Process) {
        runCatching {
            // ShizukuRemoteProcess does not implement Process.isAlive() or the
            // timed waitFor overload correctly; destroy() itself is supported.
            process.destroy()
        }.onFailure {
            Log.w(TAG, "Could not clean up a Shizuku process", it)
        }
    }

    private fun readCompletedStream(stream: Future<String>): String = runCatching {
        stream.get(STREAM_DRAIN_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    }.onFailure {
        Log.w(TAG, "Could not finish reading a Shizuku process stream", it)
    }.getOrDefault("")

    private fun combineOutput(vararg values: String): String =
        values.filter { it.isNotBlank() }.joinToString("\n") { it.trim() }

    private fun createShizukuProcess(command: String): Process {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        )
        method.isAccessible = true
        return method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
    }

    private companion object {
        private const val TAG = "QHS-Shizuku"
        private const val STREAM_THREAD_NAME = "qhs-shizuku-stream"
        private const val DEFAULT_COMMAND_TIMEOUT_MS = 120_000L
        private const val STREAM_DRAIN_TIMEOUT_MS = 2_000L
        private const val COMMAND_TIMEOUT_EXIT_CODE = 124
    }
}
