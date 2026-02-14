import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';

/// Result code indicating that an operation completed successfully.
///
/// Equivalent to Android's `Activity.RESULT_OK`.
/// Used when returning results back to the calling Android Activity.
const int kActivityResultOk = -1;

/// Result code indicating that an operation was canceled.
///
/// Equivalent to Android's `Activity.RESULT_CANCELED`.
/// Used when the operation was aborted or user exited without completing.
const int kActivityResultCanceled = 0;

/// Represents an Android [Intent] in the Dart world.
///
/// This class acts as a bridge model between native Android `Intent`
/// and Flutter code via MethodChannel and EventChannel.
///
/// It contains commonly used intent properties such as:
/// - action
/// - data (URI)
/// - MIME type
/// - categories
/// - extras
/// - calling package information
///
/// An instance may represent a *null* intent when [isNull] is true.
class Intent {
  /// Whether this intent represents a null value from native side.
  ///
  /// If `true`, no actual intent was received.
  final bool isNull;

  /// The fully qualified class name of the component that handled the intent.
  final String? componentClassName;

  /// The package name of the app that sent this intent.
  final String? fromPackageName;

  /// The signing certificate hashes of the calling package.
  ///
  /// Useful if you want to validate or restrict calling apps.
  final List<String>? fromSignatures;

  /// The intent action (e.g. `android.intent.action.VIEW`).
  final String? action;

  /// The data URI string associated with the intent.
  ///
  /// Example:
  /// `content://...`
  final String? data;

  /// Optional file name extracted from the intent, if available.
  final String? fileName;

  /// The MIME type of the intent data.
  ///
  /// Example:
  /// `image/png`
  /// `application/pdf`
  final String? type;

  /// Categories attached to the intent.
  final List<String>? categories;

  /// Extra data bundled with the intent.
  ///
  /// This is decoded from JSON sent by the native side.
  final Map<String, dynamic>? extra;

  /// Returns `true` when this intent contains valid data.
  bool get isNotNull => !isNull;

  /// Creates a Dart representation of an Android Intent.
  ///
  /// By default, an [Intent] is considered null unless [isNull] is set to false.
  const Intent({
    this.isNull = true,
    this.componentClassName,
    this.fromPackageName,
    this.fromSignatures,
    this.action,
    this.data,
    this.fileName,
    this.type,
    this.categories,
    this.extra,
  });

  /// Creates an [Intent] from a platform channel map.
  ///
  /// Typically used internally when decoding data returned
  /// from a [MethodChannel] or [EventChannel].
  ///
  /// If [map] is null, a null-intent instance is returned.
  factory Intent.fromMap(Map? map) => Intent(
    isNull: map == null,
    componentClassName: map?["componentClassName"],
    fromPackageName: map?["fromPackageName"],
    fromSignatures: map?["fromSignatures"] != null
        ? List.unmodifiable(
            (map!["fromSignatures"] as List).map((e) => e.toString()),
          )
        : null,
    action: map?["action"],
    data: map?["data"],
    fileName: map?["filename"],
    type: map?["type"],
    categories: map?["categories"] != null
        ? List.unmodifiable(
            (map!["categories"] as List).map((e) => e.toString()),
          )
        : null,
    extra: map?["extra"] != null
        ? (json.decode(map!["extra"]) as Map).map(
            (key, value) => MapEntry(key.toString(), value),
          )
        : null,
  );

  /// Converts this [Intent] into a serializable map.
  ///
  /// Useful when sending intent-like data back to native code.
  Map<String, dynamic> toMap() => {
    "componentClassName": componentClassName,
    "fromPackageName": fromPackageName,
    "fromSignatures": fromSignatures,
    "action": action,
    "data": data,
    "filename": fileName,
    "type": type,
    "categories": categories,
    "extra": extra,
  };

  @override
  String toString() {
    if (isNull) return 'Intent(null)';
    final str = 'Intent${toMap()}';
    return str.replaceFirst('{', '(').replaceFirst('}', ')', str.length - 1);
  }
}

/// A Flutter wrapper for receiving and responding to Android Intents.
///
/// This class communicates with the native Android layer using:
/// - [MethodChannel] for one-time calls
/// - [EventChannel] for continuous intent updates
///
/// Typical use cases:
/// - Receiving shared files (`ACTION_SEND`)
/// - Handling deep links
/// - Processing custom scheme URLs
/// - Acting as a target of "Open With"
class ReceiveIntent {
  static const MethodChannel _methodChannel = MethodChannel('receive_intent');

  static const EventChannel _eventChannel = EventChannel(
    "receive_intent/event",
  );

  /// Returns the intent that initially launched the app.
  ///
  /// This should typically be called inside `main()` or early in app startup
  /// to process launch-time deep links or share intents.
  ///
  /// Returns:
  /// - An [Intent] if available
  /// - A null-intent (where `isNull == true`) if none exists
  static Future<Intent?> getInitialIntent() async {
    final map = await _methodChannel.invokeMapMethod('getInitialIntent');
    return Intent.fromMap(map);
  }

  /// Stream of intents received while the app is running.
  ///
  /// Listen to this stream to handle:
  /// - New share intents
  /// - Incoming deep links
  /// - External app interactions
  ///
  /// Example:
  /// ```dart
  /// ReceiveIntent.receivedIntentStream.listen((intent) {
  ///   if (intent?.isNotNull == true) {
  ///     print(intent?.data);
  ///   }
  /// });
  /// ```
  static Stream<Intent?> receivedIntentStream = _eventChannel
      .receiveBroadcastStream()
      .map<Intent?>((event) => Intent.fromMap(event as Map?));

  /// Sets the result for the calling Android activity.
  ///
  /// Parameters:
  /// - [resultCode]: Usually [kActivityResultOk] or [kActivityResultCanceled]
  /// - [data]: Optional result data to send back (encoded as JSON)
  /// - [shouldFinish]: Whether the current activity should close after sending result
  ///
  /// Useful when your Flutter Activity was started for result
  /// and needs to respond back to the caller.
  static Future<void> setResult(
    int resultCode, {
    Map<String, Object?>? data,
    bool shouldFinish = false,
  }) async {
    await _methodChannel.invokeMethod('setResult', <String, dynamic>{
      "resultCode": resultCode,
      if (data != null) "data": json.encode(data),
      "shouldFinish": shouldFinish,
    });
  }
}
