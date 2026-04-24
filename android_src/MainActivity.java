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

        // Setup JS bridge after WebView is ready
        getBridge().getWebView().post(new Runnable() {
            @Override
            public void run() {
                getBridge().getWebView().addJavascriptInterface(
                    new SpeechBridge(), "AndroidSpeech"
                );
            }
        });
    }

    private class SpeechBridge {

        @JavascriptInterface
        public void startListening() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isListening) stopSpeech();
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
            speechRecognizer = null;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
                callJS("if(window.onAndroidSpeechReady)window.onAndroidSpeechReady()");
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = escapeJs(matches.get(0));
                    callJS("if(window.onAndroidSpeechPartial)window.onAndroidSpeechPartial('" + text + "')");
                }
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = (matches != null && !matches.isEmpty()) ? escapeJs(matches.get(0)) : "";
                callJS("if(window.onAndroidSpeechResult)window.onAndroidSpeechResult('" + text + "')");
            }

            @Override
            public void onError(int error) {
                isListening = false;
                callJS("if(window.onAndroidSpeechError)window.onAndroidSpeechError(" + error + ")");
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
        callJS("if(window.onAndroidSpeechStopped)window.onAndroidSpeechStopped()");
    }

    private String escapeJs(String text) {
        return text.replace("\\", "\\\\")
                   .replace("'", "\\'")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "");
    }

    private void callJS(final String js) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getBridge().getWebView().evaluateJavascript(js, null);
            }
        });
    }
}
