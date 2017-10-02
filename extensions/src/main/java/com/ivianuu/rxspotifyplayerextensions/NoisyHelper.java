package com.ivianuu.rxspotifyplayerextensions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

import com.ivianuu.rxspotifyplayer.RxSpotifyPlayer;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

import static com.ivianuu.rxspotifyplayer.Preconditions.checkNotNull;

/**
 * Noisy helper
 */
public final class NoisyHelper {

    private NoisyHelper() {
        // no instances
    }

    /**
     * Automatically pauses the playback on becoming noisy
     */
    @CheckResult @NonNull
    public static Observable<Object> with(@NonNull final Context context, @NonNull final RxSpotifyPlayer player) {
        checkNotNull(context, "context == null");
        checkNotNull(player, "player == null");
        return Observable.create(new ObservableOnSubscribe<Object>() {

            private boolean noisyReceiverRegistered;

            @Override
            public void subscribe(@io.reactivex.annotations.NonNull final ObservableEmitter<Object> e) throws Exception {
                final NoisyReceiver noisyReceiver = new NoisyReceiver(player);

                player.playbackState()
                        .takeUntil(playbackState -> {
                            return e.isDisposed();
                        })
                        .doOnComplete(() -> {
                            if (noisyReceiverRegistered) {
                                try {
                                    context.unregisterReceiver(noisyReceiver);
                                } catch (IllegalStateException e1) {
                                    e1.printStackTrace(); // catch error
                                }
                                noisyReceiverRegistered = false;
                            }
                        })
                        .subscribe(playbackState -> {
                            if (playbackState.isPlaying()) {
                                if (!noisyReceiverRegistered) {
                                    context.registerReceiver(noisyReceiver, noisyIntentFilter());
                                    noisyReceiverRegistered = true;
                                }
                            } else {
                                if (noisyReceiverRegistered) {
                                    try {
                                        context.unregisterReceiver(noisyReceiver);
                                    } catch (IllegalStateException e12) {
                                        e12.printStackTrace(); // catch error
                                    }
                                    noisyReceiverRegistered = false;
                                }
                            }
                        });
            }
        });
    }

    private static IntentFilter noisyIntentFilter() {
        return new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    }

    private static final class NoisyReceiver extends BroadcastReceiver {

        private RxSpotifyPlayer player;

        private NoisyReceiver(@NonNull RxSpotifyPlayer player) {
            this.player = player;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            player.pause().subscribe(() -> {

            }, throwable -> {

            });
        }
    }
}
