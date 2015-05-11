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
import android.util.Log;

import java.io.IOException;

public class PlaybackService extends Service
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
                   MediaPlayer.OnErrorListener {

    private static final String TAG = "PlaybackService";

    public static final String ACTION_PLAY = "org.fasola.fasolaminutes.media.PLAY";
    public static final String ACTION_ENQUEUE = "org.fasola.fasolaminutes.media.ENQUEUE";
    public static final String EXTRA_LEAD_ID = "org.fasola.fasolaminutes.media.EXTRA_LEAD_ID";
    public static final String EXTRA_URL = "org.fasola.fasolaminutes.media.EXTRA_URL";
    public static final String EXTRA_URL_LIST = "org.fasola.fasolaminutes.media.EXTRA_URL_LIST";
    private static final int NOTIFICATION_ID = 1;

    MediaPlayer mMediaPlayer;
    NotificationManager mNotificationManager;
    boolean mHasNotification;

    Playlist mPlaylist;

    static PlaybackService mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mPlaylist = Playlist.getInstance();
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
            else if (intent.hasExtra(EXTRA_URL_LIST))
                startLead(intent.getStringArrayExtra(EXTRA_URL_LIST));
        }
        else if (intent.getAction().equals(ACTION_ENQUEUE)) {
            if (intent.hasExtra(EXTRA_LEAD_ID))
                enqueueLead(intent.getLongExtra(EXTRA_LEAD_ID, -1));
            else if (intent.hasExtra(EXTRA_URL))
                enqueueLead(intent.getStringExtra(EXTRA_URL));
            else if (intent.hasExtra(EXTRA_URL_LIST))
                enqueueLead(intent.getStringArrayExtra(EXTRA_URL_LIST));
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
        ensurePlayer();
        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(song.url);
        } catch (IOException e) {
            Log.e(TAG, "IOException with url: " + song.url);
        }
        mMediaPlayer.prepareAsync();
        // Create Notification Intent
        Intent intent = new Intent(this, PlaylistActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        // Update notification
        Notification notification = new Notification.Builder(getApplicationContext())
            .setSmallIcon(R.drawable.ic_stat_fasola)
            .setContentTitle(song.name)
            .setContentText(song.leaders)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
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
        enqueueLead(true, C.SongLeader.leadId, leadId);
    }

    public void startLead(String... urls) {
        enqueueLead(true, C.SongLeader.audioUrl, (Object[])urls);
    }

    public void enqueueLead(long leadId) {
        enqueueLead(false, C.SongLeader.leadId, leadId);
    }

    public void enqueueLead(String... urls) {
        enqueueLead(false, C.SongLeader.audioUrl, (Object[])urls);
    }

    public void enqueueLead(final boolean start, Object column, Object... args) {
        // Construct a Song and add to the playlist
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