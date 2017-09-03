package com.ivianuu.rxspotifyplayerextensions;

/**
 * Playback progress
 */
public final class PlaybackProgress {

    private int duration;
    private int progress;

    PlaybackProgress(int duration, int progress) {
        this.duration = duration;
        this.progress = progress;
    }

    /**
     * Returns the duration
     */
    public int getDuration() { return duration; }

    /**
     * Returns the progress
     */
    public int getProgress() { return progress; }
}
