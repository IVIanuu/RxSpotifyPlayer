package com.ivianuu.rxspotifyplayerextensions;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

import com.ivianuu.rxspotifyplayer.PlaybackState;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

/**
 * Progress update helper
 */
public final class ProgressUpdateHelper {

    private ProgressUpdateHelper() {
        // no instances
    }

    /**
     * Returns an observable which loops in a 1 second interval
     */
    @CheckResult @NonNull
    public static Observable<PlaybackProgress> from(@NonNull final Observable<PlaybackState> playbackStateObservable) {
        return Observable.create(new ObservableOnSubscribe<PlaybackProgress>() {

            private PlaybackState playbackState;

            @Override
            public void subscribe(@NonNull final ObservableEmitter<PlaybackProgress> e) throws Exception {
                // subscribe
                playbackStateObservable
                        .takeUntil(new Predicate<PlaybackState>() {
                            @Override
                            public boolean test(@NonNull PlaybackState playbackState) throws Exception {
                                return e.isDisposed(); // complete if disposed
                            }
                        })
                        .subscribe(new Consumer<PlaybackState>() {
                            @Override
                            public void accept(@NonNull PlaybackState newState) throws Exception {
                                playbackState = newState; // update playback state
                            }
                        });

                // loop
                Observable.interval(1000, TimeUnit.MILLISECONDS)
                        .takeUntil(new Predicate<Long>() {
                            @Override
                            public boolean test(@NonNull Long aLong) throws Exception {
                                return e.isDisposed(); // complete if disposed
                            }
                        })
                        .map(new Function<Long, PlaybackProgress>() {
                            @Override
                            public PlaybackProgress apply(@NonNull Long aLong) throws Exception {
                                // create object
                                return new PlaybackProgress(playbackState.getDuration(), playbackState.getEstimatedProgress());
                            }
                        })
                        .subscribe(new Consumer<PlaybackProgress>() {
                            @Override
                            public void accept(@NonNull PlaybackProgress playbackProgress) throws Exception {
                                // pass to emitter
                                e.onNext(playbackProgress);
                            }
                        });
            }
        });
    }
}
