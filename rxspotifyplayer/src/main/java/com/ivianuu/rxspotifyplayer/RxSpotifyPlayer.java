package com.ivianuu.rxspotifyplayer;

import android.content.Context;
import android.net.NetworkInfo;
import android.support.annotation.CheckResult;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
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
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

import static com.ivianuu.preconditions.Preconditions.checkArgumentInRange;
import static com.ivianuu.preconditions.Preconditions.checkNonnegative;
import static com.ivianuu.preconditions.Preconditions.checkNotNull;

/**
 * Rx spotify player
 */
public final class RxSpotifyPlayer {

    private static final String URI_PREFIX = "spotify:track:";

    private final Context context;
    private final String clientId;

    private PublishSubject<Object> completionSubject = PublishSubject.create();
    private PublishSubject<Error> errorsSubject = PublishSubject.create();
    private BehaviorSubject<PlaybackState> playbackStateSubject = BehaviorSubject.createDefault(PlaybackState.extractFromPlayer(null));

    private SpotifyPlayer player;
    private VolumeAudioController audioController;

    private PlaybackState playbackState;

    private RxSpotifyPlayer(Context context, String clientId) {
        this.context = context;
        this.clientId = clientId;

        audioController = new VolumeAudioController();
    }

    /**
     * Returns a new rx spotify player
     */
    @NonNull
    public static RxSpotifyPlayer create(@NonNull Context context, @NonNull String clientId) {
        checkNotNull(context, "context == null");
        checkNotNull(clientId, "clientId == null");
        return new RxSpotifyPlayer(context, clientId);
    }

    // INIT

    /**
     * Initializes the player
     */
    @CheckResult @NonNull
    public Completable init(@NonNull final String accessToken) {
        checkNotNull(accessToken, "accessToken == null");
        return Completable.create(e -> init(e, accessToken));
    }

    private void init(final CompletableEmitter e, String accessToken) {
        if (isInitialized() && !e.isDisposed()) {
            // we only need to init once
            e.onComplete();
            return;
        }

        // config
        Config config = new Config(
                context, accessToken, clientId);

        // add audio controller
        SpotifyPlayer.Builder builder = new SpotifyPlayer.Builder(config)
                .setAudioController(audioController);

        // request player
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
     */
    @CheckResult @NonNull
    public Completable play(@NonNull String playContext) {
        checkNotNull(playContext, "playContext == null");
        final String uri;
        if (!playContext.contains(URI_PREFIX)) {
            uri = URI_PREFIX + playContext;
        } else {
            uri = playContext;
        }
        return Completable.create(e -> {
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
        });
    }

    // PAUSE

    /**
     * Pauses the current playback
     */
    @CheckResult @NonNull
    public Completable pause() {
        return Completable.create(e -> {
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
        });
    }

    // RESUME

    /**
     * Resumes the playback
     */
    @CheckResult @NonNull
    public Completable resume() {
        return Completable.create(e -> {
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
        });
    }

    // TOGGLE PAUSE

    /**
     * Resumes or pauses the playback based on the playback state
     */
    @CheckResult @NonNull
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
    @CheckResult @NonNull
    public Completable seekTo(@IntRange(from = 0) final int position) {
        checkNonnegative(position, "position must be 0 or greater");
        return Completable.create(e -> {
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
        });
    }

    // VOLUME

    /**
     * Sets the volume
     */
    @CheckResult @NonNull
    public Completable setVolume(@FloatRange(from = 0f, to = 1f) final float volume) {
        checkArgumentInRange(volume, 0f, 1f, "volume");
        return Completable.create(e -> {
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
        });
    }

    // CONNECTIVITY

    /**
     * Sets the connectivity
     */
    @CheckResult @NonNull
    public Completable setConnectivity(@NonNull final NetworkInfo info) {
        checkNotNull(info, "info == null");
        return Completable.create(e -> {
            if (isInitialized()) {
                Connectivity connectivity;
                if (info.isConnected()) {
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
        });
    }

    // BITRATE

    /**
     * Sets the playback bitrate
     */
    @CheckResult @NonNull
    public Completable setPlaybackBitrate(@NonNull final PlaybackBitrate playbackBitrate) {
        checkNotNull(playbackBitrate, "playbackBitrate == null");
        return Completable.create(e -> {
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
        });
    }

    /**
     * Returns whether the player is initialized
     */
    public boolean isInitialized() {
        return player != null && player.isLoggedIn();
    }

    // PLAYBACK STATE

    /**
     * Emits when the playback state changes
     */
    @CheckResult @NonNull
    public Observable<PlaybackState> playbackState() { return playbackStateSubject; }

    /**
     * Returns the last known playback state
     */
    @NonNull
    public PlaybackState getPlaybackState() {
        if (playbackState == null) {
            playbackState = PlaybackState.extractFromPlayer(player);
        }
        return playbackState;
    }

    // COMPLETION

    /**
     * Emits when a track completes
     */
    @CheckResult @NonNull
    public Observable<Object> completion() {
        return completionSubject;
    }

    // ERRORS

    /**
     * Emits on every playback error
     */
    @CheckResult @NonNull
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
                        completionSubject.onNext(Notification.INSTANCE);
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
