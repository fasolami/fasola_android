package org.fasola.fasolaminutes;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;

public class PlaybackService extends Service
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
                   MediaPlayer.OnErrorListener {

    private static final String TAG = "PlaybackService";

    public static final String ACTION_PLAY_MEDIA = "org.fasola.fasolaminutes.media.PLAY";
    public static final String ACTION_ENQUEUE_MEDIA = "org.fasola.fasolaminutes.media.ENQUEUE";
    public static final String EXTRA_LEAD_ID = "org.fasola.fasolaminutes.media.EXTRA_LEAD_ID";
    public static final String EXTRA_URL = "org.fasola.fasolaminutes.media.EXTRA_URL";
    public static final String EXTRA_URL_LIST = "org.fasola.fasolaminutes.media.EXTRA_URL_LIST";
    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_PLAY_PAUSE = "org.fasola.fasolaminutes.action.PLAY_PAUSE";
    public static final String ACTION_NEXT = "org.fasola.fasolaminutes.action.NEXT";

    public static final String BROADCAST_PREPARED = "org.fasola.fasolaminutes.mediaBroadcast.PREPARED";
    public static final String BROADCAST_COMPLETED = "org.fasola.fasolaminutes.mediaBroadcast.COMPLETED";
    public static final String BROADCAST_ERROR = "org.fasola.fasolaminutes.mediaBroadcast.ERROR";

    MediaPlayer mMediaPlayer;
    boolean mIsPrepared;
    NotificationManager mNotificationManager;
    Notification mNotification;

    Playlist mPlaylist;

    static PlaybackService mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mPlaylist = Playlist.getInstance();
    }

    public static PlaybackService getInstance() {
        return mInstance;
    }

    public static boolean isRunning() {
        return mInstance != null;
    }

    public boolean isPrepared() {
        return mIsPrepared;
    }

    public MediaPlayer getMediaPlayer () {
        return mMediaPlayer;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (intent.getAction() == null)
            return START_STICKY;
        // Enqueue/play
        else if (action.equals(ACTION_PLAY_MEDIA) || action.equals(ACTION_ENQUEUE_MEDIA)) {
            boolean play = false;
            if (action.equals(ACTION_PLAY_MEDIA)) {
                mPlaylist.clear();
                play = true;
            }
            if (intent.hasExtra(EXTRA_LEAD_ID))
                enqueueLead(play, C.SongLeader.leadId, intent.getLongExtra(EXTRA_LEAD_ID, -1));
            else if (intent.hasExtra(EXTRA_URL))
                enqueueLead(play, C.SongLeader.audioUrl, intent.getStringExtra(EXTRA_URL));
            else if (intent.hasExtra(EXTRA_URL_LIST))
                enqueueLead(play, C.SongLeader.audioUrl, intent.getStringArrayExtra(EXTRA_URL_LIST));
        }
        // Controls
        else if (intent.getAction().equals(ACTION_PLAY_PAUSE)) {
            if (mMediaPlayer.isPlaying())
                mMediaPlayer.pause();
            else
                mMediaPlayer.start();
            updateNotification();
        }
        else if (intent.getAction().equals(ACTION_NEXT)) {
            playNext();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
       if (mMediaPlayer != null)
           mMediaPlayer.release();
       mIsPrepared = false;
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

    public Playlist getPlaylist() {
        return mPlaylist;
    }

    /**
     * Play the next song in the playlist
     * @return true if there is a song to play; false if the playlist is empty
     */
    public boolean playNext() {
        // Get the song
        Playlist.Song song = mPlaylist.moveToNext();
        if (song == null)
            return false;
        // Prepare player
        mIsPrepared = false;
        ensurePlayer();
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(song.url);
        } catch (IOException e) {
            Log.e(TAG, "IOException with url: " + song.url);
        }
        mMediaPlayer.prepareAsync();
        updateNotification();
        return true;
    }

    /**
     * Enqueues and optionally starts playback of one or more songs
     *
     * <p>This method queries (async) the database for songs and adds them to the playlist
     *
     * @param start  {@code true} to start playback
     * @param column {@link SQL.Column} or {@code String} column name for a WHERE clause
     * @param args   values for the IN predicate for a WHERE clause
     * @see Playlist
     * @see Playlist#getSongQuery(Object, Object...)
     * @see Playlist#addAll(Cursor)
     */
    public void enqueueLead(final boolean start, Object column, Object... args) {
        MinutesLoader loader = new MinutesLoader(Playlist.getSongQuery(column, args));
        loader.startLoading(new MinutesLoader.FinishedCallback() {
            @Override
            public void onLoadFinished(Cursor cursor) {
                mPlaylist.addAll(cursor);
                if (start)
                    playNext();
            }
        });
    }

    // Notification
    public Notification getNotification() {
        if (mNotification != null)
            return mNotification;
        // RemoteViews
        RemoteViews remote = new RemoteViews(getPackageName(), R.layout.notification_playback);
        Intent playIntent = new Intent(this, PlaybackService.class);
        playIntent.setAction(ACTION_PLAY_PAUSE);
        remote.setOnClickPendingIntent(R.id.play_pause, PendingIntent.getService(
                this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT
        ));
        Intent nextIntent = new Intent(this, PlaybackService.class);
        nextIntent.setAction(ACTION_NEXT);
        remote.setOnClickPendingIntent(R.id.next, PendingIntent.getService(
                this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT
        ));
        // Main Notification Intent
        Intent intent = new Intent(this, PlaylistActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        // Build Notification
        return new Notification.Builder(getApplicationContext())
            .setSmallIcon(R.drawable.ic_stat_fasola)
            .setContent(remote)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .getNotification(); // build() was added in API 16
    }

    public void updateNotification() {
        Playlist.Song song = mPlaylist.getCurrent();
        Notification notification = getNotification();
        // Update content
        RemoteViews remote = notification.contentView;
        remote.setTextViewText(R.id.title, song.name);
        remote.setTextViewText(R.id.singing, song.singing);
        remote.setImageViewResource(R.id.play_pause, mMediaPlayer.isPlaying()
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);
        // Show or update notification
        if (mNotification == null) {
            Log.v(TAG, "Starting foreground service");
            startForeground(NOTIFICATION_ID, notification);
            mNotification = notification;
        } else
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    // Callbacks
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.v(TAG, "Prepared; starting playback");
        mIsPrepared = true;
        mp.start();
        updateNotification();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_PREPARED));
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.v(TAG, "Complete");
        mIsPrepared = false;
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_COMPLETED));
        // Start the next
        if (! playNext()) {
            Log.v(TAG, "End of playlist: stopping service");
            stopForeground(true);
            mNotification = null;
            stopSelf();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Error: " + String.valueOf(what));
        mIsPrepared = false;
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_ERROR));
        return false;
    }
}