package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Playlist of Songs
 */
public class Playlist extends ArrayList<Playlist.Song> {
    // Song struct
    public static class Song {
        public long leadId;
        public String name;
        public String leaders;
        public String singing;
        public String date;
        public String url;

        private Song(long leadId, String name, String leaders, String singing, String date, String url) {
            this.leadId = leadId;
            this.name = name;
            this.leaders = leaders;
            this.singing = singing;
            this.date = date;
            this.url = url;
        }
    }

    int mPos;

    // Singleton
    static Playlist mInstance;
    public static Playlist getInstance() {
        if (mInstance == null)
            mInstance = new Playlist();
        return mInstance;
    }

    private Playlist() {
        mPos = -1;
    }

    // Data observer
    private final DataSetObservable mDataSetObservable = new DataSetObservable();
    private final DataSetObservable mPlayingObservable = new DataSetObservable();

    // Data observer
    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    // Now playing observer
    public void registerPlayingObserver(DataSetObserver observer) {
        mPlayingObservable.registerObserver(observer);
    }

    public void unregisterPlayingObserver(DataSetObserver observer) {
        mPlayingObservable.unregisterObserver(observer);
    }

    // Current Song
    public int getPosition() {
        return mPos;
    }

    public Song getCurrent() {
        return get(mPos);
    }

    public Song moveToPosition(int i) {
        i = Math.max(-1, Math.min(i, size()));
        boolean hasChanged = i != mPos;
        mPos = i;
        if (hasChanged)
            mPlayingObservable.notifyChanged();
        if (mPos >= size() || mPos < 0)
            return null;
        return get(mPos);
    }

    public Song moveToFirst() {
        return moveToPosition(0);
    }

    public Song moveToNext() {
        return moveToPosition(mPos + 1);
    }

    // Add overrides for cursor objects
    public boolean add(Cursor cursor) {
        return add(songFromCursor(cursor));
    }

    public boolean addAll(Cursor cursor) {
        if (! cursor.moveToFirst())
            return false;
        do {
            // Don't notify for every song
            super.add(songFromCursor(cursor));
        } while(cursor.moveToNext());
        return notifyChanged(true);
    }

    // Overrides to maintain current song pointer and notify on data changed
    private void notifyChanged() {
        mDataSetObservable.notifyChanged();
    }

    private boolean notifyChanged(boolean value) {
        if (value)
            notifyChanged();
        return value;
    }

    private Song notifyChanged(Song value) {
        notifyChanged();
        return value;
    }

    @Override
    public boolean add(Song object) {
        return notifyChanged(super.add(object));
    }

    @Override
    public void add(int index, Song object) {
        if (index <= mPos) {
            ++mPos;
            mPlayingObservable.notifyChanged();
        }
        super.add(index, object);
        notifyChanged();
    }

    @Override
    public boolean addAll(Collection<? extends Song> collection) {
        return notifyChanged(super.addAll(collection));
    }

    @Override
    public boolean addAll(int index, Collection<? extends Song> collection) {
        if (index <= mPos && ! collection.isEmpty()) {
            mPos += collection.size();
            mPlayingObservable.notifyChanged();
        }
        return notifyChanged(super.addAll(index, collection));
    }

    @Override
    public Song set(int index, Song object) {
        return notifyChanged(super.set(index, object));
    }

    @Override
    public Song remove(int index) {
        if (index <= mPos) {
            --mPos;
            mPlayingObservable.notifyChanged();
        }
        return notifyChanged(super.remove(index));
    }

    @Override
    public void clear() {
        mPos = -1;
        mPlayingObservable.notifyChanged();
        super.clear();
        mDataSetObservable.notifyChanged();
    }

    // A few methods that make managing the currentSong pointer tricky (which are now unsuppprted)
    @Override
    public boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    // Return a query that can be used to construct the song object
    public static SQL.Query getSongQuery(Object column, Object... args) {
        return SQL.select(
                C.SongLeader.leadId, C.Song.fullName,
                C.Leader.fullName.func("group_concat", "', '"),
                C.Singing.name, C.Singing.startDate,
                C.SongLeader.audioUrl)
                .where(column, "IN", args)
            .group(column);
    }

    // Create a song from a cursor returned from executing getSongQuery()
    private static Song songFromCursor(Cursor cursor) {
        return new Song(cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getString(5));
    }
}
