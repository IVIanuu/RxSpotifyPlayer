package com.ivianuu.rxspotifyplayerextensions;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

import com.ivianuu.rxspotifyplayer.PlaybackState;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

import static com.ivianuu.rxspotifyplayer.Preconditions.checkNotNull;

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
        checkNotNull(playbackStateObservable, "playbackStateObservable == null");
        return Observable.create(new ObservableOnSubscribe<PlaybackProgress>() {

            private PlaybackState playbackState;

            @Override
            public void subscribe(@NonNull final ObservableEmitter<PlaybackProgress> e) throws Exception {
                // subscribe
                playbackStateObservable
                        .takeUntil(playbackState -> {
                            return e.isDisposed(); // complete if disposed
                        })
                        .subscribe(newState -> {
                            playbackState = newState; // update playback state
                        });

                // loop
                // pass to emitter
                Observable.interval(1000, TimeUnit.MILLISECONDS)
                        .takeUntil(aLong -> {
                            return e.isDisposed(); // complete if disposed
                        })
                        .map(aLong -> {
                            // create object
                            return new PlaybackProgress(playbackState.getDuration(), playbackState.getEstimatedProgress());
                        })
                        .subscribe(e::onNext);
            }
        });
    }
}
