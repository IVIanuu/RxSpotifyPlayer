package com.ivianuu.rxspotifyplayer.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.ivianuu.rxspotifyplayer.RxSpotifyPlayer;
import com.ivianuu.rxspotifyplayerextensions.AudioFocusHelper;
import com.ivianuu.rxspotifyplayerextensions.ProgressUpdateHelper;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

public class MainActivity extends AppCompatActivity {

    private static final int LOGIN_REQUEST_CODE = 1234;

    private static final String CLIENT_ID = "2eadca5243d5475b9f8003088b2a170b";
    private static final String REDIRECT_URI = "rxspotifyplayer://callback";
    private static final String[] SCOPES = new String[]{ "streaming" };

    private RxSpotifyPlayer player;
    private AudioFocusHelper audioFocusHelper;

    private Button playPauseButton;
    private Button volume;
    private SeekBar progressSeekBar;
    private boolean quieter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playPauseButton = findViewById(R.id.play_pause);
        playPauseButton.setOnClickListener(v -> {
            if (player.getPlaybackState().isPlaying()) {
                player.pause().subscribe();
                audioFocusHelper.abandonFocus(); // abandon focus
            } else {
                if (audioFocusHelper.requestAudioFocus()) {
                    player.resume().subscribe();
                } else {
                    Toast.makeText(MainActivity.this, "Granting focus denied", Toast.LENGTH_SHORT).show();
                }
            }
        });

        volume = findViewById(R.id.volume);
        volume.setOnClickListener(v -> player.setVolume(!quieter ? 0.1f : 1.0f)
                .subscribe(() -> quieter = !quieter));

        progressSeekBar = findViewById(R.id.seek_to);
        progressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                player.seekTo(seekBar.getProgress())
                        .subscribe(() -> {

                        }, throwable -> {

                        });
            }
        });

        player = RxSpotifyPlayer.create(this, CLIENT_ID);

        player.playbackState()
                .subscribe(playbackState -> playPauseButton.setText(playbackState.isPlaying() ? "pause" : "resume"));

        player.errors()
                .subscribe(error -> {});

        player.completion()
                .subscribe(o -> player.play("3yagAUVQNKv1M75u7S1ELW").subscribe(
                        () -> Toast.makeText(
                                MainActivity.this, "playing next track", Toast.LENGTH_SHORT).show()));

        audioFocusHelper = AudioFocusHelper.create(this, player);

        ProgressUpdateHelper.from(player.playbackState())
                .subscribe(playbackProgress -> {
                    progressSeekBar.setMax(playbackProgress.getDuration());
                    progressSeekBar.setProgress(playbackProgress.getProgress());
                });

        AuthenticationRequest request = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI)
                .setShowDialog(false)
                .setScopes(SCOPES)
                .build();

        AuthenticationClient.openLoginActivity(this, LOGIN_REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
            switch (response.getType()) {
                case TOKEN:
                    onAuthComplete(response.getAccessToken());
                    break;
                default:
                    Toast.makeText(this, "Error re launch the app", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void onAuthComplete(String accessToken) {
        if (!audioFocusHelper.requestAudioFocus()) {
            Toast.makeText(this, "no focus", Toast.LENGTH_SHORT).show();
            return;
        }
        player.init(accessToken) // init player
                .andThen(player.play("5atzkSaRuwgXiPDRi9qyKz")) // start playing
                .andThen(player.seekTo(180000)) // seek
                .subscribe(() -> Toast.makeText(
                        MainActivity.this, "Success", Toast.LENGTH_SHORT).show(),
                        Throwable::printStackTrace);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // clean up
        player.release();
        audioFocusHelper.abandonFocus();
    }
}
