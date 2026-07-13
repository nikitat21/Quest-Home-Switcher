package dev.codex.questhomeswitcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.codex.questhomeswitcher.data.HomeRepository
import dev.codex.questhomeswitcher.domain.ActivateHomeUseCase
import dev.codex.questhomeswitcher.domain.HomeEnvironment
import dev.codex.questhomeswitcher.domain.QuestHomeContract
import dev.codex.questhomeswitcher.shell.PrivilegeCoordinator
import dev.codex.questhomeswitcher.shell.PrivilegeState
import dev.codex.questhomeswitcher.shell.ShizukuShellRunner
import dev.codex.questhomeswitcher.shell.RootShellRunner
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            for (ignored in refreshRequests) {
                refreshOnce()
            }
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

    private suspend fun refreshOnce() {
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
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    showRestartAction = false,
                    message = "Installing ${home.displayName}...",
                    log = "",
                )
            }
            val result = activateHome(home)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    activeHome = if (result.success) home else it.activeHome,
                    showRestartAction = result.success && result.needsReboot,
                    message = when {
                        !result.success -> "Install failed. Open the log."
                        else -> "Active: ${home.displayName}"
                    },
                    log = if (result.success) "" else result.log,
                )
            }
        }
    }

    fun restartQuest() {
        viewModelScope.launch {
            _uiState.update { it.copy(message = "Requesting Quest restart...") }
            val result = when (privilegeCoordinator.state.value) {
                PrivilegeState.ROOT -> rootRunner.run("reboot")
                else -> shellRunner.run("reboot")
            }
            _uiState.update {
                it.copy(
                    message = if (result.success) "Restart requested." else "Restart failed. Use the Quest power menu.",
                    log = if (result.output.isBlank()) it.log else it.log + "\n== Restart ==\n" + result.output,
                )
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
}
