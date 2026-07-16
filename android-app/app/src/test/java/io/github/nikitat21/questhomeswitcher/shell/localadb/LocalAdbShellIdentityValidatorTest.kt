package io.github.nikitat21.questhomeswitcher.shell.localadb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAdbShellIdentityValidatorTest {
    @Test
    fun `accepts the Android shell identity`() {
        val result = LocalAdbShellIdentityValidator.validate(
            "uid=2000(shell) gid=2000(shell) groups=1003(graphics),1004(input) context=u:r:shell:s0",
        )

        assertTrue(result is LocalAdbIdentityValidation.Valid)
        assertEquals(
            LocalAdbShellIdentity.SHELL_UID,
            (result as LocalAdbIdentityValidation.Valid).identity.uid,
        )
    }

    @Test
    fun `accepts uid 2000 when id omits the account name`() {
        val result = LocalAdbShellIdentityValidator.validate("uid=2000 gid=2000")

        assertTrue(result is LocalAdbIdentityValidation.Valid)
    }

    @Test
    fun `accepts a shell uid token at end of output`() {
        val result = LocalAdbShellIdentityValidator.validate("uid=2000(shell)")

        assertTrue(result is LocalAdbIdentityValidation.Valid)
    }

    @Test
    fun `rejects root`() {
        val result = LocalAdbShellIdentityValidator.validate("uid=0(root) gid=0(root)")

        assertRejected(result, LocalAdbIdentityRejection.UID_NOT_SHELL, observedUid = 0)
    }

    @Test
    fun `rejects an application uid`() {
        val result = LocalAdbShellIdentityValidator.validate("uid=10123(u0_a123) gid=10123(u0_a123)")

        assertRejected(result, LocalAdbIdentityRejection.UID_NOT_SHELL, observedUid = 10_123)
    }

    @Test
    fun `rejects missing and malformed uid fields`() {
        assertRejected(
            LocalAdbShellIdentityValidator.validate("gid=2000(shell)"),
            LocalAdbIdentityRejection.UID_MISSING,
        )
        assertRejected(
            LocalAdbShellIdentityValidator.validate("uid=shell gid=2000(shell)"),
            LocalAdbIdentityRejection.UID_MALFORMED,
        )
    }

    @Test
    fun `rejects ambiguous output instead of accepting a later shell uid`() {
        val result = LocalAdbShellIdentityValidator.validate(
            "uid=0(root) gid=0(root)\nuid=2000(shell) gid=2000(shell)",
        )

        assertRejected(result, LocalAdbIdentityRejection.UID_AMBIGUOUS)
    }

    @Test
    fun `rejects a mismatched account name for uid 2000`() {
        val result = LocalAdbShellIdentityValidator.validate("uid=2000(root) gid=2000(shell)")

        assertRejected(
            result,
            LocalAdbIdentityRejection.ACCOUNT_NAME_MISMATCH,
            observedUid = 2_000,
        )
    }

    private fun assertRejected(
        result: LocalAdbIdentityValidation,
        reason: LocalAdbIdentityRejection,
        observedUid: Int? = null,
    ) {
        assertTrue(result is LocalAdbIdentityValidation.Rejected)
        result as LocalAdbIdentityValidation.Rejected
        assertEquals(reason, result.reason)
        assertEquals(observedUid, result.observedUid)
    }
}
