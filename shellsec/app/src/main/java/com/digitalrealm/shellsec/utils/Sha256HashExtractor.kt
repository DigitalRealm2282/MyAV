package com.digitalrealm.shellsec.utils

import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Utility SHA256 Hash Extractor class for use in multiple locations
 */
object Sha256HashExtractor {
    fun getSha256Hash(filePath: String?): String? {
        val bytes = convertFileByteArray(filePath)
        return getDigestHash(bytes)
    }

    private fun convertFileByteArray(filePath: String?): ByteArray {
        val file = File(filePath)
        val size = file.length().toInt()
        val bytes = ByteArray(size)
        try {
            val buf = BufferedInputStream(FileInputStream(file))
            buf.read(bytes, 0, bytes.size)
            buf.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bytes
    }

    private fun getDigestHash(bytes: ByteArray): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            bytesToHexString(digest)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            null
        }
    }

    private fun bytesToHexString(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (aByte in bytes) {
            val hex = Integer.toHexString(0xFF and aByte.toInt())
            if (hex.length == 1) {
                sb.append('0')
            }
            sb.append(hex)
        }
        return sb.toString()
    }
}