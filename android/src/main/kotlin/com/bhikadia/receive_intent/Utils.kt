package com.bhikadia.receive_intent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/* ------------------------------------------------------------
 * JSON → Bundle
 * ------------------------------------------------------------ */

fun jsonToBundle(json: JSONObject): Bundle {
    val bundle = Bundle()

    json.keys().forEach { key ->
        when (val value = json.opt(key)) {
            is String -> bundle.putString(key, value)
            is Int -> bundle.putInt(key, value)
            is Long -> bundle.putLong(key, value)
            is Boolean -> bundle.putBoolean(key, value)
            is Float -> bundle.putFloat(key, value)
            is Double -> bundle.putDouble(key, value)
            is JSONObject -> bundle.putBundle(key, jsonToBundle(value))
            null, JSONObject.NULL -> Unit
            else -> bundle.putString(key, value.toString())
        }
    }

    return bundle
}

fun jsonToIntent(json: JSONObject): Intent =
    Intent().apply { putExtras(jsonToBundle(json)) }

/* ------------------------------------------------------------
 * Bundle → JSON
 * ------------------------------------------------------------ */

fun bundleToJSON(bundle: Bundle): JSONObject =
    JSONObject().apply {
        bundle.keySet().forEach { key ->
            put(key, wrap(bundle.get(key)))
        }
    }

/* ------------------------------------------------------------
 * Wrap helper (clean + safe)
 * ------------------------------------------------------------ */

fun wrap(value: Any?): Any? = when (value) {
    null -> JSONObject.NULL
    is JSONObject, is JSONArray -> value
    is Map<*, *> -> JSONObject(value)
    is Collection<*> -> JSONArray(value.map { wrap(it) })
    is Array<*> -> JSONArray(value.map { wrap(it) })
    is ByteArray -> JSONArray(value.map { it.toInt() })
    is Boolean,
    is Byte,
    is Char,
    is Double,
    is Float,
    is Int,
    is Long,
    is Short,
    is String -> value
    is Uri -> value.toString()
    else -> value.toString()
}

/* ------------------------------------------------------------
 * Signature Extraction (24–36 safe)
 * ------------------------------------------------------------ */

fun getApplicationSignature(
    context: Context,
    packageName: String
): List<String> {
    return try {

        val packageManager = context.packageManager

        val packageInfo = when {
            Build.VERSION.SDK_INT >= 33 -> {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(
                        PackageManager.GET_SIGNING_CERTIFICATES.toLong()
                    )
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            }

            else -> {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
        }

        val digest = MessageDigest.getInstance("SHA-256")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

            val signingInfo = packageInfo.signingInfo
                ?: throw IllegalStateException("No signature found")

            val signatures = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }

            signatures.map {
                digest.reset()
                digest.update(it.toByteArray())
                digest.digest().toHex()
            }

        } else {

            @Suppress("DEPRECATION")
            val signatures = packageInfo.signatures
                ?: throw IllegalStateException("No signature found")

            signatures.map {
                digest.reset()
                digest.update(it.toByteArray())
                digest.digest().toHex()
            }
        }

    } catch (_: Exception) {
        emptyList()
    }
}

private fun ByteArray.toHex(): String =
    joinToString("") { "%02X".format(it) }

/* ------------------------------------------------------------
 * File Name Resolver (StrictMode-safe)
 * ------------------------------------------------------------ */

fun getFileName(uri: Uri?, context: Context): String? {
    if (uri == null) return null

    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
    }

    return uri.lastPathSegment
}
