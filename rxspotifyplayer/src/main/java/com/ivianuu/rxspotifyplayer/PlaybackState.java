package com.ivianuu.rxspotifyplayer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.SpotifyPlayer;

/**
 * Playback state
 */
public final class PlaybackState {

    private boolean playing;
    private int duration;
    private int progress;
    private String uri;
    private long timestamp;

    PlaybackState(boolean playing, int duration, int progress, @NonNull String uri, long timestamp) {
        this.playing = playing;
        this.duration = duration;
        this.progress = progress;
        this.uri = uri;
        this.timestamp = timestamp;
    }

    static PlaybackState extractFromPlayer(@Nullable SpotifyPlayer spotifyPlayer) {
        boolean playing = false;
        int duration = -1;
        int progress = -1;
        String uri = "";
        long timestamp = System.currentTimeMillis();

        if (spotifyPlayer != null) {
            com.spotify.sdk.android.player.PlaybackState playbackState = spotifyPlayer.getPlaybackState();
            if (playbackState != null) {
                playing = playbackState.isPlaying;
                progress = (int) playbackState.positionMs;
            }
            Metadata metadata = spotifyPlayer.getMetadata();
            if (metadata != null && metadata.currentTrack != null) {
                duration = (int) metadata.currentTrack.durationMs;
                uri = metadata.currentTrack.uri;
            }
        }

        return new PlaybackState(playing, duration, progress, uri, timestamp);
    }

    /**
     * Returns playing state
     */
    public boolean isPlaying() {
        return playing;
    }

    /**
     * Gets the current duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Gets the current progress
     */
    public int getProgress() {
        return progress;
    }

    /**
     * Returns the estimated progress based on the timestamp
     */
    public int getEstimatedProgress() {
        if (playing) {
            return (int) (progress + (System.currentTimeMillis() - timestamp)); // add the time difference
        } else {
            // were not playing
            return progress;
        }
    }

    /**
     * Returns the current uri
     */
    @NonNull
    public String getUri() {
        return uri;
    }

    /**
     * Gets the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
}
