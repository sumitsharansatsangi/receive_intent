package com.bhikadia.receive_intent

// import android.util.Log
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest
import java.util.ArrayList


fun jsonToBundle(json: JSONObject): Bundle {
    val bundle = Bundle()
    try {
        val iterator: Iterator<String> = json.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val value: Any = json.get(key)
            when (value.javaClass.getSimpleName()) {
                "String" -> bundle.putString(key, value as String)
                "Integer" -> bundle.putInt(key, value as Int)
                "Long" -> bundle.putLong(key, value as Long)
                "Boolean" -> bundle.putBoolean(key, value as Boolean)
                "JSONObject" -> bundle.putBundle(key, jsonToBundle(value as JSONObject))
                "Float" -> bundle.putFloat(key, value as Float)
                "Double" -> bundle.putDouble(key, value as Double)
                else -> bundle.putString(key, value.toString())
            }
        }
    } catch (e: JSONException) {
        e.printStackTrace()
    }
    return bundle

}

fun jsonToIntent(json: JSONObject): Intent = Intent().apply {
    putExtras(jsonToBundle(json))
}


fun bundleToJSON(bundle: Bundle): JSONObject {
    val json = JSONObject()
    val ks = bundle.keySet()
    val iterator: Iterator<String> = ks.iterator()
    while (iterator.hasNext()) {
        val key = iterator.next()
        try {
            // Log.e("ReceiveIntentPlugin wrapping key", "$key")
            json.put(key, wrap(bundle.get(key)))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
    return json
}

fun wrap(o: Any?): Any? {
    if (o == null) {
        // Log.e("ReceiveIntentPlugin", "$o is null")
        return JSONObject.NULL
    }
    if (o is JSONArray || o is JSONObject) {
        // Log.e("ReceiveIntentPlugin", "$o is JSONArray or JSONObject")
        return o
    }
    if (o == JSONObject.NULL) {
        // Log.e("ReceiveIntentPlugin", "$o is JSONObject.NULL")
        return o
    }
    try {
        if (o is Collection<*>) {
            // Log.e("ReceiveIntentPlugin", "$o is Collection<*>")
            if (o is ArrayList<*>) {
                // Log.e("ReceiveIntentPlugin", "..And also ArrayList")
                return toJSONArray(o)
            }
            return JSONArray(o as Collection<*>?)
        } else if (o.javaClass.isArray) {
            // Log.e("ReceiveIntentPlugin", "$o is isArray")
            return toJSONArray(o)
        }
        if (o is Map<*, *>) {
            // Log.e("ReceiveIntentPlugin", "$o is Map<*, *>")
            return JSONObject(o)
        }
        if (o is Boolean ||
            o is Byte ||
            o is Char ||
            o is Double ||
            o is Float ||
            o is Int ||
            o is Long ||
            o is Short ||
            o is String
        ) {
            return o
        }
        if (o.javaClass.getPackage() != null) {
            if (o is Uri || o.javaClass.getPackage()!!.name.startsWith("java.")) {
                return o.toString()
            }
        }
    } catch (e: Exception) {
        // Log.e("ReceiveIntentPlugin", e.message, e)
    }
    return null
}

@Throws(JSONException::class)
fun toJSONArray(array: Any): JSONArray? {
    val result = JSONArray()
    if (!array.javaClass.isArray && array !is ArrayList<*>) {
        // Log.e("ReceiveIntentPlugin not a primitive array", "")
        throw JSONException("Not a primitive array: " + array.javaClass)
    }

    when (array) {
        is List<*> -> {
            // Log.e("ReceiveIntentPlugin toJSONArray List", "")
            // Log.e("ReceiveIntentPlugin toJSONArray List size", "${array.size}")
            array.forEach { result.put(wrap(it)) }
        }

        is Array<*> -> {
            // Log.e("ReceiveIntentPlugin toJSONArray Array", "")
            // Log.e("ReceiveIntentPlugin toJSONArray Array size", "${array.size}")
            array.forEach { result.put(wrap(it)) }
        }

        is ArrayList<*> -> {
            // Log.e("ReceiveIntentPlugin toJSONArray ArrayList", "")
            array.forEach { result.put(wrap(it)) }
        }

        is ByteArray -> {
            // Log.e("ReceiveIntentPlugin toJSONArray ByteArray", "")
            array.forEach { result.put(wrap(it)) }
        }

        else -> {
            // val typename = array.javaClass.kotlin.simpleName
            // Log.e("ReceiveIntentPlugin toJSONArray else", "$typename")
            val length = java.lang.reflect.Array.getLength(array)
            for (i in 0 until length) {
                result.put(wrap(java.lang.reflect.Array.get(array, i)))
            }
        }
    }

    // Log.e("ReceiveIntentPlugin toJSONArray result", "$result")

    return result
}

fun getApplicationSignature(context: Context, packageName: String): List<String> {
    val signatureList: List<String>
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // New signature
            val sig = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            ).signingInfo
            if(sig !=null){
            signatureList = if (sig.hasMultipleSigners()) {
                // Send all with apkContentsSigners
                sig.apkContentsSigners.map {
                    val digest = MessageDigest.getInstance("SHA-256")
                    digest.update(it.toByteArray())
                    bytesToHex(digest.digest())
                }
            } else {
                // Send one with signingCertificateHistory
                sig.signingCertificateHistory.map {
                    val digest = MessageDigest.getInstance("SHA-256")
                    digest.update(it.toByteArray())
                    bytesToHex(digest.digest())
                }
            }
            }else{
                signatureList = emptyList()
            }
        } else {
            val sig = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            ).signatures
            signatureList = if (sig != null) {
                sig.map {
                    val digest = MessageDigest.getInstance("SHA-256")
                    digest.update(it.toByteArray())
                    bytesToHex(digest.digest())
                }
            } else{
                emptyList()
            }
        }
        return signatureList
    } catch (e: Exception) {
        // Handle error
    }
    return emptyList()
}

fun bytesToHex(bytes: ByteArray): String {
    val hexArray =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
    val hexChars = CharArray(bytes.size * 2)
    var v: Int
    for (j in bytes.indices) {
        v = bytes[j].toInt() and 0xFF
        hexChars[j * 2] = hexArray[v.ushr(4)]
        hexChars[j * 2 + 1] = hexArray[v and 0x0F]
    }
    return String(hexChars)
}


fun getFileName(uri: Uri, context: Context): String? {
    var result: String? = null
    if (uri.scheme.equals("content")) {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result
}