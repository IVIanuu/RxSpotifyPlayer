package com.ivianuu.rxspotifyplayer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Preconditions
 */
public final class Preconditions {

    private Preconditions() {
        // no instances
    }

    /**
     * Throws a npe if the object is null
     */
    public static void checkNotNull(@Nullable Object o, @NonNull String message) {
        if (o == null) {
            throw new NullPointerException(message);
        }
    }
}