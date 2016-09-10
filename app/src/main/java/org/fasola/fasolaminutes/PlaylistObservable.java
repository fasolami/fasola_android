/*
 * This file is part of FaSoLa Minutes for Android.
 * Copyright (c) 2016 Mike Richards. All rights reserved.
 */

package org.fasola.fasolaminutes;

import android.database.Observable;

/** Observable subclass for {@link PlaylistObserver}s. */
public class PlaylistObservable extends Observable<Playlist.Observer> {
    /** Notify that the playlist has changed. */
    public void notifyPlaylistChanged() {
        for (int i=mObservers.size()-1; i>=0; --i) {
            mObservers.get(i).onPlaylistChanged();
            mObservers.get(i).onChanged();
        }
    }

    /** Notify that the now playing cursor has changed. */
    public void notifyCursorChanged() {
        for (int i=mObservers.size()-1; i>=0; --i) {
            mObservers.get(i).onCursorChanged();
            mObservers.get(i).onChanged();
        }
    }

    /** Notify that both playlist and cursor have changed. */
    public void notifyChanged() {
        for (int i=mObservers.size()-1; i>=0; --i) {
            mObservers.get(i).onPlaylistChanged();
            mObservers.get(i).onCursorChanged();
            mObservers.get(i).onChanged();
        }
    }
}
