package io.github.nikitat21.questhomeswitcher.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import io.github.nikitat21.questhomeswitcher.domain.HomeEnvironment
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
                HomeEnvironment(
                    displayName = OfficialHomeCatalog.resolve(
                        packageName = packageName,
                        sceneHash = sceneHash,
                        fallback = apk.nameWithoutExtension.cleanHomeName(),
                    ),
                    apkPath = apk.absolutePath,
                    previewPath = findPreviewFor(apk)?.absolutePath,
                    sizeBytes = apk.length(),
                    lastModifiedMillis = apk.lastModified(),
                    packageName = packageName,
                    verifiedHomeApk = true,
                    sceneHash = sceneHash,
                )
            }
            .distinctBy { it.sceneHash?.lowercase() ?: it.apkPath }
            .sortedBy { it.displayName.lowercase() }
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
                HomeEnvironment(
                    displayName = OfficialHomeCatalog.resolve(
                        packageName = null,
                        sceneHash = cached.sceneHash,
                        fallback = fileName.substringBeforeLast('.').cleanHomeName(),
                    ),
                    apkPath = apkPath,
                    previewPath = file.previewPath,
                    sizeBytes = file.sizeBytes,
                    lastModifiedMillis = file.modifiedSeconds * 1000L,
                    verifiedHomeApk = true,
                    sceneHash = cached.sceneHash,
                )
            }
            .distinctBy { it.sceneHash?.lowercase() ?: it.apkPath }
            .sortedBy { it.displayName.lowercase() }
    }

    suspend fun loadInstalledHomes(rootRunner: ShellRunner): List<HomeEnvironment> {
        val command = """
            for pkg in ${'$'}(pm list packages | sed 's/^package://'); do
              case "${'$'}pkg" in
                *environment*|*env.vista*|*env.footprint*) ;;
                *) continue ;;
              esac
              path=${'$'}(pm path "${'$'}pkg" 2>/dev/null | sed -n 's/^package://p' | head -n 1)
              [ -n "${'$'}path" ] || continue
              unzip -l "${'$'}path" 2>/dev/null | grep -qE '[[:space:]]assets/scene\.zip${'$'}' || continue
              printf '%s\t%s\t%s\n' "${'$'}pkg" "${'$'}path" "assets/scene.zip"
            done
        """.trimIndent()
        val result = rootRunner.run(command)
        if (!result.success) return emptyList()
        return result.output.lineSequence().mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size < 3) return@mapNotNull null
            val pkg = parts[0]
            HomeEnvironment(
                displayName = OfficialHomeCatalog.resolve(
                    packageName = pkg,
                    sceneHash = null,
                    fallback = pkg.substringAfterLast('.').cleanHomeName(),
                ),
                apkPath = parts[1],
                previewPath = null,
                sizeBytes = 0L,
                lastModifiedMillis = 0L,
                packageName = pkg,
                sceneUri = "apk://$pkg/${parts[2]}",
                installed = true,
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

        val command = """
            installed=${'$'}(pm path --user 0 ${QuestHomeContract.TargetPackage} 2>/dev/null | sed -n 's/^package://p' | head -n 1)
            [ -n "${'$'}installed" ] && [ -r "${'$'}installed" ] || exit 1
            unzip -l "${'$'}installed" 2>/dev/null | grep -qE '[[:space:]]assets/scene\.zip${'$'}' || exit 1
            unzip -p "${'$'}installed" assets/scene.zip 2>/dev/null | sha256sum | cut -d ' ' -f 1
        """.trimIndent()

        val result = shellRunner.run(command)
        if (!result.success) return null
        val activeSceneHash = result.output.lineSequence()
            .firstOrNull { it.matches(Regex("[0-9a-fA-F]{64}")) }
            ?.trim()
            ?: return null
        return homes.firstOrNull { it.sceneHash.equals(activeSceneHash, ignoreCase = true) }
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
