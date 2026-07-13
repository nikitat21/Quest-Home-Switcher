package io.github.nikitat21.questhomeswitcher.domain

data class HomeEnvironment(
    val displayName: String,
    val apkPath: String,
    val previewPath: String?,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val packageName: String? = null,
    val sceneUri: String? = null,
    val installed: Boolean = false,
    val type: HomeEnvironmentType = HomeEnvironmentType.ENVIRONMENT,
    val verifiedHomeApk: Boolean = false,
    val sceneHash: String? = null,
)

enum class HomeEnvironmentType {
    ENVIRONMENT,
    VISTA,
    FOOTPRINT,
}

data class ActivationResult(
    val success: Boolean,
    val needsReboot: Boolean,
    val log: String,
)

object QuestHomeContract {
    const val TargetPackage = "com.meta.shell.env.footprint.haven2025"
    const val HomesFolderName = "Quest Homes"
    val SearchFolders = listOf("Quest Homes", "QuestHomes", "Homes", "Download")
}
