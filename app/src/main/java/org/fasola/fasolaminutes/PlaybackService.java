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
import java.util.Queue;
import java.util.LinkedList;

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
    Queue<Long> mPlaylist;

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mPlaylist = new LinkedList<>();
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

    // Play a song given a lead id
    public void startLead(long leadId) {
        startLead(C.SongLeader.leadId, String.valueOf(leadId));
    }

    public void startLead(String url) {
        startLead(C.SongLeader.audioUrl, url);
    }

    public void startLead(Object column, String... args) {
        SQL.Query query = C.SongLeader.select(C.SongLeader.audioUrl,
                                              C.Song.fullName,
                                              C.Leader.fullName.func("group_concat", "', '"))
                                      .whereEq(column)
                                      .group(column);
        MinutesLoader loader = new MinutesLoader(query, args);
        loader.startLoading(new MinutesLoader.FinishedCallback() {
            @Override
            public void onLoadFinished(Cursor cursor) {
                if (!cursor.moveToFirst())
                    return;
                // Prepare player
                ensurePlayer();
                mMediaPlayer.reset();
                try {
                    mMediaPlayer.setDataSource(cursor.getString(0));
                } catch (IOException e) {
                    Log.e(TAG, "IOException with url: " + cursor.getString(0));
                }
                mMediaPlayer.prepareAsync();
                // Update notification
                Notification notification = new Notification.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.ic_stat_fasola)
                    .setContentTitle(cursor.getString(1))
                    .setContentText(cursor.getString(2))
                    .setOngoing(true)
                    .build();
                if (! mHasNotification) {
                    Log.v(TAG, "Starting foreground service");
                    startForeground(NOTIFICATION_ID, notification);
                    mHasNotification = true;
                } else
                    mNotificationManager.notify(NOTIFICATION_ID, notification);
            }
        });
    }

    public void enqueueLead(long leadId) {
        mPlaylist.add(leadId);
    }

    public void enqueueLead(String url) {
        // Find the id then add to playlist
        SQL.Query query = C.SongLeader.select(C.SongLeader.leadId).whereEq(C.SongLeader.audioUrl);
        MinutesLoader loader = new MinutesLoader(query, url);
        loader.startLoading(new MinutesLoader.FinishedCallback() {
            @Override
            public void onLoadFinished(Cursor cursor) {
                if (!cursor.moveToFirst())
                    return;
                enqueueLead(cursor.getLong(0));
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
        if (! mPlaylist.isEmpty())
            startLead(mPlaylist.remove());
        else {
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