package org.fasola.fasolaminutes;

import android.database.Cursor;
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
 *     <li> {@link #registerObserver(Observer)}
 *     <li> {@link #unregisterObserver(Observer)}
 * </ul>
 */
public class Playlist extends ArrayList<Playlist.Song> {
    /** Cursor position */
    int mPos = -1;

    // Observers
    private final PlaylistObservable mObservable = new PlaylistObservable();

    // Singleton
    static Playlist mInstance;

    /** Returns the {@link Playlist} singleton */
    public static Playlist getInstance() {
        if (mInstance == null)
            mInstance = new Playlist();
        return mInstance;
    }

    /** Song data structure */
    public static class Song {
        public long songId;
        public long leadId;
        public String name;
        public String leaders;
        public String singing;
        public String date;
        public String year;
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
            this.songId = cursor.getLong(0);
            this.leadId = cursor.getLong(1);
            this.name = cursor.getString(2);
            this.leaders = cursor.getString(3);
            this.singing = cursor.getString(4);
            this.date = cursor.getString(5);
            this.year = cursor.getString(6);
            this.url = cursor.getString(7);
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
                C.SongLeader.songId,
                C.SongLeader.leadId,
                C.Song.fullName,
                C.Leader.fullName.func("group_concat", "', '"),
                C.Singing.name,
                C.Singing.startDate,
                C.Singing.year,
                C.SongLeader.audioUrl)
            .from(C.SongLeader)
            .where(column, "IN", args) // This makes for a verbose but efficient query
            .group(C.SongLeader.leadId);
    }

    /** Observer base class */
    public static class Observer {
        /** Override to respond when items are added or removed from the playlist. */
        public void onPlaylistChanged() {}
        /** Override to respond when the playlist cursor moves. */
        public void onCursorChanged() {}
        /** Override to respond to either event. */
        public void onChanged() {}
    }

    /**
     * Registers an observer.
     *
     * @see PlaylistObserver
     */
    public void registerObserver(Observer observer) {
        mObservable.registerObserver(observer);
    }

    /** Unregister a previously registered observer. */
    public void unregisterObserver(Observer observer) {
        mObservable.unregisterObserver(observer);
    }

    /** Returns the cursor position */
    public int getPosition() {
        return mPos;
    }

    /** Is there a previous song? */
    public boolean hasPrevious() {
        return mPos > 0;
    }

    /** Is there a next song? */
    public boolean hasNext() {
        return mPos < size() - 1;
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
            mObservable.notifyCursorChanged();
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

    /** Adds all {@link Song}s in a cursor.
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
        mObservable.notifyPlaylistChanged();
        return true;
    }

    // ArrayList overrides to manage notifications

    @Override
    public boolean add(Song object) {
        if (super.add(object)) {
            mObservable.notifyPlaylistChanged();
            return true;
        }
        return false;
    }

    @Override
    public void add(int index, Song object) {
        super.add(index, object);
        if (index <= mPos) {
            ++mPos;
            mObservable.notifyChanged();
        }
        else {
            mObservable.notifyPlaylistChanged();
        }
    }

    @Override
    public boolean addAll(Collection<? extends Song> collection) {
        if (super.addAll(collection)) {
            mObservable.notifyPlaylistChanged();
            return true;
        }
        return false;
    }

    // Replace all songs in the playlist with these songs (avoids extra notifications)
    public boolean replaceWith(Collection<? extends Song> collection) {
        super.clear();
        boolean result = super.addAll(collection);
        mPos = collection.size() > 0 ? 0 : -1;
        mObservable.notifyChanged();
        return result;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Song> collection) {
        if (super.addAll(index, collection)) {
            if (index <= mPos && ! collection.isEmpty()) {
                mPos += collection.size();
                mObservable.notifyChanged();
            }
            else
                mObservable.notifyPlaylistChanged();
            return true;
        }
        return false;
    }

    @Override
    public Song set(int index, Song object) {
        Song song = super.set(index, object);
        if (song != null)
            mObservable.notifyPlaylistChanged();
        return song;
    }

    @Override
    public Song remove(int index) {
        Song song = super.remove(index);
        if (song != null) {
            if (index <= mPos) {
                --mPos;
                mObservable.notifyChanged();
            }
            else
                mObservable.notifyPlaylistChanged();
        }
        return song;
    }

    public void move(int from, int to) {
        int lastPos = getPosition();
        super.add(to, super.remove(from));
        mObservable.notifyChanged();
        // Update now playing
        if (from == lastPos) // moved playing item
            moveToPosition(to);
        else if (from < lastPos && to >= lastPos) // moved an item from before to after
            moveToPosition(lastPos - 1);
        else if (from > lastPos && to <= lastPos) // moved an item from after to before
            moveToPosition(lastPos + 1);
    }


    @Override
    public void clear() {
        mPos = -1;
        super.clear();
        mObservable.notifyChanged();
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
}
