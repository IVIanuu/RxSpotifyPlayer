package com.ivianuu.rxspotifyplayer;

import android.content.Context;
import android.net.NetworkInfo;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;

import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Connectivity;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.PlaybackBitrate;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

/**
 * @author Manuel Wrage (IVIanuu)
 */
public class RxSpotifyPlayer {

    private static final String URI_PREFIX = "spotify:track:";

    private Context context;
    private String clientId;

    private SpotifyPlayer player;
    private VolumeAudioController audioController;

    private PlaybackState playbackState;

    public RxSpotifyPlayer(@NonNull Context context, @NonNull String clientId) {
        this.context = context;
        this.clientId = clientId;

        audioController = new VolumeAudioController();

        // initial value
        playbackState = getPlaybackState();
        playbackStateSubject.onNext(playbackState);
    }

    // INIT

    /**
     * Initializes the player
     */
    public Completable init(@NonNull final String accessToken) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull CompletableEmitter e) throws Exception {
                init(e, accessToken);
            }
        });
    }

    private void init(final CompletableEmitter e, String accessToken) {
        if (isInitialized() && !e.isDisposed()) {
            // we only need to init once
            e.onComplete();
            return;
        }

        Config config = new Config(
                context, accessToken, clientId);

        SpotifyPlayer.Builder builder = new SpotifyPlayer.Builder(config)
                .setAudioController(audioController);

        Spotify.getPlayer(
                builder, this, new SpotifyPlayer.InitializationObserver() {
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer) {
                        player = spotifyPlayer;

                        // we need to wait for login in order to play tracks
                        player.addConnectionStateCallback(new ConnectionStateCallback() {
                            @Override
                            public void onLoggedIn() {
                                // ready to play
                                if (!e.isDisposed()) {
                                    e.onComplete();
                                }
                                player.removeConnectionStateCallback(this); // clean
                                player.addNotificationCallback(notificationCallback); // add notification callback
                            }

                            @Override
                            public void onLoginFailed(Error error) {
                                // something goes wrong
                                if (!e.isDisposed()) {
                                    e.onError(new Throwable(error.name()));
                                }
                                player.removeConnectionStateCallback(this); // clean;
                            }

                            @Override public void onLoggedOut() {} // ignore

                            @Override public void onTemporaryError() {} // ignore

                            @Override public void onConnectionMessage(String s) {}  // ignore
                        });
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        // error
                        if (!e.isDisposed()) {
                            e.onError(throwable);
                        }
                    }
                });
    }

    // RELEASE

    /**
     * Releases the player
     */
    public void release() {
        // Clean up The player
        if (player != null) {
            player.logout();
            player.removeNotificationCallback(notificationCallback);
            Spotify.destroyPlayer(this);
        }

        player = null;
    }

    // PLAY

    /**
     * Plays the spotify id or uri
     * @param playContext you can pass a track uri or id
     */
    public Completable play(@NonNull String playContext) {
        final String uri;
        if (!playContext.contains(URI_PREFIX)) {
            uri = URI_PREFIX + playContext;
        } else {
            uri = playContext;
        }
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull final CompletableEmitter e) throws Exception {
                if (isInitialized()) {
                    player.playUri(new Player.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            if (!e.isDisposed()) {
                                e.onComplete();
                            }
                        }

                        @Override
                        public void onError(Error error) {
                            if (!e.isDisposed()) {
                                e.onError(new Throwable(error.name()));
                            }
                        }
                    }, uri, 0, 0);
                } else {
                    if (!e.isDisposed()) {
                        e.onError(new Throwable(Error.kSpErrorUninitialized.name()));
                    }
                }
            }
        });
    }

    // PAUSE

    /**
     * Pauses the current playback
     */
    public Completable pause() {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull final CompletableEmitter e) throws Exception {
                if (isInitialized()) {
                    player.pause(new Player.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            if (!e.isDisposed()) {
                                e.onComplete();
                            }
                        }

                        @Override
                        public void onError(Error error) {
                            if (!e.isDisposed()) {
                                e.onError(new Throwable(error.name()));
                            }
                        }
                    });
                } else {
                    if (!e.isDisposed()) {
                        e.onError(new Throwable(Error.kSpErrorUninitialized.name()));
                    }
                }
            }
        });
    }

    // RESUME

    /**
     * Resumes the playback
     */
    public Completable resume() {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull final CompletableEmitter e) throws Exception {
                if (isInitialized()) {
                    player.resume(new Player.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            if (!e.isDisposed()) {
                                e.onComplete();
                            }
                        }

                        @Override
                        public void onError(Error error) {
                            if (!e.isDisposed()) {
                                e.onError(new Throwable(error.name()));
                            }
                        }
                    });
                } else {
                    if (!e.isDisposed()) {
                        e.onError(new Throwable(Error.kSpErrorUninitialized.name()));
                    }
                }
            }
        });
    }

    // TOGGLE PAUSE

    /**
     * Resumes or pauses the playback based on the playback state
     */
    public Completable playPause() {
        if (getPlaybackState().isPlaying()) {
            return pause();
        } else {
            return resume();
        }
    }

    // SEEK

    /**
     * Seeks to the specified position
     */
    public Completable seekTo(final int position) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull final CompletableEmitter e) throws Exception {
                if (isInitialized()) {
                    player.seekToPosition(new Player.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            if (!e.isDisposed()) {
                                e.onComplete();
                            }
                            playbackStateSubject.onNext(PlaybackState.extractFromPlayer(player));
                        }

                        @Override
                        public void onError(Error error) {
                            if (!e.isDisposed()) {
                                e.onError(new Throwable(error.name()));
                            }
                        }
                    }, position);
                } else {
                    if (!e.isDisposed()) {
                        e.onError(new Throwable(Error.kSpErrorUninitialized.name()));
                    }
                }
            }
        });
    }

    // VOLUME

    /**
     * Sets the volume
     */
    public Completable setVolume(@FloatRange(from = 0.0f, to = 1.0f) final float volume) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull CompletableEmitter e) throws Exception {
                if (isInitialized()) {
                    audioController.setVolume(volume);
                    if (!e.isDisposed()) {
                        e.onComplete();
                    }
                } else {
                    if (!e.isDisposed()) {
                        e.onError(new Throwable(Error.kSpErrorUninitialized.name()));
                    }
                }
            }
        });
    }

    // CONNECTIVITY

    /**
     * Sets the connectivity
     */
    public Completable setConnectivity(final NetworkInfo info) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull final CompletableEmitter e) throws Exception {
                if (isInitialized()) {
                    Connectivity connectivity;
                    if (info != null && info.isConnected()) {
                        connectivity = Connectivity.fromNetworkType(info.getType());
                    } else {
                        connectivity = Connectivity.OFFLINE;
                    }
                    player.setConnectivityStatus(new Player.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            if (!e.isDisposed()) {
                                e.onComplete();
                            }
                        }

                        @Override
                        public void onError(Error error) {
                            if (!e.isDisposed()) {
                                e.onError(new Throwable(error.name()));
                            }
                        }
                    }, connectivity);
                } else {
                    if (!e.isDisposed()) {
                        e.onError(new Throwable(Error.kSpErrorUninitialized.name()));
                    }
                }
            }
        });
    }

    // BITRATE

    /**
     * Sets the playback bitrate
     */
    public Completable setPlaybackBitrate(final PlaybackBitrate playbackBitrate) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull final CompletableEmitter e) throws Exception {
                if (isInitialized()) {
                    player.setPlaybackBitrate(new Player.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            if (!e.isDisposed()) {
                                e.onComplete();
                            }
                        }

                        @Override
                        public void onError(Error error) {
                            if (!e.isDisposed()) {
                                e.onError(new Throwable(error.name()));
                            }
                        }
                    }, playbackBitrate);
                } else {
                    if (!e.isDisposed()) {
                        e.onError(new Throwable(Error.kSpErrorUninitialized.name()));
                    }
                }
            }
        });
    }

    public boolean isInitialized() {
        return player != null && player.isLoggedIn();
    }

    // PLAYBACK STATE

    private BehaviorSubject<PlaybackState> playbackStateSubject = BehaviorSubject.create();
    /**
     * Emits when the playback state changes
     */
    public Observable<PlaybackState> playbackState() { return playbackStateSubject; }

    /**
     * Returns the last known playback state
     */
    public PlaybackState getPlaybackState() {
        if (playbackState == null) {
            playbackState = PlaybackState.extractFromPlayer(player);
        }
        return playbackState;
    }

    // COMPLETION

    private PublishSubject<Object> completionSubject = PublishSubject.create();
    /**
     * Emits when a track completes
     */
    public Observable<Object> completion() {
        return completionSubject;
    }

    // ERRORS

    private PublishSubject<Error> errorsSubject = PublishSubject.create();
    /**
     * Emits on every playback error
     */
    public Observable<Error> errors() { return errorsSubject; }

    // CALLBACKS

    private boolean pendingChange;
    private SpotifyPlayer.NotificationCallback notificationCallback = new Player.NotificationCallback() {
        @Override
        public void onPlaybackEvent(PlayerEvent playerEvent) {
            if (player == null || !player.isLoggedIn()) return; // ignore

            switch (playerEvent) {
                case kSpPlaybackNotifyAudioDeliveryDone:
                    if (player.getPlaybackState() != null && !player.getPlaybackState().isPlaying
                            && player.getMetadata() != null && player.getMetadata().currentTrack != null) {
                        // track has completed
                        completionSubject.onNext(new Object());
                    }
                    break;
                case kSpPlaybackNotifyPlay:
                case kSpPlaybackNotifyPause:
                    // update playback state here only if we have meta data
                    if (player.getMetadata() != null && player.getMetadata().currentTrack != null) {
                        playbackState = PlaybackState.extractFromPlayer(player);
                        playbackStateSubject.onNext(playbackState);
                    } else {
                        // otherwise set pending change
                        pendingChange = true;
                    }
                    break;
                case kSpPlaybackNotifyMetadataChanged:
                    // now we can create a valid playback state
                    if (pendingChange) {
                        playbackState = PlaybackState.extractFromPlayer(player);
                        playbackStateSubject.onNext(playbackState);
                        pendingChange = false;
                    }
                    break;
            }
        }

        @Override
        public void onPlaybackError(Error error) {
            errorsSubject.onNext(error);
        }
    };

}
