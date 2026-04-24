package com.nawaz.fixlog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.webkit.JavascriptInterface;
import com.getcapacitor.BridgeActivity;
import java.util.ArrayList;

public class MainActivity extends BridgeActivity {

    private static final int SPEECH_REQUEST = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inject JS bridge after WebView loads
        getBridge().getWebView().post(new Runnable() {
            @Override
            public void run() {
                getBridge().getWebView().addJavascriptInterface(
                    new VoiceBridge(), "AndroidVoice"
                );
            }
        });
    }

    private class VoiceBridge {
        // Called from JS: AndroidVoice.startVoiceInput()
        // Opens Android's built-in voice input dialog
        @JavascriptInterface
        public void startVoiceInput() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe the issue and fix...");
                    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                    startActivityForResult(intent, SPEECH_REQUEST);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                );
                if (results != null && !results.isEmpty()) {
                    String text = results.get(0)
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\"", "\\\"");
                    // Send result back to JS
                    getBridge().getWebView().evaluateJavascript(
                        "window.onVoiceResult && window.onVoiceResult('" + text + "')", null
                    );
                }
            } else {
                // User cancelled or error
                getBridge().getWebView().evaluateJavascript(
                    "window.onVoiceResult && window.onVoiceResult('')", null
                );
            }
        }
    }
}
