package io.github.nikitat21.questhomeswitcher.shell

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootShellRunnerTest {
    @Test
    fun `successful command returns exit code and both output streams`() = runBlocking {
        val process = FakeProcess(stdout = "uid=0(root)\n", stderr = "diagnostic\n")
        val runner = runnerWith { process }

        val result = runner.run("id")

        assertEquals(0, result.exitCode)
        assertEquals("uid=0(root)\ndiagnostic", result.output)
        assertFalse(process.destroyed)
    }

    @Test
    fun `nonzero process result preserves stderr`() = runBlocking {
        val runner = runnerWith {
            FakeProcess(exitCode = 7, stderr = "permission denied")
        }

        val result = runner.run("restricted-command")

        assertEquals(7, result.exitCode)
        assertEquals("permission denied", result.output)
        assertFalse(result.success)
    }

    @Test
    fun `timeout terminates process and returns timeout result`() = runBlocking {
        val process = FakeProcess(completes = false)
        val runner = runnerWith(commandTimeoutMillis = 25L) { process }

        val result = runner.run("blocked-command")

        assertEquals(124, result.exitCode)
        assertTrue(result.output.contains("timed out after 25 ms"))
        assertTrue(process.destroyed)
        assertFalse(process.isAlive)
    }

    @Test
    fun `process start failure is returned without throwing`() = runBlocking {
        val runner = runnerWith {
            throw IOException("su executable missing")
        }

        val result = runner.run("id")

        assertEquals(-1, result.exitCode)
        assertTrue(result.output.contains("su executable missing"))
    }

    @Test
    fun `root readiness probes id directly`() = runBlocking {
        var capturedCommand = ""
        val runner = runnerWith {
            capturedCommand = it
            FakeProcess(stdout = "uid=0(root)")
        }

        assertTrue(runner.isReady())
        assertEquals("id", capturedCommand)
    }

    @Test
    fun `root readiness rejects successful shell uid`() = runBlocking {
        val runner = runnerWith {
            FakeProcess(stdout = "uid=2000(shell) gid=2000(shell)")
        }

        assertFalse(runner.isReady())
    }

    @Test
    fun `root readiness accepts uid zero with supplementary output`() = runBlocking {
        val runner = runnerWith {
            FakeProcess(stdout = "context line\nuid=0(root) gid=0(root)")
        }

        assertTrue(runner.isReady())
    }

    private fun runnerWith(
        commandTimeoutMillis: Long = 1_000L,
        processFactory: (String) -> Process,
    ): RootShellRunner = RootShellRunner(
        commandTimeoutMillis = commandTimeoutMillis,
        rootCheckTimeoutMillis = 100L,
        processFactory = processFactory,
    )

    private class FakeProcess(
        stdout: String = "",
        stderr: String = "",
        private val exitCode: Int = 0,
        completes: Boolean = true,
    ) : Process() {
        private val standardInput = ByteArrayOutputStream()
        private val standardOutput = ByteArrayInputStream(stdout.toByteArray())
        private val standardError = ByteArrayInputStream(stderr.toByteArray())
        private val completion = CountDownLatch(if (completes) 0 else 1)

        @Volatile
        private var alive = !completes

        @Volatile
        var destroyed = false
            private set

        override fun getOutputStream(): OutputStream = standardInput

        override fun getInputStream(): InputStream = standardOutput

        override fun getErrorStream(): InputStream = standardError

        override fun waitFor(): Int {
            completion.await()
            alive = false
            return exitCode
        }

        override fun waitFor(timeout: Long, unit: TimeUnit): Boolean {
            val completed = completion.await(timeout, unit)
            if (completed) alive = false
            return completed
        }

        override fun exitValue(): Int {
            if (alive) throw IllegalThreadStateException("Process is still running")
            return exitCode
        }

        override fun destroy() {
            destroyed = true
            alive = false
            completion.countDown()
        }

        override fun destroyForcibly(): Process {
            destroy()
            return this
        }

        override fun isAlive(): Boolean = alive
    }
}
