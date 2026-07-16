package io.github.nikitat21.questhomeswitcher.shell.localadb

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/** Opaque, destroyable host identity bytes. Plaintext is never exposed outside this package. */
class LocalAdbKeyMaterial private constructor(
    encoded: ByteArray,
) : AutoCloseable {
    private var bytes: ByteArray? = encoded.copyOf()

    val size: Int
        @Synchronized get() = checkNotNull(bytes) { "ADB key material has been destroyed" }.size

    @Synchronized
    internal fun copyForStorage(): ByteArray =
        checkNotNull(bytes) { "ADB key material has been destroyed" }.copyOf()

    @Synchronized
    override fun close() {
        bytes?.fill(0)
        bytes = null
    }

    companion object {
        internal const val MIN_SIZE_BYTES = 32
        internal const val MAX_SIZE_BYTES = 64 * 1_024

        fun copyOf(encoded: ByteArray): LocalAdbKeyMaterial {
            require(encoded.size in MIN_SIZE_BYTES..MAX_SIZE_BYTES) {
                "ADB key material has an invalid size"
            }
            return LocalAdbKeyMaterial(encoded)
        }
    }
}

/** App-private host identity persistence; implementations must never use external/shared storage. */
interface LocalAdbKeyStorage {
    @Throws(IOException::class)
    fun load(): LocalAdbKeyMaterial?

    @Throws(IOException::class)
    fun store(material: LocalAdbKeyMaterial)

    @Throws(IOException::class)
    fun clear()
}

/**
 * Atomic storage below [Context.getNoBackupFilesDir].
 *
 * The application manifest also disables backup globally, giving the ADB host identity two
 * independent backup barriers.
 */
class NoBackupLocalAdbKeyStorage(context: Context) : LocalAdbKeyStorage {
    private val lock = Any()
    private val directory = File(context.noBackupFilesDir, DIRECTORY_NAME)
    private val baseFile = File(directory, KEY_FILE_NAME)
    private val atomicFile = AtomicFile(baseFile)

    override fun load(): LocalAdbKeyMaterial? = synchronized(lock) {
        val encoded = try {
            // openRead/readFully performs AtomicFile's interrupted-write recovery before we
            // inspect the bytes. Checking baseFile first would bypass a valid backup file.
            atomicFile.readFully()
        } catch (_: FileNotFoundException) {
            return@synchronized null
        }
        try {
            if (encoded.size !in LocalAdbKeyMaterial.MIN_SIZE_BYTES..
                LocalAdbKeyMaterial.MAX_SIZE_BYTES
            ) {
                throw IOException("Stored ADB host identity has an invalid size")
            }
            LocalAdbKeyMaterial.copyOf(encoded)
        } catch (error: IllegalArgumentException) {
            throw IOException("Stored ADB host identity is invalid", error)
        } finally {
            encoded.fill(0)
        }
    }

    override fun store(material: LocalAdbKeyMaterial) = synchronized(lock) {
        ensureDirectory()
        val encoded = material.copyForStorage()
        val output = atomicFile.startWrite()
        try {
            output.write(encoded)
            output.fd.sync()
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        } finally {
            encoded.fill(0)
        }
    }

    override fun clear() = synchronized(lock) {
        atomicFile.delete()
        if (directory.exists() && directory.list()?.isEmpty() == true) {
            directory.delete()
        }
    }

    private fun ensureDirectory() {
        if (directory.isDirectory) return
        if (directory.exists() || !directory.mkdirs()) {
            throw IOException("Unable to create app-private ADB key directory")
        }
    }

    private companion object {
        const val DIRECTORY_NAME = "local_adb_bridge"
        const val KEY_FILE_NAME = "host_identity.bin"
    }
}
