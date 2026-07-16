package io.github.nikitat21.questhomeswitcher.shell.localadb

/** DNS-SD discovery restricted to Android's two TLS ADB service types. */
interface LocalAdbServiceDiscovery : AutoCloseable {
    suspend fun discover(service: LocalAdbServiceKind): LocalAdbDiscoveryResult
}

sealed interface LocalAdbDiscoveryResult {
    data class Found(val endpoint: LocalAdbEndpoint) : LocalAdbDiscoveryResult

    data class Failed(val reason: LocalAdbFailureReason) : LocalAdbDiscoveryResult
}

/** One-shot pairing. Implementations receive their host identity through dependency injection. */
interface LocalAdbPairingClient {
    suspend fun pair(
        endpoint: LocalAdbEndpoint,
        code: LocalAdbPairingCode,
    ): LocalAdbPairingResult
}

sealed interface LocalAdbPairingResult {
    data object Paired : LocalAdbPairingResult

    data class Failed(val reason: LocalAdbFailureReason) : LocalAdbPairingResult
}

/** Establishes an authenticated TLS connection to a previously paired local adbd. */
interface LocalAdbConnector {
    suspend fun connect(endpoint: LocalAdbEndpoint): LocalAdbConnectionResult
}

sealed interface LocalAdbConnectionResult {
    data class Connected(val connection: LocalAdbConnection) : LocalAdbConnectionResult

    data class Failed(val reason: LocalAdbFailureReason) : LocalAdbConnectionResult
}

/**
 * Deliberately narrow connection surface.
 *
 * There is no `execute(String)` method. Phase 0 permits only the identity probe needed before a
 * verified transaction can exist.
 */
interface LocalAdbConnection : AutoCloseable {
    val endpoint: LocalAdbEndpoint

    suspend fun validateShellIdentity(): LocalAdbIdentityValidation
}

/** Metadata visible inside a verified transaction; it grants no raw shell capability. */
interface VerifiedLocalAdbSession {
    val endpoint: LocalAdbEndpoint
    val identity: LocalAdbShellIdentity
}

fun interface VerifiedLocalAdbOperation<T> {
    suspend fun run(session: VerifiedLocalAdbSession): T
}

/**
 * Lifecycle and serialization boundary for future typed home operations.
 *
 * Implementations must open/connect, validate uid=2000, run exactly one typed operation, then close
 * in `finally`. They must never return their transport or a generic command executor.
 */
interface LocalAdbTransactionBoundary {
    suspend fun <T> runVerified(operation: VerifiedLocalAdbOperation<T>): T
}
