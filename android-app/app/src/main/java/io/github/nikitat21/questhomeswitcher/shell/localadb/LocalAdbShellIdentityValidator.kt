package io.github.nikitat21.questhomeswitcher.shell.localadb

enum class LocalAdbIdentityRejection {
    EMPTY_OUTPUT,
    UID_MISSING,
    UID_MALFORMED,
    UID_AMBIGUOUS,
    UID_NOT_SHELL,
    ACCOUNT_NAME_MISMATCH,
}

sealed interface LocalAdbIdentityValidation {
    data class Valid(val identity: LocalAdbShellIdentity) : LocalAdbIdentityValidation

    data class Rejected(
        val reason: LocalAdbIdentityRejection,
        val observedUid: Int? = null,
    ) : LocalAdbIdentityValidation
}

/** Strict parser for the output of the fixed `id` identity probe. */
object LocalAdbShellIdentityValidator {
    private val uidMarker = Regex("(?<![A-Za-z0-9_])uid=")
    private val uidToken = Regex(
        "(?<![A-Za-z0-9_])uid=([0-9]+)(?:\\(([A-Za-z0-9_.-]+)\\))?(?=\\z|[\\t\\r\\n ])",
    )

    fun validate(idOutput: String): LocalAdbIdentityValidation {
        if (idOutput.isBlank()) {
            return LocalAdbIdentityValidation.Rejected(LocalAdbIdentityRejection.EMPTY_OUTPUT)
        }
        if ('\u0000' in idOutput) {
            return LocalAdbIdentityValidation.Rejected(LocalAdbIdentityRejection.UID_MALFORMED)
        }

        val markerCount = uidMarker.findAll(idOutput).count()
        if (markerCount == 0) {
            return LocalAdbIdentityValidation.Rejected(LocalAdbIdentityRejection.UID_MISSING)
        }

        val matches = uidToken.findAll(idOutput).toList()
        if (matches.size != markerCount) {
            return LocalAdbIdentityValidation.Rejected(LocalAdbIdentityRejection.UID_MALFORMED)
        }
        if (matches.size != 1) {
            return LocalAdbIdentityValidation.Rejected(LocalAdbIdentityRejection.UID_AMBIGUOUS)
        }

        val match = matches.single()
        val uid = match.groupValues[1].toIntOrNull()
            ?: return LocalAdbIdentityValidation.Rejected(LocalAdbIdentityRejection.UID_MALFORMED)
        if (uid != LocalAdbShellIdentity.SHELL_UID) {
            return LocalAdbIdentityValidation.Rejected(
                reason = LocalAdbIdentityRejection.UID_NOT_SHELL,
                observedUid = uid,
            )
        }

        val accountName = match.groupValues[2].ifEmpty { null }
        if (accountName != null && accountName != SHELL_ACCOUNT_NAME) {
            return LocalAdbIdentityValidation.Rejected(
                reason = LocalAdbIdentityRejection.ACCOUNT_NAME_MISMATCH,
                observedUid = uid,
            )
        }

        return LocalAdbIdentityValidation.Valid(LocalAdbShellIdentity(uid))
    }

    private const val SHELL_ACCOUNT_NAME = "shell"
}
