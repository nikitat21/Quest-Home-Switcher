package dev.codex.questhomeswitcher.shell

enum class PrivilegeState {
    ROOT,
    CHECKING,
    NOT_INSTALLED,
    SERVER_OFFLINE,
    PERMISSION_REQUIRED,
    READY,
}

internal fun resolvePrivilegeState(
    rootAvailable: Boolean,
    shizukuInstalled: Boolean,
    binderAvailable: Boolean,
    permissionGranted: Boolean,
): PrivilegeState = when {
    rootAvailable -> PrivilegeState.ROOT
    !shizukuInstalled -> PrivilegeState.NOT_INSTALLED
    !binderAvailable -> PrivilegeState.SERVER_OFFLINE
    !permissionGranted -> PrivilegeState.PERMISSION_REQUIRED
    else -> PrivilegeState.READY
}
