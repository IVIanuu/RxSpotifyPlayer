package com.ivianuu.rxspotifyplayerextensions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

import com.ivianuu.rxspotifyplayer.PlaybackState;
import com.ivianuu.rxspotifyplayer.RxSpotifyPlayer;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;

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
        return Observable.create(new ObservableOnSubscribe<Object>() {

            private boolean noisyReceiverRegistered;

            @Override
            public void subscribe(@io.reactivex.annotations.NonNull final ObservableEmitter<Object> e) throws Exception {
                final NoisyReceiver noisyReceiver = new NoisyReceiver(player);

                player.playbackState()
                        .takeUntil(new Predicate<PlaybackState>() {
                            @Override
                            public boolean test(@io.reactivex.annotations.NonNull PlaybackState playbackState) throws Exception {
                                return e.isDisposed();
                            }
                        })
                        .doOnComplete(new Action() {
                            @Override
                            public void run() throws Exception {
                                if (noisyReceiverRegistered) {
                                    try {
                                        context.unregisterReceiver(noisyReceiver);
                                    } catch (IllegalStateException e) {
                                        e.printStackTrace(); // catch error
                                    }
                                    noisyReceiverRegistered = false;
                                }
                            }
                        })
                        .subscribe(new Consumer<PlaybackState>() {
                            @Override
                            public void accept(@io.reactivex.annotations.NonNull PlaybackState playbackState) throws Exception {
                                if (playbackState.isPlaying()) {
                                    if (!noisyReceiverRegistered) {
                                        context.registerReceiver(noisyReceiver, noisyIntentFilter());
                                        noisyReceiverRegistered = true;
                                    }
                                } else {
                                    if (noisyReceiverRegistered) {
                                        try {
                                            context.unregisterReceiver(noisyReceiver);
                                        } catch (IllegalStateException e) {
                                            e.printStackTrace(); // catch error
                                        }
                                        noisyReceiverRegistered = false;
                                    }
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
            player.pause().subscribe(new Action() {
                @Override
                public void run() throws Exception {

                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {

                }
            });
        }
    }
}
