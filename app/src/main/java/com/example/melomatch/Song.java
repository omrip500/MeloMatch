package com.example.melomatch;

public class Song {
    private String title;
    private String artist;
    private int albumArtResId;
    private int audioResId;
    private int startTime; // זמן התחלה במילישניות

    public Song(String title, String artist, int albumArtResId, int audioResId, int startTime) {
        this.title = title;
        this.artist = artist;
        this.albumArtResId = albumArtResId;
        this.audioResId = audioResId;
        this.startTime = startTime;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public int getAlbumArtResId() { return albumArtResId; }
    public int getAudioResId() { return audioResId; }
    public int getStartTime() { return startTime; }
}
