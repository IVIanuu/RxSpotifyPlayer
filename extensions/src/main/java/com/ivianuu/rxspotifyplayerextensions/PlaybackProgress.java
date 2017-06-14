package com.ivianuu.rxspotifyplayerextensions;

/**
 * @author Manuel Wrage (IVIanuu)
 */
public class PlaybackProgress {

    private int duration;
    private int progress;

    public PlaybackProgress(int duration, int progress) {
        this.duration = duration;
        this.progress = progress;
    }

    public int getDuration() { return duration; }

    public int getProgress() { return progress; }
}
