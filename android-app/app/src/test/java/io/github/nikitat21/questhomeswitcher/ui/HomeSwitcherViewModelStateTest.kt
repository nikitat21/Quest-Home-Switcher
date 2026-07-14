package io.github.nikitat21.questhomeswitcher.ui

import io.github.nikitat21.questhomeswitcher.domain.ActivationResult
import io.github.nikitat21.questhomeswitcher.domain.HomeEnvironment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSwitcherViewModelStateTest {
    @Test
    fun debugSettingsAreEnabledForRoot() {
        assertTrue(HomeSwitcherUiState(rootReady = true).canOpenMetaDebugSettings())
    }

    @Test
    fun debugSettingsAreEnabledForReadyShizuku() {
        assertTrue(HomeSwitcherUiState(shizukuReady = true).canOpenMetaDebugSettings())
    }

    @Test
    fun debugSettingsStayDisabledWithoutPrivilegedAccess() {
        assertFalse(HomeSwitcherUiState().canOpenMetaDebugSettings())
    }

    @Test
    fun debugSettingsStayDisabledDuringAnotherOperation() {
        assertFalse(
            HomeSwitcherUiState(
                isBusy = true,
                rootReady = true,
                shizukuReady = true,
            ).canOpenMetaDebugSettings(),
        )
    }

    @Test
    fun foregroundGateRejectsASecondOperationUntilTheFirstLeaves() {
        val gate = ForegroundOperationGate()

        assertTrue(gate.tryEnter())
        assertTrue(gate.isActive)
        assertFalse(gate.tryEnter())

        gate.leave()

        assertFalse(gate.isActive)
        assertTrue(gate.tryEnter())
        gate.leave()
    }

    @Test
    fun refreshWorkerContinuesAfterANonCancellationFailure() = runBlocking {
        val requests = Channel<Unit>(Channel.UNLIMITED)
        var refreshAttempts = 0
        val failures = mutableListOf<Exception>()
        requests.send(Unit)
        requests.send(Unit)
        requests.close()

        consumeRefreshRequests(
            requests = requests,
            refresh = {
                refreshAttempts += 1
                if (refreshAttempts == 1) error("first scan failed")
            },
            onFailure = failures::add,
        )

        assertEquals(2, refreshAttempts)
        assertEquals(1, failures.size)
        assertEquals("first scan failed", failures.single().message)
    }

    @Test
    fun refreshWorkerPropagatesCancellation() = runBlocking {
        val requests = Channel<Unit>(Channel.UNLIMITED)
        var failureCallbacks = 0
        requests.send(Unit)
        requests.close()

        var cancellation: CancellationException? = null
        try {
            consumeRefreshRequests(
                requests = requests,
                refresh = { throw CancellationException("stop") },
                onFailure = { failureCallbacks += 1 },
            )
        } catch (error: CancellationException) {
            cancellation = error
        }

        assertEquals("stop", cancellation?.message)
        assertEquals(0, failureCallbacks)
    }

    @Test
    fun refreshFailureRestoresIdleStateAndPreservesTheLog() {
        val state = HomeSwitcherUiState(
            isBusy = true,
            log = "Previous details",
        ).completeRefreshFailure(
            error = IllegalStateException("scan failed"),
            foregroundOperationActive = false,
        )

        assertFalse(state.isBusy)
        assertTrue(state.message.contains("Try Refresh again"))
        assertTrue(state.log.contains("Previous details"))
        assertTrue(state.log.contains("scan failed"))
    }

    @Test
    fun refreshFailureDoesNotClearBusyWhileForegroundOperationIsActive() {
        val state = HomeSwitcherUiState(isBusy = true).completeRefreshFailure(
            error = IllegalStateException("scan failed"),
            foregroundOperationActive = true,
        )

        assertTrue(state.isBusy)
    }

    @Test
    fun refreshFailureStillFinalizesSuccessfulActivation() = runBlocking {
        val result = ActivationResult(
            success = true,
            needsReboot = false,
            log = "Home was applied.",
        )
        val expectedFailure = IllegalStateException("scan failed")

        val refreshFailure = refreshAfterActivation(result) { throw expectedFailure }
        val state = HomeSwitcherUiState(isBusy = true).completeActivation(home(), result, refreshFailure)

        assertSame(expectedFailure, refreshFailure)
        assertFalse(state.isBusy)
        assertFalse(state.showRestartAction)
        assertTrue(state.message.contains("could not be refreshed"))
        assertTrue(state.log.contains("scan failed"))
    }

    @Test
    fun failedActivationCanStillExposeRequiredRestart() {
        val result = ActivationResult(
            success = false,
            needsReboot = true,
            log = "The previous Home was restored.",
        )

        val state = HomeSwitcherUiState(isBusy = true).completeActivation(home(), result, refreshFailure = null)

        assertFalse(state.isBusy)
        assertTrue(state.showRestartAction)
        assertTrue(state.message.contains("Restart the Quest once"))
        assertTrue(state.log.contains("previous Home was restored"))
    }

    private fun home() = HomeEnvironment(
        displayName = "Test Home",
        apkPath = "/sdcard/Download/Test Home.apk",
        previewPath = null,
        sizeBytes = 1234L,
        lastModifiedMillis = 1L,
    )
}
