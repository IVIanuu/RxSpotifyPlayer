package com.ivianuu.rxspotifyplayerextensions;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import com.ivianuu.rxspotifyplayer.PlaybackState;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;

/**
 * @author Manuel Wrage (IVIanuu)
 */
public class ProgressUpdateHelper extends Handler {

    private static final int MESSAGE_REFRESH_PROGRESS_VIEWS = 1;

    private static final int UPDATE_INTERVAL_PAUSED = 500;
    private static final int UPDATE_INTERVAL_PLAYING = 1000;
    private static final int MIN_INTERVAL = 20;

    private PlaybackState playbackState;

    public ProgressUpdateHelper(Observable<PlaybackState> playbackState) {
        playbackState.subscribe(new Consumer<PlaybackState>() {
            @Override
            public void accept(@io.reactivex.annotations.NonNull PlaybackState playbackState) throws Exception {
                ProgressUpdateHelper.this.playbackState = playbackState;
            }
        });
    }

    private void queueNextRefresh(long delay) {
        Message obtainMessage = obtainMessage(MESSAGE_REFRESH_PROGRESS_VIEWS);
        removeMessages(MESSAGE_REFRESH_PROGRESS_VIEWS);
        sendMessageDelayed(obtainMessage, delay);
    }

    private int refreshProgressViews() {
        int duration = playbackState.getDuration();
        int progress = playbackState.getEstimatedProgress();

        playbackProgressSubject.onNext(new PlaybackProgress(duration, progress));
        return !playbackState.isPlaying() ? UPDATE_INTERVAL_PAUSED : Math.max(MIN_INTERVAL, 1000 - (progress % UPDATE_INTERVAL_PLAYING));
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        if (msg.what == MESSAGE_REFRESH_PROGRESS_VIEWS) {
            queueNextRefresh((long) refreshProgressViews());
        }
    }

    private final BehaviorSubject<PlaybackProgress> playbackProgressSubject = BehaviorSubject.create();
    public Observable<PlaybackProgress> playbackProgress() {
        return playbackProgressSubject;
    }

}
