package com.ivianuu.rxspotifyplayer.sample;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.ivianuu.rxspotifyplayer.PlaybackState;
import com.ivianuu.rxspotifyplayer.RxSpotifyPlayer;
import com.ivianuu.rxspotifyplayerextensions.PlaybackProgress;
import com.ivianuu.rxspotifyplayerextensions.ProgressUpdateHelper;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.PlaybackBitrate;

import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    private static final int LOGIN_REQUEST_CODE = 1234;

    private static final String CLIENT_ID = "2eadca5243d5475b9f8003088b2a170b";
    private static final String REDIRECT_URI = "rxspotifyplayer://callback";
    private static final String[] SCOPES = new String[]{"streaming"};

    private RxSpotifyPlayer player;

    private Button playPauseButton;
    private Button volume;
    private SeekBar progressSeekBar;
    private boolean quieter;

    private PlaybackState playbackState = PlaybackState.extractFromPlayer(null);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playPauseButton = (Button) findViewById(R.id.play_pause);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.playPause().subscribe();
            }
        });

        volume = (Button) findViewById(R.id.volume);
        volume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.setVolume(!quieter ? 0.1f : 1.0f)
                        .subscribe(new Action() {
                            @Override
                            public void run() throws Exception {
                                quieter = !quieter;
                            }
                        });
            }
        });

        progressSeekBar = (SeekBar) findViewById(R.id.seek_to);
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
                        .subscribe(new Action() {
                            @Override
                            public void run() throws Exception {

                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {

                            }
                        });
            }
        });

        player = new RxSpotifyPlayer(this, CLIENT_ID);

        ProgressUpdateHelper progressUpdateHelper = new ProgressUpdateHelper(player.playbackState());
        progressUpdateHelper.playbackProgress()
                .subscribe(new Consumer<PlaybackProgress>() {
                    @Override
                    public void accept(@NonNull PlaybackProgress playbackProgress) throws Exception {
                        progressSeekBar.setMax(playbackProgress.getDuration());
                        progressSeekBar.setProgress(playbackProgress.getProgress());
                    }
                });

        player.playbackState()
                .subscribe(new Consumer<PlaybackState>() {
                    @Override
                    public void accept(@NonNull PlaybackState playbackState) throws Exception {
                        MainActivity.this.playbackState = playbackState;
                        playPauseButton.setText(playbackState.isPlaying() ? "pause" : "resume");
                    }
                });

        player.completion()
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(@NonNull Object o) throws Exception {
                        player.play("3yagAUVQNKv1M75u7S1ELW").subscribe(new Action() {
                            @Override
                            public void run() throws Exception {
                                Toast.makeText(MainActivity.this, "playing next track", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
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
        player.init(accessToken)
                .andThen(player.play("5atzkSaRuwgXiPDRi9qyKz")) // start playing
                .andThen(player.seekTo(380000)) // seek
                .subscribe(new Action() {
                    @Override
                    public void run() throws Exception {
                        Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                progressSeekBar.setMax(playbackState.getDuration());
                                progressSeekBar.setProgress(playbackState.getEstimatedProgress());
                                handler.postDelayed(this, 1000);
                            }
                        }, 1000);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // clean up
        player.release();
    }
}
