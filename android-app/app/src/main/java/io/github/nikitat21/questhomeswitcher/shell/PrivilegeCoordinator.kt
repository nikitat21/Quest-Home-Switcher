package io.github.nikitat21.questhomeswitcher.shell

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class PrivilegeCoordinator(
    context: Context,
    private val rootRunner: RootShellRunner,
    private val shizukuRunner: ShizukuShellRunner,
    private val scope: CoroutineScope,
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val checkRequests = Channel<Unit>(Channel.CONFLATED)
    private val forceRootCheck = AtomicBoolean(true)

    private val _state = MutableStateFlow(PrivilegeState.CHECKING)
    val state: StateFlow<PrivilegeState> = _state.asStateFlow()

    private var started = false
    private var rootAvailable: Boolean? = null
    private var fallbackJob: Job? = null
    private var automaticManagerLaunchSucceeded = false
    private var permissionRequestAttempted = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        requestCheck()
    }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        requestCheck()
    }
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, _ ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST) {
            requestCheck()
        }
    }

    fun start() {
        if (started) return
        started = true

        scope.launch {
            for (ignored in checkRequests) {
                evaluate(forceRootCheck.getAndSet(false))
            }
        }

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        requestCheck(forceRoot = true)
    }

    fun requestCheck(forceRoot: Boolean = false) {
        if (forceRoot) forceRootCheck.set(true)
        checkRequests.trySend(Unit)
    }

    fun onAppResumed() {
        requestCheck(forceRoot = true)
        fallbackJob?.cancel()
        fallbackJob = scope.launch {
            for (delayMillis in FALLBACK_RECHECK_DELAYS_MS) {
                delay(delayMillis)
                when (_state.value) {
                    PrivilegeState.ROOT,
                    PrivilegeState.READY,
                    PrivilegeState.NOT_INSTALLED,
                    PrivilegeState.PERMISSION_REQUIRED -> return@launch

                    PrivilegeState.CHECKING,
                    PrivilegeState.SERVER_OFFLINE -> requestCheck()
                }
            }

            delay(FINAL_STATE_SETTLE_DELAY_MS)
            if (_state.value == PrivilegeState.SERVER_OFFLINE) {
                openShizukuManager(onlyOnce = true)
            }
        }
    }

    fun onAppPaused() {
        fallbackJob?.cancel()
        fallbackJob = null
    }

    fun requestShizukuPermission() {
        permissionRequestAttempted = true
        scope.launch {
            shizukuRunner.requestPermissionIfNeeded(SHIZUKU_PERMISSION_REQUEST)
            requestCheck()
        }
    }

    fun openShizukuManager(onlyOnce: Boolean): Boolean {
        if (onlyOnce && automaticManagerLaunchSucceeded) return false
        if (!isShizukuInstalled()) return false

        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            ?: Intent().setClassName(SHIZUKU_PACKAGE, SHIZUKU_MAIN_ACTIVITY)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val startedSuccessfully = runCatching {
            appContext.startActivity(launchIntent)
            true
        }.onFailure {
            Log.w(TAG, "Could not open Shizuku Manager", it)
        }.getOrDefault(false)

        if (startedSuccessfully) {
            automaticManagerLaunchSucceeded = true
        }
        return startedSuccessfully
    }

    override fun close() {
        fallbackJob?.cancel()
        fallbackJob = null
        if (started) {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        }
        checkRequests.close()
        started = false
    }

    private suspend fun evaluate(forceRoot: Boolean) {
        if (forceRoot || rootAvailable == null) {
            _state.value = PrivilegeState.CHECKING
            rootAvailable = rootRunner.isReady()
        }

        if (rootAvailable == true) {
            _state.value = PrivilegeState.ROOT
            return
        }

        val shizukuInstalled = isShizukuInstalled()
        val binderAvailable = shizukuInstalled && shizukuRunner.isBinderAvailable()
        val resolved = resolvePrivilegeState(
            rootAvailable = false,
            shizukuInstalled = shizukuInstalled,
            binderAvailable = binderAvailable,
            permissionGranted = shizukuRunner.isPermissionGranted(binderAvailable),
        )
        _state.value = resolved

        if (resolved == PrivilegeState.PERMISSION_REQUIRED && !permissionRequestAttempted) {
            permissionRequestAttempted = true
            shizukuRunner.requestPermissionIfNeeded(SHIZUKU_PERMISSION_REQUEST)
        }
    }

    @Suppress("DEPRECATION")
    private fun isShizukuInstalled(): Boolean = try {
        appContext.packageManager.getApplicationInfo(SHIZUKU_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private companion object {
        private const val TAG = "QHS-Privilege"
        private const val SHIZUKU_PERMISSION_REQUEST = 42
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val SHIZUKU_MAIN_ACTIVITY = "moe.shizuku.manager.MainActivity"
        private const val FINAL_STATE_SETTLE_DELAY_MS = 250L
        private val FALLBACK_RECHECK_DELAYS_MS = longArrayOf(500L, 1_000L, 2_000L, 3_000L)
    }
}
