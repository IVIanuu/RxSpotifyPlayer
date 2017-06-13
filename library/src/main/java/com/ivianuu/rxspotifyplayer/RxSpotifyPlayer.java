package com.ivianuu.rxspotifyplayer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.SpotifyPlayer;

/**
 * @author Manuel Wrage (IVIanuu)
 */
public class RxSpotifyPlayer implements SpotifyPlayer.NotificationCallback, ConnectionStateCallback {

    private Context context;
    private String clientId;

    private SpotifyPlayer player;

    public RxSpotifyPlayer(@NonNull Context context, @NonNull String clientId) {
        this.context = context;
        this.clientId = clientId;
    }

    // CALLBACKS

    @Override
    public void onLoggedIn() {

    }

    @Override
    public void onLoggedOut() {

    }

    @Override
    public void onLoginFailed(Error error) {

    }

    @Override
    public void onTemporaryError() {

    }

    @Override
    public void onConnectionMessage(String s) {

    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {

    }

    @Override
    public void onPlaybackError(Error error) {

    }
}
