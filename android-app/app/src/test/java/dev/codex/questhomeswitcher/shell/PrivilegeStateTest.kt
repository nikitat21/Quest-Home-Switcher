package dev.codex.questhomeswitcher.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class PrivilegeStateTest {
    @Test
    fun `root wins over every Shizuku state`() {
        assertEquals(
            PrivilegeState.ROOT,
            resolvePrivilegeState(
                rootAvailable = true,
                shizukuInstalled = false,
                binderAvailable = false,
                permissionGranted = false,
            ),
        )
    }

    @Test
    fun `missing Shizuku is reported separately`() {
        assertEquals(
            PrivilegeState.NOT_INSTALLED,
            resolvePrivilegeState(false, false, false, false),
        )
    }

    @Test
    fun `installed Shizuku without binder is offline`() {
        assertEquals(
            PrivilegeState.SERVER_OFFLINE,
            resolvePrivilegeState(false, true, false, false),
        )
    }

    @Test
    fun `connected binder without permission requests permission`() {
        assertEquals(
            PrivilegeState.PERMISSION_REQUIRED,
            resolvePrivilegeState(false, true, true, false),
        )
    }

    @Test
    fun `connected and permitted Shizuku is ready`() {
        assertEquals(
            PrivilegeState.READY,
            resolvePrivilegeState(false, true, true, true),
        )
    }
}
