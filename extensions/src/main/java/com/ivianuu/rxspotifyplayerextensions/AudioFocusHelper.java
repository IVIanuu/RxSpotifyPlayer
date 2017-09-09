package com.ivianuu.rxspotifyplayerextensions;

import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.NonNull;

import com.ivianuu.rxspotifyplayer.RxSpotifyPlayer;

/**
 * Automatically handles audio focus
 */
public class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener {

    private final AudioManager audioManager;
    private final RxSpotifyPlayer player;

    private boolean isDucking;
    private boolean shouldPlayOnFocusGain;

    private boolean hasFocus;

    private AudioFocusHelper(Context context, RxSpotifyPlayer player) {
        this.player = player;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * Returns a new audio focus helper
     */
    public static AudioFocusHelper create(@NonNull Context context, @NonNull RxSpotifyPlayer player) {
        return new AudioFocusHelper(context, player);
    }

    /**
     * Returns if we have focus
     */
    public boolean hasFocus() {
        return hasFocus;
    }

    /**
     * Requests focus returns true if granted
     */
    public boolean requestAudioFocus() {
        hasFocus = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        return hasFocus;
    }


    /**
     * Abandons focus
     */
    public void abandonFocus() {
        audioManager.abandonAudioFocus(this);
        hasFocus = false;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                player.pause().subscribe(() -> {

                }, throwable -> {

                });
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (player.getPlaybackState().isPlaying()) {
                    player.pause().subscribe(() -> shouldPlayOnFocusGain = true,
                            throwable -> {});
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (!isDucking && shouldPlayOnFocusGain) {
                    player.resume().subscribe(() -> {

                    }, throwable -> {});
                } else {
                    player.setVolume(1.0f).subscribe(() -> isDucking = false, throwable -> {});
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                player.setVolume(0.1f).subscribe(
                        () -> isDucking = true,
                        throwable -> {});
                break;
        }
    }

}
