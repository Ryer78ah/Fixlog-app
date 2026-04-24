package com.nawaz.fixlog;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;
import java.util.ArrayList;

public class MainActivity extends BridgeActivity {

    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupSpeechBridge();
    }

    private void setupSpeechBridge() {
        WebView webView = getBridge().getWebView();
        webView.addJavascriptInterface(new SpeechBridge(), "AndroidSpeech");
    }

    private class SpeechBridge {

        @JavascriptInterface
        public void startListening() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isListening) {
                        stopSpeech();
                    }
                    startSpeech();
                }
            });
        }

        @JavascriptInterface
        public void stopListening() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stopSpeech();
                }
            });
        }

        @JavascriptInterface
        public boolean isAvailable() {
            return SpeechRecognizer.isRecognitionAvailable(MainActivity.this);
        }
    }

    private void startSpeech() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
                callJS("window.onAndroidSpeechReady()");
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0)
                        .replace("'", "\\'")
                        .replace("\"", "\\\"");
                    callJS("window.onAndroidSpeechPartial('" + text + "')");
                }
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0)
                        .replace("'", "\\'")
                        .replace("\"", "\\\"");
                    callJS("window.onAndroidSpeechResult('" + text + "')");
                } else {
                    callJS("window.onAndroidSpeechResult('')");
                }
            }

            @Override
            public void onError(int error) {
                isListening = false;
                callJS("window.onAndroidSpeechError(" + error + ")");
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onEndOfSpeech() {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEvent(int eventType, Bundle params) {}
            @Override public void onRmsChanged(float rmsdB) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        speechRecognizer.startListening(intent);
    }

    private void stopSpeech() {
        isListening = false;
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        callJS("window.onAndroidSpeechStopped()");
    }

    private void callJS(final String js) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getBridge().getWebView().evaluateJavascript(js, null);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }
}
