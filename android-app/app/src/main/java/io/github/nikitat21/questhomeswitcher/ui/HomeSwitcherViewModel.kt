package io.github.nikitat21.questhomeswitcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.nikitat21.questhomeswitcher.data.HomeRepository
import io.github.nikitat21.questhomeswitcher.domain.ActivateHomeUseCase
import io.github.nikitat21.questhomeswitcher.domain.ActivationResult
import io.github.nikitat21.questhomeswitcher.domain.HomeEnvironment
import io.github.nikitat21.questhomeswitcher.domain.QuestHomeContract
import io.github.nikitat21.questhomeswitcher.shell.PrivilegeCoordinator
import io.github.nikitat21.questhomeswitcher.shell.PrivilegeState
import io.github.nikitat21.questhomeswitcher.shell.RootShellRunner
import io.github.nikitat21.questhomeswitcher.shell.ShizukuShellRunner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class HomeSwitcherUiState(
    val homes: List<HomeEnvironment> = emptyList(),
    val selected: HomeEnvironment? = null,
    val activeHome: HomeEnvironment? = null,
    val isBusy: Boolean = false,
    val privilegeState: PrivilegeState = PrivilegeState.CHECKING,
    val shizukuReady: Boolean = false,
    val rootReady: Boolean = false,
    val showRestartAction: Boolean = false,
    val message: String = "",
    val log: String = "",
)

class HomeSwitcherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HomeRepository(application)
    private val shellRunner = ShizukuShellRunner()
    private val rootRunner = RootShellRunner()
    private val activateHome = ActivateHomeUseCase(shellRunner, rootRunner)
    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)
    private val refreshMutex = Mutex()
    private val foregroundOperations = ForegroundOperationGate()
    private val privilegeCoordinator = PrivilegeCoordinator(
        context = application,
        rootRunner = rootRunner,
        shizukuRunner = shellRunner,
        scope = viewModelScope,
    )

    private val _uiState = MutableStateFlow(HomeSwitcherUiState())
    val uiState: StateFlow<HomeSwitcherUiState> = _uiState

    init {
        viewModelScope.launch {
            consumeRefreshRequests(
                requests = refreshRequests,
                refresh = ::refreshOnce,
                onFailure = { error ->
                    val foregroundOperationActive = foregroundOperations.isActive
                    _uiState.update {
                        it.completeRefreshFailure(error, foregroundOperationActive)
                    }
                },
            )
        }
        viewModelScope.launch {
            privilegeCoordinator.state.collect { privilegeState ->
                _uiState.update {
                    it.copy(
                        privilegeState = privilegeState,
                        shizukuReady = privilegeState == PrivilegeState.READY,
                        rootReady = privilegeState == PrivilegeState.ROOT,
                        message = privilegeState.statusMessage(),
                    )
                }
                if (privilegeState != PrivilegeState.CHECKING) {
                    refresh()
                }
            }
        }
        privilegeCoordinator.start()
    }

    override fun onCleared() {
        privilegeCoordinator.close()
        refreshRequests.close()
        super.onCleared()
    }

    fun refresh() {
        if (privilegeCoordinator.state.value == PrivilegeState.CHECKING) {
            privilegeCoordinator.requestCheck()
            return
        }
        refreshRequests.trySend(Unit)
    }

    private suspend fun refreshOnce() = refreshMutex.withLock {
        val privilegeState = privilegeCoordinator.state.value
        val ready = privilegeState == PrivilegeState.READY
        val rootReady = privilegeState == PrivilegeState.ROOT
        _uiState.update {
            it.copy(
                privilegeState = privilegeState,
                shizukuReady = ready,
                rootReady = rootReady,
                message = when {
                    rootReady -> "Scanning installed environments..."
                    ready -> "Shizuku online - scanning home APKs..."
                    else -> "Shizuku is offline. Scanning accessible folders..."
                },
            )
        }
        val fileHomes = if (ready) repository.loadHomesWithShell(shellRunner) else repository.loadHomes()
        val homes = if (rootReady) repository.loadInstalledHomes(rootRunner) + fileHomes else fileHomes
        val activeHome = when {
            rootReady -> repository.findActiveInstalledHome(rootRunner, homes)
            ready -> repository.findActiveHomeWithShell(shellRunner, homes)
            else -> null
        }
        _uiState.update {
            it.copy(
                homes = homes,
                selected = it.selected?.takeIf { selected -> homes.any { home -> home.apkPath == selected.apkPath } }
                    ?: activeHome
                    ?: homes.firstOrNull(),
                activeHome = activeHome,
                privilegeState = privilegeState,
                shizukuReady = ready,
                rootReady = rootReady,
                message = if (homes.isEmpty()) {
                    "Download a home APK or install an environment package"
                } else if (activeHome != null) {
                    "Active: ${activeHome.displayName}"
                } else {
                    "${homes.size} home(s) found - ${if (rootReady) "Root direct mode" else "Shizuku fallback"}"
                },
            )
        }
    }

    fun onAppResumed() {
        privilegeCoordinator.onAppResumed()
    }

    fun onAppPaused() {
        privilegeCoordinator.onAppPaused()
    }

    fun requestShizukuPermission() {
        when (privilegeCoordinator.state.value) {
            PrivilegeState.ROOT,
            PrivilegeState.READY -> refresh()

            PrivilegeState.PERMISSION_REQUIRED -> {
                privilegeCoordinator.requestShizukuPermission()
                _uiState.update { it.copy(message = "Approve the Shizuku permission request.") }
            }

            PrivilegeState.SERVER_OFFLINE -> {
                val opened = privilegeCoordinator.openShizukuManager(onlyOnce = false)
                _uiState.update {
                    it.copy(
                        message = if (opened) {
                            "Shizuku was opened. Start the server, then return here."
                        } else {
                            "Shizuku could not be opened. Open it from the app library."
                        },
                    )
                }
            }

            PrivilegeState.NOT_INSTALLED -> {
                _uiState.update { it.copy(message = "Shizuku is not installed.") }
            }

            PrivilegeState.CHECKING -> privilegeCoordinator.requestCheck()
        }
    }

    fun select(home: HomeEnvironment) {
        _uiState.update { it.copy(selected = home) }
    }

    fun activateSelected() {
        val home = _uiState.value.selected ?: return
        launchForegroundOperation(
            start = {
                it.copy(
                    isBusy = true,
                    showRestartAction = false,
                    message = "Installing ${home.displayName}...",
                    log = "",
                )
            },
            failureMessage = "Install failed. Open the log.",
            logHeading = "Apply Home",
        ) {
            val result = refreshMutex.withLock { activateHome(home) }
            val refreshFailure = refreshAfterActivation(result, ::refreshOnce)
            _uiState.update {
                it.completeActivation(home, result, refreshFailure)
            }
        }
    }

    fun restartQuest() {
        launchForegroundOperation(
            start = { it.copy(isBusy = true, message = "Requesting Quest restart...") },
            failureMessage = "Restart failed. Use the Quest power menu.",
            logHeading = "Restart",
        ) {
            val result = when (privilegeCoordinator.state.value) {
                PrivilegeState.ROOT -> refreshMutex.withLock { rootRunner.run("reboot") }
                else -> refreshMutex.withLock { shellRunner.run("reboot") }
            }
            _uiState.update {
                it.copy(
                    message = if (result.success) "Restart requested." else "Restart failed. Use the Quest power menu.",
                    log = if (result.output.isBlank()) it.log else it.log + "\n== Restart ==\n" + result.output,
                )
            }
        }
    }

    fun openMetaDebugSettings() {
        val runner = when (privilegeCoordinator.state.value) {
            PrivilegeState.ROOT -> rootRunner
            PrivilegeState.READY -> shellRunner
            else -> null
        }
        if (runner == null) {
            _uiState.update {
                it.copy(message = "Start Shizuku or use root before opening Meta Debug Settings.")
            }
            return
        }
        launchForegroundOperation(
            start = { it.copy(isBusy = true, message = "Opening Meta Debug Settings...") },
            failureMessage = "Meta Debug Settings could not be opened.",
            logHeading = "Open Meta Debug Settings",
        ) {
            val result = refreshMutex.withLock {
                runner.run(OPEN_META_DEBUG_SETTINGS_COMMAND)
            }
            _uiState.update {
                it.copy(
                    message = if (result.success) {
                        "Meta Debug Settings launch requested."
                    } else {
                        "Meta Debug Settings could not be opened."
                    },
                    log = if (result.success || result.output.isBlank()) {
                        it.log
                    } else {
                        it.appendLog("Open Meta Debug Settings", result.output)
                    },
                )
            }
        }
    }

    private fun launchForegroundOperation(
        start: (HomeSwitcherUiState) -> HomeSwitcherUiState,
        failureMessage: String,
        logHeading: String,
        operation: suspend () -> Unit,
    ) {
        if (!foregroundOperations.tryEnter()) return
        _uiState.update(start)
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        message = failureMessage,
                        log = it.appendLog(
                            logHeading,
                            error.message ?: error::class.java.simpleName,
                        ),
                    )
                }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
                foregroundOperations.leave()
            }
        }
    }

    fun formatSize(bytes: Long): String = repository.formatSize(bytes)

    private fun PrivilegeState.statusMessage(): String = when (this) {
        PrivilegeState.ROOT -> "Root access ready."
        PrivilegeState.CHECKING -> "Checking root and Shizuku..."
        PrivilegeState.NOT_INSTALLED -> "Shizuku is not installed."
        PrivilegeState.SERVER_OFFLINE -> "Shizuku is installed, but its server is offline."
        PrivilegeState.PERMISSION_REQUIRED -> "Shizuku permission is required."
        PrivilegeState.READY -> "Shizuku is online."
    }

    private companion object {
        const val OPEN_META_DEBUG_SETTINGS_COMMAND =
            "am broadcast --user 0 " +
                "-a com.oculus.vrshell.intent.action.LAUNCH " +
                "-n com.oculus.vrshell/.ShellControlBroadcastReceiver " +
                "--es intent_data " +
                "com.oculus.vrshell/com.oculus.panelapp.debug.ShellDebugActivity"
    }
}

internal fun HomeSwitcherUiState.canOpenMetaDebugSettings(): Boolean =
    !isBusy && (rootReady || shizukuReady)

internal class ForegroundOperationGate {
    private val mutex = Mutex()

    val isActive: Boolean
        get() = mutex.isLocked

    fun tryEnter(): Boolean = mutex.tryLock()

    fun leave() = mutex.unlock()
}

internal suspend fun consumeRefreshRequests(
    requests: ReceiveChannel<Unit>,
    refresh: suspend () -> Unit,
    onFailure: (Exception) -> Unit,
) {
    for (ignored in requests) {
        try {
            refresh()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            onFailure(error)
        }
    }
}

internal fun HomeSwitcherUiState.completeRefreshFailure(
    error: Exception,
    foregroundOperationActive: Boolean,
): HomeSwitcherUiState = copy(
    isBusy = foregroundOperationActive,
    message = "Home scan failed. Try Refresh again.",
    log = appendLog(
        heading = "Refresh Home library",
        details = error.message ?: error::class.java.simpleName,
    ),
)

private fun HomeSwitcherUiState.appendLog(heading: String, details: String): String = buildString {
    if (log.isNotBlank()) {
        appendLine(log.trimEnd())
    }
    appendLine("== $heading ==")
    append(details.trim())
}

internal suspend fun refreshAfterActivation(
    result: ActivationResult,
    refresh: suspend () -> Unit,
): Exception? {
    if (!result.success) return null
    return try {
        refresh()
        null
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        error
    }
}

internal fun HomeSwitcherUiState.completeActivation(
    home: HomeEnvironment,
    result: ActivationResult,
    refreshFailure: Exception?,
): HomeSwitcherUiState {
    val resultLog = if (refreshFailure == null) {
        result.log
    } else {
        buildString {
            if (result.log.isNotBlank()) {
                appendLine(result.log.trimEnd())
                appendLine()
            }
            appendLine("== Refresh active Home ==")
            append(refreshFailure.message ?: refreshFailure.javaClass.simpleName)
        }
    }
    return copy(
        isBusy = false,
        showRestartAction = result.needsReboot,
        message = when {
            !result.success && result.needsReboot ->
                "Install was rolled back. Restart the Quest once to reload Home."
            !result.success -> "Install failed. Open the log."
            result.needsReboot -> "Installed ${home.displayName}. Restart the Quest once to reload Home."
            refreshFailure != null -> "Applied ${home.displayName}, but the active Home status could not be refreshed."
            activeHome != null -> "Active: ${activeHome.displayName}"
            else -> "Applied ${home.displayName}, but the active Home could not be confirmed."
        },
        log = if (result.success && !result.needsReboot && refreshFailure == null) "" else resultLog,
    )
}
