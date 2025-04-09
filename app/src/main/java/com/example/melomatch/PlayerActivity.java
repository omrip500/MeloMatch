package com.example.melomatch;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PlayerActivity extends AppCompatActivity {

    private static final int RECORD_AUDIO_PERMISSION_REQUEST = 123;

    private ImageButton closeButton;
    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private TextView tvCurrentTime, tvTotalTime;

    private Handler handler = new Handler();
    private Runnable updateRunnable;

    // משתנים הקשורים להאזנה למיקרופון
    private AudioRecord audioRecord;
    private boolean isRecording = false;    // כדי לשלוט בלולאת ההאזנה
    private boolean userIsSinging = false;  // האם המשתמש שר כעת?

    // הגדרות בסיסיות ל-AudioRecord
    private static final int SAMPLING_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT   = AudioFormat.ENCODING_PCM_16BIT;

    // סף דציבל גס לזיהוי “שירה”
    private static double THRESHOLD = 3000.0;

    // כמה דגימות "רצופות" נדרשות כדי להגדיר מצב חדש של שירה או שקט
    private static final int REQUIRED_SILENCE_FRAMES = 8;  // אחרי 10 "מסגרות" שקט נעצור
    private static final int REQUIRED_SINGING_FRAMES = 2;   // אחרי 2 "מסגרות" שירה נמשיך

    private int consecutiveSilenceFrames = 0;
    private int consecutiveSingingFrames = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // איתור רכיבים מה-UI
        TextView songTitle = findViewById(R.id.songTitle);
        TextView artistName = findViewById(R.id.artistName);
        ImageView albumArt = findViewById(R.id.albumArt);
        closeButton = findViewById(R.id.closeButton);
        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);

        // קבלת נתוני השיר מה-Intent
        String title = getIntent().getStringExtra("title");
        String artist = getIntent().getStringExtra("artist");
        int albumArtResId = getIntent().getIntExtra("albumArt", 0);
        int audioResId = getIntent().getIntExtra("audioResId", 0);
        final int startTime = getIntent().getIntExtra("startTime", 0);

        // עדכון ה-UI
        songTitle.setText(title);
        artistName.setText(artist);
        albumArt.setImageResource(albumArtResId);

        // אנימציית סיבוב
        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_anim);
        albumArt.startAnimation(rotateAnimation);

        // יצירת ה־MediaPlayer
        mediaPlayer = MediaPlayer.create(this, audioResId);
        if (mediaPlayer == null) {
            finish();
            return;
        }

        // אם השיר הוא "נתתי לה חיי", מנמיכים את הווליום מראש לכל אורכו
        // אפשר גם להשתמש ב־contains("נתתי לה חיי") במקום equals, אם השם לא תמיד תואם ב-100%.
        if ("נתתי לה חיי".equals(title)) {
            mediaPlayer.setVolume(0.3f, 0.3f); // 30% ווליום בשני הערוצים
        }

        // קובעים את נקודת ההתחלה בשיר
        mediaPlayer.seekTo(startTime);
        mediaPlayer.start();

        // הגדרת ה־SeekBar
        seekBar.setMax(mediaPlayer.getDuration());
        seekBar.setProgress(startTime);

        // עדכון הטקסטים עבור הזמן ההתחלתי והזמן הכולל
        tvCurrentTime.setText(formatTime(startTime));
        tvTotalTime.setText(formatTime(mediaPlayer.getDuration()));

        // Runnable לעדכון ה־SeekBar והזמן הנוכחי בכל שנייה
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    try {
                        if (mediaPlayer.isPlaying()) {
                            int currentPos = mediaPlayer.getCurrentPosition();
                            seekBar.setProgress(currentPos);
                            tvCurrentTime.setText(formatTime(currentPos));
                        }
                        // ירוץ בכל מקרה שוב בעוד שנייה
                        handler.postDelayed(this, 1000);

                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        handler.post(updateRunnable);

        // כפתור הסגירה
        closeButton.setOnClickListener(v -> {
            stopAndReleasePlayer();
            finish();
        });

        // בודקים אם יש הרשאת מיקרופון; אם לא, מבקשים
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_PERMISSION_REQUEST
            );
        } else {
            startListeningToMicrophone();
        }
    }

    // טיפול בתשובת המשתמש לגבי הרשאה
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListeningToMicrophone();
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void startListeningToMicrophone() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLING_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Toast.makeText(this, "AudioRecord initialization failed", Toast.LENGTH_SHORT).show();
            return;
        }

        isRecording = true;
        audioRecord.startRecording();

        // שרשור שמאזין למיקרופון ומחשב אמפליטודה
        new Thread(() -> {
            short[] buffer = new short[bufferSize];
            while (isRecording) {
                int read = audioRecord.read(buffer, 0, bufferSize);
                if (read > 0) {
                    double amplitude = 0;
                    for (int i = 0; i < read; i++) {
                        amplitude += Math.abs(buffer[i]);
                    }
                    amplitude /= read;

                    Log.d("AudioDebug", "Amplitude = " + amplitude);

                    boolean singingNow = amplitude > THRESHOLD;

                    if (singingNow) {
                        consecutiveSingingFrames++;
                        consecutiveSilenceFrames = 0;
                    } else {
                        consecutiveSilenceFrames++;
                        consecutiveSingingFrames = 0;
                    }

                    // אם לא היינו במצב שירה ועברנו את מספר המסגרות הרצופות הנדרש, מעבר למצב שירה
                    if (!userIsSinging && consecutiveSingingFrames >= REQUIRED_SINGING_FRAMES) {
                        userIsSinging = true;
                        runOnUiThread(() -> {
                            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                                mediaPlayer.start();
                            }
                        });
                    }
                    // אם היינו במצב שירה ועברנו את מספר המסגרות הרצופות לשקט, מעבר למצב שקט
                    else if (userIsSinging && consecutiveSilenceFrames >= REQUIRED_SILENCE_FRAMES) {
                        userIsSinging = false;
                        runOnUiThread(() -> {
                            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                mediaPlayer.pause();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    private void stopAndReleasePlayer() {
        handler.removeCallbacks(updateRunnable);

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopMicrophoneListening();
    }

    private void stopMicrophoneListening() {
        isRecording = false;
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            audioRecord = null;
        }
    }

    // פירמוט הזמן לצורת mm:ss
    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAndReleasePlayer();
    }
}
