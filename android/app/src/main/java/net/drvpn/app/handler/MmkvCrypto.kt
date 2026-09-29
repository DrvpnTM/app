package net.drvpn.app.handler

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.tencent.mmkv.MMKV
import net.drvpn.app.AppConfig
import net.drvpn.app.util.LogUtil
import java.io.File
import java.io.RandomAccessFile
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps the MMKV stores that hold credentials (profiles, subscription URLs) encrypted at rest.
 *
 * A random MMKV key is wrapped with an AES-GCM key that lives in the Android Keystore; only the
 * wrapped form is written to disk. Stores written by older versions in plaintext are re-keyed once.
 * Every app process runs [prepare] at startup under one file lock, before any store is opened.
 */
internal object MmkvCrypto {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEK_ALIAS = "drvpn_mmkv_kek"
    private const val WRAPPED_FILE = "mmkv_key.bin"
    private const val MIGRATED_FILE = "mmkv_encrypted_ids"
    private const val LOCK_FILE = "mmkv_crypto.lock"
    private const val FORMAT_VERSION: Byte = 1
    private const val KEY_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    /** MMKV key for sensitive stores, or null when the Keystore is unavailable (stores stay plaintext). */
    @Volatile
    var cryptKey: String? = null
        private set

    fun prepare(context: Context, sensitiveIds: List<String>) {
        val dir = context.noBackupFilesDir
        RandomAccessFile(File(dir, LOCK_FILE), "rw").use { raf ->
            raf.channel.lock().use {
                cryptKey = prepareLocked(dir, sensitiveIds)
            }
        }
    }

    private fun prepareLocked(dir: File, sensitiveIds: List<String>): String? {
        val wrapped = File(dir, WRAPPED_FILE)
        val marker = File(dir, MIGRATED_FILE)

        var key: String? = null
        if (wrapped.exists()) {
            if (!kekExists()) {
                // The Keystore key is gone (e.g. app data restored elsewhere); the encrypted data cannot be read.
                LogUtil.e(AppConfig.TAG, "Storage key is missing; resetting protected stores")
                wrapped.delete()
            } else {
                // The key exists, so a failure here is transient. Never open the stores without it:
                // that would corrupt them. Retry, then fail loudly with the data left intact.
                var lastError: Exception? = null
                for (attempt in 1..3) {
                    try {
                        key = unwrap(wrapped.readBytes())
                        break
                    } catch (e: Exception) {
                        lastError = e
                        Thread.sleep(200L * attempt)
                    }
                }
                if (key == null) {
                    if (lastError == null) {
                        // Unreadable blob: same situation as a missing key.
                        wrapped.delete()
                    } else {
                        throw IllegalStateException("Could not unlock protected storage", lastError)
                    }
                }
            }
        }
        if (key == null) {
            val migrated = readMigrated(marker)
            if (migrated.isNotEmpty()) {
                migrated.forEach { runCatching { MMKV.removeStorage(it) } }
                marker.delete()
            }
            val fresh = newKey()
            val blob = runCatching { wrap(fresh) }.getOrElse {
                LogUtil.e(AppConfig.TAG, "Android Keystore unavailable; storage stays unencrypted", it)
                return null
            }
            writeAtomic(wrapped, blob)
            key = fresh
        }

        val migrated = readMigrated(marker).toMutableSet()
        for (id in sensitiveIds) {
            if (id in migrated) continue
            MMKV.mmkvWithID(id, MMKV.MULTI_PROCESS_MODE)?.reKey(key)
            migrated.add(id)
            writeAtomic(marker, migrated.joinToString("\n").toByteArray())
        }
        return key
    }

    /** Re-keys a store copy inside [rootPath] (used by backup/restore). A null [to] removes encryption. */
    fun rekeyCopy(id: String, rootPath: String, from: String?, to: String?) {
        if (!File(rootPath, id).exists()) return
        val store = MMKV.mmkvWithID(id, MMKV.SINGLE_PROCESS_MODE, from, rootPath) ?: return
        store.reKey(to)
        store.close()
    }

    private fun readMigrated(marker: File): Set<String> =
        if (marker.exists()) marker.readText().lines().filter { it.isNotBlank() }.toSet() else emptySet()

    private fun writeAtomic(target: File, bytes: ByteArray) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
    }

    internal fun newKey(random: SecureRandom = SecureRandom()): String =
        (1..16).map { KEY_CHARS[random.nextInt(KEY_CHARS.length)] }.joinToString("")

    private fun kekExists(): Boolean =
        KeyStore.getInstance(KEYSTORE).apply { load(null) }.containsAlias(KEK_ALIAS)

    private fun kek(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getKey(KEK_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(
                KEK_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun wrap(key: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, kek())
        return encodeBlob(cipher.iv, cipher.doFinal(key.toByteArray(Charsets.US_ASCII)))
    }

    private fun unwrap(blob: ByteArray): String? {
        val (iv, ct) = decodeBlob(blob) ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, kek(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ct), Charsets.US_ASCII)
    }

    /** Blob layout: version (1 byte), IV length (1 byte), IV, ciphertext+tag. */
    internal fun encodeBlob(iv: ByteArray, ct: ByteArray): ByteArray =
        byteArrayOf(FORMAT_VERSION, iv.size.toByte()) + iv + ct

    internal fun decodeBlob(blob: ByteArray): Pair<ByteArray, ByteArray>? {
        if (blob.size < 2 || blob[0] != FORMAT_VERSION) return null
        val ivLen = blob[1].toInt() and 0xFF
        if (ivLen == 0 || blob.size <= 2 + ivLen) return null
        return blob.copyOfRange(2, 2 + ivLen) to blob.copyOfRange(2 + ivLen, blob.size)
    }
}
