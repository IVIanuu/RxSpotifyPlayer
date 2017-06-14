package com.ivianuu.rxspotifyplayer;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.spotify.sdk.android.player.AudioController;
import com.spotify.sdk.android.player.AudioRingBuffer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Author IVIanuu.
 */

class VolumeAudioController implements AudioController {

    private static final int AUDIO_BUFFER_SIZE_SAMPLES = 4096;
    private static final int AUDIO_BUFFER_CAPACITY = 81920;
    @Nullable
    private static AudioTrack audioTrack;
    private static float volume = AudioTrack.getMaxVolume();
    private final AudioRingBuffer audioBuffer = new AudioRingBuffer(AUDIO_BUFFER_CAPACITY);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Object playingMutex = new Object();
    private final Runnable audioRunnable = new Runnable() {
        final short[] pendingSamples = new short[AUDIO_BUFFER_SIZE_SAMPLES];

        @Override
        public void run() {
            int itemsRead = audioBuffer.peek(pendingSamples);
            if (itemsRead > 0) {
                int itemsWritten = writeSamplesToAudioOutput(pendingSamples, itemsRead);
                audioBuffer.remove(itemsWritten);
            }

        }
    };
    private int sampleRate;
    private int channels;

    @Override
    public int onAudioDataDelivered(@NonNull short[] samples, int sampleCount, int sampleRate, int channels) {
        if (audioTrack != null && (this.sampleRate != sampleRate || this.channels != channels)) {
            synchronized (playingMutex) {
                audioTrack.release();
                audioTrack = null;
            }
        }

        this.sampleRate = sampleRate;
        this.channels = channels;
        if (audioTrack == null) {
            createAudioTrack(sampleRate, channels);
        }

        try {
            executorService.execute(audioRunnable);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }

        return audioBuffer.write(samples, sampleCount);
    }

    @Override
    public void onAudioFlush() {
        audioBuffer.clear();
        if (audioTrack != null) {
            synchronized (playingMutex) {
                audioTrack.pause();
                audioTrack.flush();
                audioTrack.release();
                audioTrack = null;
            }
        }
    }

    @Override
    public void onAudioPaused() {
        if (audioTrack != null) {
            audioTrack.pause();
        }
    }

    @Override
    public void onAudioResumed() {
        if (audioTrack != null) {
            audioTrack.play();
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        executorService.shutdown();
    }

    private void createAudioTrack(int sampleRate, int channels) {
        byte channelConfig;
        switch (channels) {
            case 0:
                throw new IllegalStateException("Input source has 0 channels");
            case 1:
                channelConfig = 4;
                break;
            case 2:
                channelConfig = 12;
                break;
            default:
                throw new IllegalArgumentException("Unsupported input source has " + channels + " channels");
        }

        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, 2) * 2;
        synchronized (playingMutex) {
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, 2, bufferSize, 1);
            if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioTrack.setVolume(volume);
                } else {
                    audioTrack.setStereoVolume(volume, volume);
                }
                audioTrack.play();
            } else {
                audioTrack.release();
                audioTrack = null;
            }
        }
    }

    private int writeSamplesToAudioOutput(@NonNull short[] samples, int samplesCount) {
        if (isAudioTrackPlaying()) {
            int itemsWritten = audioTrack.write(samples, 0, samplesCount);
            if (itemsWritten > 0) {
                return itemsWritten;
            }
        }

        return 0;
    }

    private boolean isAudioTrackPlaying() {
        return audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
    }

    void setVolume(float volume) {
        VolumeAudioController.volume = volume;
        if (audioTrack != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                audioTrack.setVolume(volume);
            } else {
                audioTrack.setStereoVolume(volume, volume);
            }
        }
    }
}