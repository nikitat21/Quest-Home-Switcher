package io.github.nikitat21.questhomeswitcher.shell.localadb

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAdbBridgeTransitionsTest {
    private val endpoint = LocalAdbEndpoint("127.0.0.1", 37_123)

    @Test
    fun `pairing and connection happy path is accepted`() {
        val states = listOf(
            LocalAdbBridgeState.Disabled,
            LocalAdbBridgeState.Idle,
            LocalAdbBridgeState.Discovering(LocalAdbServiceKind.PAIRING),
            LocalAdbBridgeState.AwaitingPairingCode(endpoint),
            LocalAdbBridgeState.Pairing(endpoint),
            LocalAdbBridgeState.Discovering(LocalAdbServiceKind.CONNECTION),
            LocalAdbBridgeState.Connecting(endpoint),
            LocalAdbBridgeState.ValidatingIdentity(endpoint),
            LocalAdbBridgeState.Ready(endpoint, LocalAdbShellIdentity(2_000)),
        )

        states.zipWithNext().forEach { (from, to) ->
            assertTrue("${from.phase} -> ${to.phase}", LocalAdbBridgeTransitions.isAllowed(from, to))
        }
    }

    @Test
    fun `ready cannot be reached before identity validation`() {
        val ready = LocalAdbBridgeState.Ready(endpoint, LocalAdbShellIdentity(2_000))

        assertFalse(LocalAdbBridgeTransitions.isAllowed(LocalAdbBridgeState.Idle, ready))
        assertFalse(
            LocalAdbBridgeTransitions.isAllowed(LocalAdbBridgeState.Connecting(endpoint), ready),
        )
    }

    @Test
    fun `failure stage must match the active phase`() {
        val pairingFailure = LocalAdbBridgeState.Failed(
            LocalAdbFailure(
                stage = LocalAdbFailureStage.PAIRING,
                reason = LocalAdbFailureReason.PAIRING_REJECTED,
                retryable = true,
            ),
        )

        assertTrue(
            LocalAdbBridgeTransitions.isAllowed(LocalAdbBridgeState.Pairing(endpoint), pairingFailure),
        )
        assertFalse(
            LocalAdbBridgeTransitions.isAllowed(
                LocalAdbBridgeState.ValidatingIdentity(endpoint),
                pairingFailure,
            ),
        )
    }

    @Test
    fun `failed state must return to idle before retry`() {
        val failed = LocalAdbBridgeState.Failed(
            LocalAdbFailure(
                stage = LocalAdbFailureStage.DISCOVERY,
                reason = LocalAdbFailureReason.SERVICE_NOT_FOUND,
                retryable = true,
            ),
        )

        assertTrue(LocalAdbBridgeTransitions.isAllowed(failed, LocalAdbBridgeState.Idle))
        assertFalse(
            LocalAdbBridgeTransitions.isAllowed(
                failed,
                LocalAdbBridgeState.Discovering(LocalAdbServiceKind.CONNECTION),
            ),
        )
    }

    @Test
    fun `pairing code is redacted and destroyable`() {
        val code = LocalAdbPairingCode.parse("123456")
            ?: error("A six-digit pairing code must parse")
        assertEquals("123456", code.copyUtf8().decodeToString())
        assertFalse(code.toString().contains("123456"))

        code.close()
        val destroyed = runCatching { code.copyUtf8() }.isFailure
        assertTrue(destroyed)
    }
}
