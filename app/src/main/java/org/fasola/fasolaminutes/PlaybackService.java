package org.fasola.fasolaminutes;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlaybackService extends Service
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
                   MediaPlayer.OnErrorListener {

    private static final String TAG = "PlaybackService";

    public static final String ACTION_PLAY = "org.fasola.fasolaminutes.media.PLAY";
    public static final String ACTION_ENQUEUE = "org.fasola.fasolaminutes.media.ENQUEUE";
    public static final String EXTRA_LEAD_ID = "org.fasola.fasolaminutes.media.EXTRA_LEAD_ID";
    public static final String EXTRA_URL = "org.fasola.fasolaminutes.media.EXTRA_URL";
    private static final int NOTIFICATION_ID = 1;
    MediaPlayer mMediaPlayer;
    NotificationManager mNotificationManager;
    boolean mHasNotification;

    // Song struct
    public static class Song {
        public long leadId;
        public String name;
        public String leaders;
        public String singing;
        public String date;
        public String url;

        public Song(long leadId, String name, String leaders, String singing, String date, String url) {
            this.leadId = leadId;
            this.name = name;
            this.leaders = leaders;
            this.singing = singing;
            this.date = date;
            this.url = url;
        }

        // Create a song from a cursor returned from executing Song.getQuery()
        private Song(Cursor cursor) {
            this(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                 cursor.getString(4), cursor.getString(5));
        }

        // Return a query that can be used to construct the song object
        private static SQL.Query getQuery(Object column) {
            return SQL.select(
                    C.SongLeader.leadId, C.Song.fullName,
                    C.Leader.fullName.func("group_concat", "', '"),
                    C.Singing.name, C.Singing.startDate,
                    C.SongLeader.audioUrl)
                    .whereEq(column)
                .group(column);
        }
    }

    List<Song> mPlaylist;
    static PlaybackService mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mPlaylist = new ArrayList<>();
    }

    static PlaybackService getInstance() {
        return mInstance;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() == null)
            return START_STICKY;
        else if (intent.getAction().equals(ACTION_PLAY)) {
            mPlaylist.clear();
            if (intent.hasExtra(EXTRA_LEAD_ID))
                startLead(intent.getLongExtra(EXTRA_LEAD_ID, -1));
            else if (intent.hasExtra(EXTRA_URL))
                startLead(intent.getStringExtra(EXTRA_URL));
        }
        else if (intent.getAction().equals(ACTION_ENQUEUE)) {
            if (intent.hasExtra(EXTRA_LEAD_ID))
                enqueueLead(intent.getLongExtra(EXTRA_LEAD_ID, -1));
            else if (intent.hasExtra(EXTRA_URL))
                enqueueLead(intent.getStringExtra(EXTRA_URL));
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
       if (mMediaPlayer != null)
           mMediaPlayer.release();
        mInstance = null;
    }

    private final IBinder mBinder = new MediaBinder();

    public class MediaBinder extends Binder {
        PlaybackService getService() {
            return PlaybackService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void ensurePlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
        }
    }

    public List<Song> getPlaylist() {
        return mPlaylist;
    }

    /**
     * Play the next song in the playlist
     * @return true if there is a song to play; false if the playlist is empty
     */
    public boolean playNext() {
        // Get the song
        if (mPlaylist.isEmpty())
            return false;
        Song song = mPlaylist.remove(0);
        // Prepare player
        ensurePlayer();
        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(song.url);
        } catch (IOException e) {
            Log.e(TAG, "IOException with url: " + song.url);
        }
        mMediaPlayer.prepareAsync();
        // Update notification
        Notification notification = new Notification.Builder(getApplicationContext())
            .setSmallIcon(R.drawable.ic_stat_fasola)
            .setContentTitle(song.name)
            .setContentText(song.leaders)
            .setOngoing(true)
            .getNotification(); // build() was added in API 16
        if (! mHasNotification) {
            Log.v(TAG, "Starting foreground service");
            startForeground(NOTIFICATION_ID, notification);
            mHasNotification = true;
        } else
            mNotificationManager.notify(NOTIFICATION_ID, notification);
        return true;
    }

    // Start/Enqueue overloads
    public void startLead(long leadId) {
        enqueueLead(C.SongLeader.leadId, String.valueOf(leadId), true);
    }

    public void startLead(String url) {
        enqueueLead(C.SongLeader.audioUrl, url, true);
    }

    public void enqueueLead(long leadId) {
        enqueueLead(C.SongLeader.leadId, String.valueOf(leadId), false);
    }

    public void enqueueLead(String url) {
        enqueueLead(C.SongLeader.audioUrl, url, false);
    }

    public void enqueueLead(Object column, String arg, final boolean start) {
        // Construct a Song and add to the playlist
        MinutesLoader loader = new MinutesLoader(Song.getQuery(column), arg);
        loader.startLoading(new MinutesLoader.FinishedCallback() {
            @Override
            public void onLoadFinished(Cursor cursor) {
                if (! cursor.moveToFirst())
                    return;
                mPlaylist.add(new Song(cursor));
                if (start)
                    playNext();
            }
        });
    }


    // Callbacks
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.v(TAG, "Prepared; starting playback");
        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.v(TAG, "Complete");
        // Start the next
        if (! playNext()) {
            Log.v(TAG, "End of playlist: stopping foreground service");
            stopForeground(true);
            mHasNotification = false;
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Error: " + String.valueOf(what));
        return false;
    }
}