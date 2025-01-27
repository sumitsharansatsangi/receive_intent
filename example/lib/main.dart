import 'dart:async';

import 'package:flutter/material.dart' hide Intent;
import 'package:receive_intent/receive_intent.dart';

void main() {
  runApp(const MyApp());
}

/// Example app widget for the plugin.
class MyApp extends StatefulWidget {
  /// Constructor of MyApp widget.
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  Intent? _initialIntent;

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _init() async {
    final receivedIntent = await ReceiveIntent.getInitialIntent();

    if (!mounted) return;

    setState(() {
      _initialIntent = receivedIntent;
    });
  }

  Widget _buildFromIntent(String label, Intent? intent) {
    final dataString = intent?.dataString;
    return Center(
      child: Column(
        children: [
          Text(label),
          Text(
              "fromPackage: ${intent?.fromPackageName}\nfromSignatures: ${intent?.fromSignatures}"),
          Text(
              'action: ${intent?.action}\ndata: ${dataString?.substring(dataString.lastIndexOf("/") + 1)} \ntype: ${intent?.type} \nFileName: ${intent?.fileName} \ncategories: ${intent?.categories}'),
          Text("extras: ${intent?.extra}")
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              _buildFromIntent("INITIAL", _initialIntent),
              StreamBuilder<Intent?>(
                stream: ReceiveIntent.receivedIntentStream,
                builder: (context, snapshot) =>
                    _buildFromIntent("STREAMED", snapshot.data),
              )
            ],
          ),
        ),
      ),
    );
  }
}
