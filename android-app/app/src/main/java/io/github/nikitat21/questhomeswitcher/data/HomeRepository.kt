package io.github.nikitat21.questhomeswitcher.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import io.github.nikitat21.questhomeswitcher.domain.HomeEnvironment
import io.github.nikitat21.questhomeswitcher.domain.HomeEnvironmentSource
import io.github.nikitat21.questhomeswitcher.domain.HomeEnvironmentType
import io.github.nikitat21.questhomeswitcher.domain.QuestHomeContract
import io.github.nikitat21.questhomeswitcher.shell.ShellRunner
import java.io.File
import java.security.MessageDigest
import java.text.DecimalFormat
import java.util.zip.ZipFile

class HomeRepository(context: Context) {
    private val packageManager = context.packageManager
    private val metadataCache = context.applicationContext.getSharedPreferences(
        "home_metadata_cache_v2",
        Context.MODE_PRIVATE,
    )
    private val previewExtensions = listOf("png", "jpg", "jpeg", "webp")

    fun homesDirectory(): File {
        return File(Environment.getExternalStorageDirectory(), QuestHomeContract.HomesFolderName)
    }

    fun loadHomes(): List<HomeEnvironment> {
        return QuestHomeContract.SearchFolders
            .map { File(Environment.getExternalStorageDirectory(), it) }
            .filter { it.exists() && it.isDirectory }
            .flatMap { dir -> dir.walkTopDown().maxDepth(3).filter { it.isFile }.toList() }
            .filter { it.extension.equals("apk", ignoreCase = true) }
            .distinctBy { it.absolutePath }
            .mapNotNull { apk ->
                val sceneHash = sceneHash(apk) ?: return@mapNotNull null
                val packageName = archivePackageName(apk.absolutePath)
                val official = OfficialHomeCatalog.identify(packageName, sceneHash)
                HomeEnvironment(
                    displayName = official?.displayName ?: apk.nameWithoutExtension.cleanHomeName(),
                    apkPath = apk.absolutePath,
                    previewPath = findPreviewFor(apk)?.absolutePath,
                    sizeBytes = apk.length(),
                    lastModifiedMillis = apk.lastModified(),
                    packageName = packageName,
                    source = homeSourceForPath(apk.absolutePath),
                    officialHomeId = official?.id,
                    verifiedHomeApk = true,
                    sceneHash = sceneHash,
                    knownSceneHashes = official?.sceneHashes ?: setOf(sceneHash.lowercase()),
                )
            }
            .let(::mergeHomeCandidates)
    }

    suspend fun loadHomesWithShell(shellRunner: ShellRunner): List<HomeEnvironment> {
        val dirs = QuestHomeContract.SearchFolders.joinToString(" ") { "/sdcard/${it.shellQuote()}" }
        val listCommand = """
            for dir in $dirs; do
            [ -d "${'$'}dir" ] || continue
            find "${'$'}dir" -maxdepth 3 -type f \( -iname '*.apk' \) 2>/dev/null
            done | while IFS= read -r f; do
              [ -f "${'$'}f" ] || continue
              base="${'$'}{f%.*}"
              preview=""
              for ext in png jpg jpeg webp PNG JPG JPEG WEBP; do
                [ -f "${'$'}base.${'$'}ext" ] && preview="${'$'}base.${'$'}ext" && break
              done
              size=${'$'}(wc -c < "${'$'}f" | tr -d ' ')
              mod=${'$'}(stat -c %Y "${'$'}f" 2>/dev/null || echo 0)
              printf '%s\t%s\t%s\t%s\n' "${'$'}f" "${'$'}size" "${'$'}mod" "${'$'}preview"
            done
        """.trimIndent()

        val listResult = shellRunner.run(listCommand)
        if (!listResult.success) return emptyList()

        val files = listResult.output.lineSequence().mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size < 3) return@mapNotNull null
            ShellHomeFile(
                path = parts[0],
                sizeBytes = parts[1].toLongOrNull() ?: return@mapNotNull null,
                modifiedSeconds = parts[2].toLongOrNull() ?: 0L,
                previewPath = parts.getOrNull(3)?.takeIf { it.isNotBlank() },
            )
        }.distinctBy { it.path }.toList()

        val metadata = files.associateWith { file -> readCachedMetadata(file) }.toMutableMap()
        val changedFiles = files.filter { metadata[it] == null }
        if (changedFiles.isNotEmpty()) {
            val candidates = changedFiles.joinToString(" ") { it.path.shellQuote() }
            val inspectCommand = """
                for f in $candidates; do
                  [ -f "${'$'}f" ] || continue
                  if unzip -l "${'$'}f" 2>/dev/null | grep -qE '[[:space:]]assets/scene\.zip${'$'}'; then
                    scene_hash=${'$'}(unzip -p "${'$'}f" assets/scene.zip 2>/dev/null | sha256sum | cut -d ' ' -f 1)
                    printf '%s\t1\t%s\n' "${'$'}f" "${'$'}scene_hash"
                  else
                    printf '%s\t0\t\n' "${'$'}f"
                  fi
                done
            """.trimIndent()
            val inspectResult = shellRunner.run(inspectCommand)
            val inspectedByPath = inspectResult.output.lineSequence().mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size < 2) null else parts[0] to CachedHomeMetadata(
                    verified = parts[1] == "1",
                    sceneHash = parts.getOrNull(2)?.takeIf { it.isNotBlank() },
                )
            }.toMap()
            changedFiles.forEach { file ->
                val inspected = inspectedByPath[file.path] ?: CachedHomeMetadata(false, null)
                metadata[file] = inspected
                writeCachedMetadata(file, inspected)
            }
        }

        return files
            .mapNotNull { file ->
                val cached = metadata[file] ?: return@mapNotNull null
                if (!cached.verified || cached.sceneHash.isNullOrBlank()) return@mapNotNull null
                val apkPath = file.path
                val fileName = apkPath.substringAfterLast('/')
                val official = OfficialHomeCatalog.identify(
                    packageName = null,
                    sceneHash = cached.sceneHash,
                )
                HomeEnvironment(
                    displayName = official?.displayName
                        ?: fileName.substringBeforeLast('.').cleanHomeName(),
                    apkPath = apkPath,
                    previewPath = file.previewPath,
                    sizeBytes = file.sizeBytes,
                    lastModifiedMillis = file.modifiedSeconds * 1000L,
                    source = homeSourceForPath(apkPath),
                    officialHomeId = official?.id,
                    verifiedHomeApk = true,
                    sceneHash = cached.sceneHash,
                    knownSceneHashes = official?.sceneHashes ?: setOf(cached.sceneHash.lowercase()),
                )
            }
            .let(::mergeHomeCandidates)
    }

    suspend fun loadInstalledHomes(rootRunner: ShellRunner): List<HomeEnvironment> {
        val command = installedHomeLookupCommand()
        val result = rootRunner.run(command)
        if (!result.success) return emptyList()
        return result.output.lineSequence().mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size < 3) return@mapNotNull null
            val pkg = parts[0]
            val sceneHash = parts.getOrNull(3)
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.matches(EXACT_SCENE_HASH) }
            val official = OfficialHomeCatalog.identify(packageName = pkg, sceneHash = sceneHash)
            HomeEnvironment(
                displayName = official?.displayName ?: pkg.substringAfterLast('.').cleanHomeName(),
                apkPath = parts[1],
                previewPath = null,
                sizeBytes = 0L,
                lastModifiedMillis = 0L,
                packageName = pkg,
                sceneUri = "apk://$pkg/${parts[2]}",
                installed = true,
                source = HomeEnvironmentSource.ROOT_INSTALLED,
                officialHomeId = official?.id,
                sceneHash = sceneHash,
                knownSceneHashes = official?.sceneHashes ?: setOfNotNull(sceneHash),
                type = when {
                    ".env.vista." in pkg -> HomeEnvironmentType.VISTA
                    ".env.footprint." in pkg -> HomeEnvironmentType.FOOTPRINT
                    else -> HomeEnvironmentType.ENVIRONMENT
                },
            )
        }.distinctBy { it.packageName }.sortedBy { it.displayName.lowercase() }.toList()
    }

    suspend fun findActiveHomeWithShell(
        shellRunner: ShellRunner,
        homes: List<HomeEnvironment>,
    ): HomeEnvironment? {
        if (homes.isEmpty()) return null

        val result = shellRunner.run(activeHomeLookupCommand())
        if (!result.success) return null
        return findHomeForActiveSceneOutput(result.output, homes)
    }

    suspend fun findActiveInstalledHome(
        rootRunner: ShellRunner,
        homes: List<HomeEnvironment>,
    ): HomeEnvironment? {
        val result = rootRunner.run(
            "oculuspreferences --getc environment_selected environment_vista_selected default_footprint",
        )
        if (!result.success) return null
        val uris = Regex("apk://[^\\\"\\s]+", RegexOption.IGNORE_CASE)
            .findAll(result.output)
            .map { it.value }
            .toSet()
        return homes.firstOrNull { it.type == HomeEnvironmentType.ENVIRONMENT && it.sceneUri in uris }
            ?: homes.firstOrNull { it.sceneUri in uris }
    }

    fun formatSize(bytes: Long): String {
        val mb = bytes / 1024.0 / 1024.0
        return "${DecimalFormat("0.0").format(mb)} MB"
    }

    private fun findPreviewFor(apk: File): File? {
        val base = File(apk.parentFile, apk.nameWithoutExtension)
        return previewExtensions
            .map { File(base.parentFile, "${base.name}.$it") }
            .firstOrNull { it.exists() && it.isFile }
    }

    @Suppress("DEPRECATION")
    private fun isHomeApk(path: String): Boolean {
        return archivePackageName(path)?.let(::isHomePackage) == true
    }

    @Suppress("DEPRECATION")
    private fun archivePackageName(path: String): String? =
        packageManager.getPackageArchiveInfo(path, 0)?.packageName

    private fun sceneHash(apk: File): String? = runCatching {
        ZipFile(apk).use { zip ->
            val entry = zip.getEntry("assets/scene.zip") ?: return@runCatching null
            val digest = MessageDigest.getInstance("SHA-256")
            zip.getInputStream(entry).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }.getOrNull()

    private fun readCachedMetadata(file: ShellHomeFile): CachedHomeMetadata? {
        val raw = metadataCache.getString(cacheKey(file.path), null) ?: return null
        val parts = raw.split('\t')
        if (parts.size < 4) return null
        if (parts[0].toLongOrNull() != file.sizeBytes || parts[1].toLongOrNull() != file.modifiedSeconds) {
            return null
        }
        return CachedHomeMetadata(
            verified = parts[2] == "1",
            sceneHash = parts[3].takeIf { it.isNotBlank() },
        )
    }

    private fun writeCachedMetadata(file: ShellHomeFile, metadata: CachedHomeMetadata) {
        val value = listOf(
            file.sizeBytes.toString(),
            file.modifiedSeconds.toString(),
            if (metadata.verified) "1" else "0",
            metadata.sceneHash.orEmpty(),
        ).joinToString("\t")
        metadataCache.edit().putString(cacheKey(file.path), value).apply()
    }

    private fun cacheKey(path: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(path.toByteArray(Charsets.UTF_8))
        return "apk_" + digest.joinToString("") { "%02x".format(it) }
    }

    @Suppress("DEPRECATION")
    private fun isHomePackage(packageName: String): Boolean {
        val name = packageName.lowercase()
        return name == QuestHomeContract.TargetPackage ||
            ".environment." in name ||
            name.startsWith("com.environment.") ||
            name.startsWith("com.oculus.environment.") ||
            name.startsWith("com.meta.environment.") ||
            ".shell.env." in name ||
            ".env.vista." in name ||
            ".env.footprint." in name
    }

    private fun String.cleanHomeName(): String {
        return replace('_', ' ')
            .replace('-', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase() } }
    }

    private fun String.shellQuote(): String {
        return "'" + replace("'", "'\\''") + "'"
    }

    private data class ShellHomeFile(
        val path: String,
        val sizeBytes: Long,
        val modifiedSeconds: Long,
        val previewPath: String?,
    )

    private data class CachedHomeMetadata(
        val verified: Boolean,
        val sceneHash: String?,
    )
}

internal fun homeSourceForPath(path: String): HomeEnvironmentSource {
    val normalized = path.replace('\\', '/').lowercase()
    return if ("/download/quest homes/official library/" in normalized) {
        HomeEnvironmentSource.OFFICIAL_LIBRARY
    } else {
        HomeEnvironmentSource.PERSONAL_IMPORT
    }
}

internal fun installedHomeLookupCommand(): String = """
    for pkg in ${'$'}(pm list packages | sed 's/^package://'); do
      [ "${'$'}pkg" = "${QuestHomeContract.TargetPackage}" ] && continue
      case "${'$'}pkg" in
        *environment*|*env.vista*|*env.footprint*) ;;
        *) continue ;;
      esac
      path=${'$'}(pm path "${'$'}pkg" 2>/dev/null | sed -n 's/^package://p' | head -n 1)
      [ -n "${'$'}path" ] || continue
      unzip -l "${'$'}path" 2>/dev/null | grep -qE '[[:space:]]assets/scene\.zip${'$'}' || continue
      scene_hash=${'$'}(unzip -p "${'$'}path" assets/scene.zip 2>/dev/null | sha256sum 2>/dev/null | awk '{print ${'$'}1}')
      printf '%s\t%s\t%s\t%s\n' "${'$'}pkg" "${'$'}path" "assets/scene.zip" "${'$'}scene_hash"
    done
""".trimIndent()

internal fun mergeHomeCandidates(candidates: List<HomeEnvironment>): List<HomeEnvironment> {
    return candidates
        .groupBy(::homeIdentityKey)
        .values
        .map { group ->
            val preferred = group.maxWithOrNull(
                compareBy<HomeEnvironment> { sourcePriority(it.source) }
                    .thenBy { it.lastModifiedMillis }
                    .thenBy { it.sizeBytes }
                    .thenBy { it.apkPath.lowercase() },
            ) ?: error("A Home candidate group cannot be empty.")
            val sceneAliases = group
                .flatMap { home -> home.knownSceneHashes + listOfNotNull(home.sceneHash) }
                .map { it.trim().lowercase() }
                .filter { it.matches(EXACT_SCENE_HASH) }
                .toSet()
            val metadataSource = group.maxWithOrNull(
                compareBy<HomeEnvironment> { it.previewPath != null }
                    .thenBy { it.sizeBytes }
                    .thenBy { it.lastModifiedMillis },
            )
            preferred.copy(
                previewPath = preferred.previewPath ?: metadataSource?.previewPath,
                sizeBytes = preferred.sizeBytes.takeIf { it > 0L } ?: (metadataSource?.sizeBytes ?: 0L),
                knownSceneHashes = sceneAliases,
            )
        }
        .sortedBy { it.displayName.lowercase() }
}

private fun homeIdentityKey(home: HomeEnvironment): String {
    home.officialHomeId?.takeIf { it.isNotBlank() }?.let { return "official:${it.lowercase()}" }
    home.sceneHash?.trim()?.lowercase()?.takeIf { it.matches(EXACT_SCENE_HASH) }
        ?.let { return "scene:$it" }
    if (home.installed) {
        home.packageName?.takeIf { it.isNotBlank() }?.let { return "package:${it.lowercase()}" }
    }
    return "path:${home.apkPath.replace('\\', '/').lowercase()}"
}

private fun sourcePriority(source: HomeEnvironmentSource): Int = when (source) {
    HomeEnvironmentSource.ROOT_INSTALLED -> 3
    HomeEnvironmentSource.OFFICIAL_LIBRARY -> 2
    HomeEnvironmentSource.PERSONAL_IMPORT -> 1
}

private const val ACTIVE_SCENE_HASH_PREFIX = "ACTIVE_SCENE_SHA256="
private val EXACT_SCENE_HASH = Regex("^[0-9a-f]{64}${'$'}")

internal fun activeHomeLookupCommand(): String {
    return """
        max_attempts=10
        retry_delay=0.5
        attempt=1
        while [ "${'$'}attempt" -le "${'$'}max_attempts" ]; do
          candidate_paths=${'$'}(
            {
              pm path --user 0 ${QuestHomeContract.TargetPackage}
              pm path ${QuestHomeContract.TargetPackage}
              cmd package path --user 0 ${QuestHomeContract.TargetPackage}
              cmd package path ${QuestHomeContract.TargetPackage}
            } 2>/dev/null | tr -d '\r' | sed -n 's/^package://p' | sort -u
          )
          found=0
          for installed in ${'$'}candidate_paths; do
            [ -r "${'$'}installed" ] || continue
            unzip -l "${'$'}installed" 2>/dev/null | grep -qE '[[:space:]]assets/scene\.zip${'$'}' || continue
            installed_hash=${'$'}(unzip -p "${'$'}installed" assets/scene.zip 2>/dev/null | sha256sum | cut -d ' ' -f 1 | tr '[:upper:]' '[:lower:]')
            printf '%s\n' "${'$'}installed_hash" | grep -Eq '^[0-9a-f]{64}${'$'}' || continue
            printf '${ACTIVE_SCENE_HASH_PREFIX}%s\n' "${'$'}installed_hash"
            found=1
          done
          [ "${'$'}found" -eq 1 ] && exit 0
          [ "${'$'}attempt" -lt "${'$'}max_attempts" ] && sleep "${'$'}retry_delay"
          attempt=${'$'}((attempt + 1))
        done
        exit 1
    """.trimIndent()
}

internal fun findHomeForActiveSceneOutput(
    output: String,
    homes: List<HomeEnvironment>,
): HomeEnvironment? {
    val activeSceneHashes = output.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith(ACTIVE_SCENE_HASH_PREFIX) }
        .map { it.removePrefix(ACTIVE_SCENE_HASH_PREFIX).trim().lowercase() }
        .filter { it.matches(EXACT_SCENE_HASH) }
        .toSet()
    if (activeSceneHashes.isEmpty()) return null

    return homes.firstOrNull { home ->
        val knownHashes = home.knownSceneHashes + listOfNotNull(home.sceneHash)
        knownHashes.any { hash ->
            val normalizedHash = hash.trim().lowercase()
            normalizedHash.matches(EXACT_SCENE_HASH) && normalizedHash in activeSceneHashes
        }
    }
}
