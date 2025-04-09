package com.example.melomatch;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivityWithWordsRecognition extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private ImageButton micButton;
    private TextView titleText, subText, resultText;

    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;

    private Animation rotateAnim, zoomAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // בדיקת הרשאה למיקרופון
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }

        micButton = findViewById(R.id.micButton);
        titleText = findViewById(R.id.titleText);
        subText = findViewById(R.id.subText);
        resultText = findViewById(R.id.resultText);

        rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        zoomAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_in_out);

        initSpeechRecognition();

        // אירוע לחיצה על כפתור המיקרופון
        micButton.setOnClickListener(v -> {
            titleText.setVisibility(View.GONE);
            subText.setVisibility(View.VISIBLE);
            subText.startAnimation(zoomAnim);
            micButton.startAnimation(rotateAnim);

            // איפוס טקסט התוצאה
            resultText.setText("");
            speechRecognizer.startListening(speechRecognizerIntent);
        });
    }

    private void initSpeechRecognition() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        // הגדרת מודל השפה
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // הגדרת שפה (כאן מוגדר שיבחר לפי ברירת המחדל במכשיר)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        // הגדרת EXTRA_PARTIAL_RESULTS כדי לקבל תוצאות ביניים (ולזרז הופעת טקסט)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        // הגבלנו את מספר התוצאות (לא חובה, רק לשם יעילות)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        // הגדרת זמני המתנה לקיצור העיכוב:
        // זמן שקט "מוחלט" שמביא לסיום ההאזנה
        speechRecognizerIntent.putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);
        // זמן שקט "אפשרי" שמביא לסיום (גורם למנוע להתחיל לחשוב שסיימת לדבר)
        speechRecognizerIntent.putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);
        // אורך מינימלי לדגימה (אפשר להקטין עוד יותר בהתאם לצורך)
        speechRecognizerIntent.putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d("SPEECH", "Ready for speech");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d("SPEECH", "Beginning of speech");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // אפשר לשים כאן קוד שמעדכן UI על עוצמת הקול, אם רוצים
            }

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                Log.d("SPEECH", "End of speech");
            }

            @Override
            public void onResults(Bundle results) {
                Log.d("SPEECH", "Got final results");
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognized = matches.get(0);
                    Log.d("SPEECH", "Recognized: " + recognized);
                    resultText.setText(recognized);
                }
                resetUI();
            }

            @Override
            public void onError(int error) {
                Log.e("SPEECH", "Error code: " + error);
                resultText.setText("Error recognizing speech. Try again.");
                resetUI();
            }

            // קבלת תוצאות ביניים (partial results)
            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partial != null && !partial.isEmpty()) {
                    // מציגים על המסך את הטקסט הביניים (שעשוי להתעדכן/להשתנות כל פעם)
                    resultText.setText(partial.get(0));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void resetUI() {
        micButton.clearAnimation();
        subText.clearAnimation();
        titleText.setVisibility(View.VISIBLE);
        subText.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
