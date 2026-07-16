package io.github.nikitat21.questhomeswitcher.shell.localadb

/** The two TLS services published by Android's Wireless debugging feature. */
enum class LocalAdbServiceKind(val dnsSdType: String) {
    PAIRING("_adb-tls-pairing._tcp"),
    CONNECTION("_adb-tls-connect._tcp"),
}

/**
 * An endpoint returned by local DNS-SD discovery.
 *
 * This type validates syntax only. A discovery implementation must additionally prove that the
 * resolved address belongs to this device (loopback or one of its active interfaces) before
 * constructing a connection.
 */
data class LocalAdbEndpoint(
    val host: String,
    val port: Int,
) {
    init {
        require(host.isNotBlank()) { "ADB endpoint host must not be blank" }
        require(host == host.trim()) { "ADB endpoint host must not contain surrounding whitespace" }
        require(host.length <= MAX_HOST_LENGTH) { "ADB endpoint host is too long" }
        require(host.none { it.isWhitespace() || it == '\u0000' || it == '/' }) {
            "ADB endpoint host contains an invalid character"
        }
        require(port in 1..MAX_PORT) { "ADB endpoint port must be in 1..65535" }
    }

    private companion object {
        const val MAX_HOST_LENGTH = 255
        const val MAX_PORT = 65_535
    }
}

/** A six-digit Wireless debugging pairing code whose string form is always redacted. */
class LocalAdbPairingCode private constructor(
    digits: ByteArray,
) : AutoCloseable {
    private var digits: ByteArray? = digits.copyOf()

    @Synchronized
    internal fun copyUtf8(): ByteArray =
        checkNotNull(digits) { "Pairing code has been destroyed" }.copyOf()

    @Synchronized
    override fun close() {
        digits?.fill(0)
        digits = null
    }

    override fun toString(): String = "LocalAdbPairingCode([REDACTED])"

    companion object {
        private const val LENGTH = 6

        fun parse(input: CharSequence): LocalAdbPairingCode? {
            if (input.length != LENGTH || input.any { it !in '0'..'9' }) return null
            val encoded = ByteArray(LENGTH) { index -> input[index].code.toByte() }
            return try {
                LocalAdbPairingCode(encoded)
            } finally {
                encoded.fill(0)
            }
        }
    }
}

/** An identity that is impossible to construct for root or an application UID. */
data class LocalAdbShellIdentity(val uid: Int) {
    init {
        require(uid == SHELL_UID) { "A local ADB session must run as Android shell (uid=2000)" }
    }

    companion object {
        const val SHELL_UID = 2_000
    }
}

enum class LocalAdbBridgePhase {
    DISABLED,
    IDLE,
    DISCOVERING,
    AWAITING_PAIRING_CODE,
    PAIRING,
    CONNECTING,
    VALIDATING_IDENTITY,
    READY,
    FAILED,
}

enum class LocalAdbFailureStage {
    STORAGE,
    DISCOVERY,
    PAIRING,
    CONNECTION,
    IDENTITY_VALIDATION,
}

enum class LocalAdbFailureReason {
    KEY_UNAVAILABLE,
    SERVICE_NOT_FOUND,
    TIMEOUT,
    PAIRING_REJECTED,
    CONNECTION_REJECTED,
    TLS_FAILURE,
    PROTOCOL_FAILURE,
    INVALID_SHELL_IDENTITY,
}

data class LocalAdbFailure(
    val stage: LocalAdbFailureStage,
    val reason: LocalAdbFailureReason,
    val retryable: Boolean,
)

/**
 * State owned by the future built-in bridge coordinator.
 *
 * Nothing in the current UI or privilege coordinator consumes this model yet. That is deliberate:
 * phase 0 must not expose an unfinished privilege path.
 */
sealed interface LocalAdbBridgeState {
    val phase: LocalAdbBridgePhase

    data object Disabled : LocalAdbBridgeState {
        override val phase = LocalAdbBridgePhase.DISABLED
    }

    data object Idle : LocalAdbBridgeState {
        override val phase = LocalAdbBridgePhase.IDLE
    }

    data class Discovering(val service: LocalAdbServiceKind) : LocalAdbBridgeState {
        override val phase = LocalAdbBridgePhase.DISCOVERING
    }

    data class AwaitingPairingCode(val endpoint: LocalAdbEndpoint) : LocalAdbBridgeState {
        override val phase = LocalAdbBridgePhase.AWAITING_PAIRING_CODE
    }

    data class Pairing(val endpoint: LocalAdbEndpoint) : LocalAdbBridgeState {
        override val phase = LocalAdbBridgePhase.PAIRING
    }

    data class Connecting(val endpoint: LocalAdbEndpoint) : LocalAdbBridgeState {
        override val phase = LocalAdbBridgePhase.CONNECTING
    }

    data class ValidatingIdentity(val endpoint: LocalAdbEndpoint) : LocalAdbBridgeState {
        override val phase = LocalAdbBridgePhase.VALIDATING_IDENTITY
    }

    data class Ready(
        val endpoint: LocalAdbEndpoint,
        val identity: LocalAdbShellIdentity,
    ) : LocalAdbBridgeState {
        override val phase = LocalAdbBridgePhase.READY
    }

    data class Failed(val failure: LocalAdbFailure) : LocalAdbBridgeState {
        override val phase = LocalAdbBridgePhase.FAILED
    }
}

/** Fail-closed transition validation for the future coordinator. */
object LocalAdbBridgeTransitions {
    fun isAllowed(from: LocalAdbBridgeState, to: LocalAdbBridgeState): Boolean = when (from) {
        LocalAdbBridgeState.Disabled -> to === LocalAdbBridgeState.Idle

        LocalAdbBridgeState.Idle ->
            to === LocalAdbBridgeState.Disabled ||
                to is LocalAdbBridgeState.Discovering ||
                to.isFailureAt(LocalAdbFailureStage.STORAGE)

        is LocalAdbBridgeState.Discovering -> when (from.service) {
            LocalAdbServiceKind.PAIRING ->
                to is LocalAdbBridgeState.AwaitingPairingCode ||
                    to.isFailureAt(LocalAdbFailureStage.DISCOVERY) ||
                    to.isAbortState()

            LocalAdbServiceKind.CONNECTION ->
                to is LocalAdbBridgeState.Connecting ||
                    to.isFailureAt(LocalAdbFailureStage.DISCOVERY) ||
                    to.isAbortState()
        }

        is LocalAdbBridgeState.AwaitingPairingCode ->
            to is LocalAdbBridgeState.Pairing ||
                to.isFailureAt(LocalAdbFailureStage.PAIRING) ||
                to.isAbortState()

        is LocalAdbBridgeState.Pairing ->
            (to is LocalAdbBridgeState.Discovering &&
                to.service == LocalAdbServiceKind.CONNECTION) ||
                to.isFailureAt(LocalAdbFailureStage.PAIRING) ||
                to.isAbortState()

        is LocalAdbBridgeState.Connecting ->
            to is LocalAdbBridgeState.ValidatingIdentity ||
                to.isFailureAt(LocalAdbFailureStage.CONNECTION) ||
                to.isAbortState()

        is LocalAdbBridgeState.ValidatingIdentity ->
            to is LocalAdbBridgeState.Ready ||
                to.isFailureAt(LocalAdbFailureStage.IDENTITY_VALIDATION) ||
                to.isAbortState()

        is LocalAdbBridgeState.Ready ->
            to.isFailureAt(LocalAdbFailureStage.CONNECTION) || to.isAbortState()

        is LocalAdbBridgeState.Failed -> to.isAbortState()
    }

    fun requireAllowed(from: LocalAdbBridgeState, to: LocalAdbBridgeState) {
        require(isAllowed(from, to)) {
            "Invalid local ADB bridge transition: ${from.phase} -> ${to.phase}"
        }
    }

    private fun LocalAdbBridgeState.isAbortState(): Boolean =
        this === LocalAdbBridgeState.Idle || this === LocalAdbBridgeState.Disabled

    private fun LocalAdbBridgeState.isFailureAt(stage: LocalAdbFailureStage): Boolean =
        this is LocalAdbBridgeState.Failed && failure.stage == stage
}
