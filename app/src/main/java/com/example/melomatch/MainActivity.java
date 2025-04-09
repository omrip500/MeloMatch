package com.example.melomatch;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ImageButton micButton;
    private TextView titleText, subText;

    private Animation rotateAnim, zoomAnim;

    // רשימת השירים המזויפים
    private Song[] songs;
    private int currentSongIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        micButton = findViewById(R.id.micButton);
        titleText = findViewById(R.id.titleText);
        subText = findViewById(R.id.subText);

        rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        zoomAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_in_out);

        // 🎵 הגדרת השירים הספציפיים שלך 🎵
        songs = new Song[]{
                new Song("תתארו לכם", "שלמה ארצי", R.drawable.album1, R.raw.song1, 24000), // מתחיל משניה 8
                new Song("מה עושות האיילות", "יוני רכטר", R.drawable.album2, R.raw.song2, 13500),
                new Song("נתתי לה חיי", "כוורת", R.drawable.album3, R.raw.song3, 40500)
        };

        micButton.setOnClickListener(v -> {
            titleText.setVisibility(View.GONE);
            subText.setVisibility(View.VISIBLE);
            subText.startAnimation(zoomAnim);
            micButton.startAnimation(rotateAnim);

            // מעבר ל-PlayerActivity אחרי 3 שניות (מזויף)
            micButton.postDelayed(() -> {
                Song currentSong = songs[currentSongIndex];

                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                intent.putExtra("title", currentSong.getTitle());
                intent.putExtra("artist", currentSong.getArtist());
                intent.putExtra("albumArt", currentSong.getAlbumArtResId());
                intent.putExtra("audioResId", currentSong.getAudioResId());
                intent.putExtra("startTime", currentSong.getStartTime());

                currentSongIndex = (currentSongIndex + 1) % songs.length; // מעבר לשיר הבא

                startActivity(intent);
                resetUI();
            }, 3000); // השהיה של 3 שניות
        });
    }

    private void resetUI() {
        micButton.clearAnimation();
        subText.clearAnimation();
        titleText.setVisibility(View.VISIBLE);
        subText.setVisibility(View.GONE);
    }
}
