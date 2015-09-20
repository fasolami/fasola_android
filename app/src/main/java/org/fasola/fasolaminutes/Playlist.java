package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Playlist of Songs
 *
 * <p>Songs should be added using {@link #add(Cursor)} or {@link #addAll(Cursor)} if possible,
 * using asynchronous queries based on {@link #getSongQuery(Object, Object...)}.
 *
 * <p>Use a "now playing" cursor to move between Songs.
 * <ul>
 *     <li> {@link #moveToFirst()}
 *     <li> {@link #moveToNext()}
 *     <li> {@link #moveToNext()}
 *     <li> {@link #moveToPosition(int)}
 *     <li> {@link #getCurrent()}
 * </ul>
 *
 * <p>Observers can be used to receive notifications on playlist changes and cursor changes.
 * <ul>
 *     <li> {@link #registerObserver(DataSetObserver)}
 *     <li> {@link #unregisterObserver(DataSetObserver)}
 *     <li> {@link #registerPlayingObserver(DataSetObserver)}
 *     <li> {@link #unregisterPlayingObserver(DataSetObserver)}
 * </ul>
 */
public class Playlist extends ArrayList<Playlist.Song> {
    /** Song data structure */
    public static class Song {
        public long leadId;
        public String name;
        public String leaders;
        public String singing;
        public String date;
        public String url;
        public int status;

        public static final int STATUS_OK = 0;
        public static final int STATUS_ERROR = 1;

        /**
         * Constructs a song from a cursor fetched from executing
         * {@link #getSongQuery(Object, Object...)}
         *
         * @param cursor {@code Cursor}
         */
        public Song(Cursor cursor) {
            this.leadId = cursor.getLong(0);
            this.name = cursor.getString(1);
            this.leaders = cursor.getString(2);
            this.singing = cursor.getString(3);
            this.date = cursor.getString(4);
            this.url = cursor.getString(5);
            this.status = STATUS_OK;
        }
    }

    /**
     * Returns a query that can be used to construct a {@link Song} object.
     *
     * <p>Cursors fetched using this query can be passed to {@link #add(Cursor)},
     * or {@link #addAll(Cursor)} to construct and add the {@link Song}
     *
     * @param column column name or {@link SQL.Column} for the WHERE clause
     * @param args   values for the IN predicate for the WHERE clause
     * @return {@link SQL.Query}
     */
    public static SQL.Query getSongQuery(Object column, Object... args) {
        return SQL.select(
                C.SongLeader.leadId, C.Song.fullName,
                C.Leader.fullName.func("group_concat", "', '"),
                C.Singing.name, C.Singing.startDate,
                C.SongLeader.audioUrl)
            .where(column, "IN", args) // This makes for a verbose but efficient query
            .group(C.SongLeader.leadId);
    }

    // Singleton
    static Playlist mInstance;

    /** Returns the {@link Playlist} singleton */
    public static Playlist getInstance() {
        if (mInstance == null)
            mInstance = new Playlist();
        return mInstance;
    }

    /** Cursor position */
    int mPos;

    /** Constructs an empty playlist */
    private Playlist() {
        mPos = -1;
    }

    /** DataSet Observers */
    private final DataSetObservable mObservable = new DataSetObservable();
    private final DataSetObservable mPlayingObservable = new DataSetObservable();

    /** Registers an observer that is notified when Songs are added or removed */
    public void registerObserver(DataSetObserver observer) {
        mObservable.registerObserver(observer);
    }

    /** Removes an observer added by {@link #registerObserver(DataSetObserver)} */
    public void unregisterObserver(DataSetObserver observer) {
        mObservable.unregisterObserver(observer);
    }

    /**
     * Registers an observer that is notified when the cursor position changes
     *
     * @see #getPosition
     * @see #moveToPosition
     */
    public void registerPlayingObserver(DataSetObserver observer) {
        mPlayingObservable.registerObserver(observer);
    }

    /** Removes an observer added by {@link #registerPlayingObserver(DataSetObserver)} */
    public void unregisterPlayingObserver(DataSetObserver observer) {
        mPlayingObservable.unregisterObserver(observer);
    }

    /** Returns the cursor position */
    public int getPosition() {
        return mPos;
    }

    /** Returns the {@link Song} at the cursor or {@code null} */
    public Song getCurrent() {
        if (mPos >= size() || mPos < 0)
            return null;
        return get(mPos);
    }

    /**
     * Moves the cursor to a new position
     *
     * @param pos    new position (0-based)
     * @return {@link Song} at the new position
     *      or {@code null} if {@code pos} is before the beginning or after the end
     */
    public Song moveToPosition(int pos) {
        pos = Math.max(-1, Math.min(pos, size()));
        boolean hasChanged = pos != mPos;
        mPos = pos;
        if (hasChanged)
            mPlayingObservable.notifyChanged();
        return getCurrent();
    }

    /**
     * Moves the cursor to the first Song
     *
     * @return {@link Song}
     */
    public Song moveToFirst() {
        return moveToPosition(0);
    }

    /**
     * Moves the cursor to the next Song
     *
     * @return {@link Song}
     */
    public Song moveToNext() {
        return moveToPosition(mPos + 1);
    }

    /**
     * Moves the cursor to the previous Song
     *
     * @return {@link Song}
     */
    public Song moveToPrev() {
        return moveToPosition(mPos - 1);
    }

    /** Adds a new {@link Song}
     *
     * @param cursor Cursor from {@link #getSongQuery(Object, Object...)}
     * @return always {@code true}
     * @see ArrayList#add(Object)
     */
    public boolean add(Cursor cursor) {
        return add(new Song(cursor));
    }

    /** Adds all {@link Song}s in a cursor
     *
     * <p>Observers are notified only once if the Playlist is changed, not for each song.
     *
     * @param cursor Cursor from {@link #getSongQuery(Object, Object...)}
     * @return {@code true} if there any rows exist in the cursor, {@code false} otherwise
     * @see ArrayList#addAll(Collection)
     */
    public boolean addAll(Cursor cursor) {
        if (! cursor.moveToFirst())
            return false;
        do {
            // Don't notify for every song
            super.add(new Song(cursor));
        } while(cursor.moveToNext());
        return notifyWrapper(true);
    }

    // ArrayList overrides to manage notifications

    @Override
    public boolean add(Song object) {
        return notifyWrapper(super.add(object));
    }

    @Override
    public void add(int index, Song object) {
        super.add(index, object);
        notifyChanged();
        if (index <= mPos) {
            ++mPos;
            mPlayingObservable.notifyChanged();
        }
    }

    @Override
    public boolean addAll(Collection<? extends Song> collection) {
        return notifyWrapper(super.addAll(collection));
    }

    @Override
    public boolean addAll(int index, Collection<? extends Song> collection) {
        if (super.addAll(index, collection)) {
            notifyChanged();
            if (index <= mPos && ! collection.isEmpty()) {
                mPos += collection.size();
                mPlayingObservable.notifyChanged();
            }
            return true;
        }
        return false;
    }

    @Override
    public Song set(int index, Song object) {
        Song song = super.set(index, object);
        if (song != null)
            notifyChanged();
        return song;
    }

    @Override
    public Song remove(int index) {
        Song song = super.remove(index);
        if (song != null) {
            notifyChanged();
            if (index <= mPos) {
                --mPos;
                mPlayingObservable.notifyChanged();
            }
        }
        return song;
    }

    public void move(int from, int to) {
        super.add(to, super.remove(from));
        notifyChanged();
        moveToPosition(to);
    }


    @Override
    public void clear() {
        mPos = -1;
        super.clear();
        mPlayingObservable.notifyChanged();
        notifyChanged();
    }

    // These methods make managing the currentSong pointer tricky, so they are unsupported
    @Override
    public boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    /** Notifies observers that the playlist has changed */
    private void notifyChanged() {
        mObservable.notifyChanged();
    }

    /** Wrapper for functions that should {@link #notifyChanged()} and return a value */
    private boolean notifyWrapper(boolean value) {
        if (value)
            notifyChanged();
        return value;
    }
}
