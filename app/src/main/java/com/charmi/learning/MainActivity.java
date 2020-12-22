package com.charmi.learning;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements RecognitionListener {

    private TextView returnedText;
    private ImageView ivSearch;
    private ProgressBar progressBar;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private String TAG = "VoiceRecognitionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkMyPermission();
        returnedText = findViewById(R.id.tvResult);
        progressBar =  findViewById(R.id.progressBar);
        ivSearch =  findViewById(R.id.ivVoiceSearch);

        progressBar.setVisibility(View.INVISIBLE);
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        ivSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    returnedText.setText("");
                    progressBar.setVisibility(View.VISIBLE);
                    speech.startListening(recognizerIntent);

            }
        });

    }

    private void checkMyPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    101);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 ){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            finish();
            startActivity(getIntent());
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (speech != null) {
            speech.destroy();
            Log.i(TAG, "destroy");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speech.stopListening();
        speech.destroy();
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.i(TAG, "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(TAG, "onBeginningOfSpeech");
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(TAG, "onBufferReceived: " + buffer);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(TAG, "onEndOfSpeech");
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onError(int error) {
        String errorMessage = getErrorText(error);
        Log.d(TAG, "FAILED " + errorMessage);
        returnedText.setText(errorMessage);
    }

    @Override
    public void onResults(Bundle results) {
        Log.i(TAG, "onResults");
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null)
            returnedText.setText(matches.get(0));
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.i(TAG, "onEvent");
    }

    public String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }
}