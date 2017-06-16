package com.ivianuu.rxspotifyplayerextensions;

import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;

import com.ivianuu.rxspotifyplayer.RxSpotifyPlayer;

import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

/**
 * Automatically handles audio focus
 */
public class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener {

    private AudioManager audioManager;
    private RxSpotifyPlayer player;

    private boolean isDucking;
    private boolean shouldPlayOnFocusGain;

    private boolean hasFocus;

    public AudioFocusHelper(@NonNull Context context, @NonNull RxSpotifyPlayer player) {
        this.player = player;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
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
                player.pause().subscribe(new Action() {
                    @Override
                    public void run() throws Exception {

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {

                    }
                });
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (player.getPlaybackState().isPlaying()) {
                    player.pause().subscribe(new Action() {
                        @Override
                        public void run() throws Exception {
                            shouldPlayOnFocusGain = true;
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {

                        }
                    });
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (!isDucking && shouldPlayOnFocusGain) {
                    player.resume().subscribe(new Action() {
                        @Override
                        public void run() throws Exception {

                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {

                        }
                    });
                } else {
                    player.setVolume(1.0f).subscribe(new Action() {
                        @Override
                        public void run() throws Exception {
                            isDucking = false;
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {

                        }
                    });
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                player.setVolume(0.1f).subscribe(new Action() {
                    @Override
                    public void run() throws Exception {
                        isDucking = true;
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {

                    }
                });
                break;
        }
    }

}
