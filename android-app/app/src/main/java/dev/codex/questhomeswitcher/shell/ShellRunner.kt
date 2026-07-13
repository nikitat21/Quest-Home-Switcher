package dev.codex.questhomeswitcher.shell

interface ShellRunner {
    suspend fun isReady(): Boolean
    suspend fun requestPermissionIfNeeded(requestCode: Int)
    suspend fun run(command: String): ShellResult
}

data class ShellResult(
    val exitCode: Int,
    val output: String,
) {
    val success: Boolean get() = exitCode == 0
}
