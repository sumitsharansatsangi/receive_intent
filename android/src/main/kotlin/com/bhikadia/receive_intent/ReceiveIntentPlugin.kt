package com.bhikadia.receive_intent

import android.app.Activity
import android.content.Context
import android.content.Intent
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.json.JSONObject
//import android.net.Uri
//import android.os.Bundle
//import android.provider.OpenableColumns
// import android.util.Log


/** ReceiveIntentPlugin */
class ReceiveIntentPlugin : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var eventSink: EventSink? = null

    private lateinit var context: Context
    private var activity: Activity? = null


    private var initialIntentMap: Map<String, Any?>? = null
    private var latestIntentMap: Map<String, Any?>? = null
    private var initialIntent = true

    private fun handleIntent(intent: Intent, fromPackageName: String?) {
        // Log.e("ReceiveIntentPlugin", "intent: $intent")
        // val decodeData = intent.extras?.get("com.symbol.datawedge.decode_data")
        // Log.e("ReceiveIntentPlugin", "decodeData: $decodeData")
        // Log.e("ReceiveIntentPlugin", "fromPackageName: $fromPackageName")
        val intentMap = mapOf<String, Any?>(
                "fromPackageName" to fromPackageName,
                "fromSignatures" to fromPackageName?.let { getApplicationSignature(context, it).toList() },
                "action" to intent.action,
                "data" to intent.dataString,
                "filename" to getFileName(intent.data, context),
                "categories" to intent.categories?.toList(),
                "type" to intent.type,
                "extra" to intent.extras?.let { bundleToJSON(it).toString() }
        )
//        if (intent.data != null && intent.data!!.scheme.equals("content")) {
//            val cursor =context.contentResolver.query(intent.data!!, null, null, null, null)
//            try {
//                if (cursor != null && cursor.moveToFirst()) {
//                    intentMap["filePath"] = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
//                }
//            } finally {
//                cursor?.close()
//            }
//        }

        // Log.e("ReceiveIntentPlugin", "intentMap: $intentMap")
        if (initialIntent) {
            initialIntentMap = intentMap
            initialIntent = false
        }
        latestIntentMap = intentMap
        eventSink?.success(latestIntentMap)
    }

    private fun setResult(result: Result, resultCode: Int?, data: String?, shouldFinish: Boolean?) {
        if (resultCode != null) {
            if (data == null) {
                activity?.setResult(resultCode)
            } else {
                val json = JSONObject(data)
                activity?.setResult(resultCode, jsonToIntent(json))
            }
            if (shouldFinish == true) {
                activity?.finish()
            }
            return result.success(null)
        }
        result.error("InvalidArg", "resultCode can not be null", null)
    }


    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext

        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "receive_intent")
        methodChannel.setMethodCallHandler(this)

        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "receive_intent/event")
        eventChannel.setStreamHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getInitialIntent" -> {
                result.success(initialIntentMap)
            }
            "setResult" -> {
                setResult(result, call.argument("resultCode"), call.argument("data"), call.argument("shouldFinish"))
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onListen(arguments: Any?, events: EventSink?) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addOnNewIntentListener(fun(intent: Intent?): Boolean {
            // Log.e("addOnNewIntentListener", "intent: $intent")
            intent?.let { handleIntent(it, binding.activity.callingActivity?.packageName) }
            return false
        })
        handleIntent(binding.activity.intent, binding.activity.callingActivity?.packageName)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addOnNewIntentListener(fun(intent: Intent?): Boolean {
            intent?.let { handleIntent(it, binding.activity.callingActivity?.packageName) }
            return false
        })
        handleIntent(binding.activity.intent, binding.activity.callingActivity?.packageName)
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}
