package org.fasola.fasolaminutes;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A singleton foreground service for music playback
 */
public class PlaybackService extends Service
        implements MediaPlayer.OnPreparedListener,
            MediaPlayer.OnCompletionListener,
            MediaPlayer.OnErrorListener,
            MediaPlayer.OnInfoListener,
            MediaController.MediaPlayerControl,
            AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "PlaybackService";
    private static final int ERROR_LIMIT = 100;
    private static final int ERROR_DELAY_MS = 500;

    /** Enqueue songs */
    public static final String ACTION_ENQUEUE_MEDIA = "org.fasola.fasolaminutes.media.ENQUEUE";
    /** Clear playlist, enqueue songs, and start playing from the top */
    public static final String ACTION_PLAY_MEDIA = "org.fasola.fasolaminutes.media.PLAY";
    /** Extra is {@code long leadId} */
    public static final String EXTRA_LEAD_ID = "org.fasola.fasolaminutes.media.EXTRA_LEAD_ID";
    /** Extra is {@code String url} */
    public static final String EXTRA_URL = "org.fasola.fasolaminutes.media.EXTRA_URL";
    /** Extra is {@code String[] urls} */
    public static final String EXTRA_URL_LIST = "org.fasola.fasolaminutes.media.EXTRA_URL_LIST";
    /** Index of EXTRA_URL_LIST that should start playing */
    public static final String EXTRA_PLAY_INDEX = "org.fasola.fasolaminutes.media.EXTRA_INDEX";

    /** Play */
    public static final String ACTION_PLAY = "org.fasola.fasolaminutes.action.PLAY";
    /** Notification Play/Pause */
    public static final String ACTION_PLAY_PAUSE = "org.fasola.fasolaminutes.action.PLAY_PAUSE";
    /** Notification Next */
    public static final String ACTION_NEXT = "org.fasola.fasolaminutes.action.NEXT";
    /** Notification Close */
    public static final String ACTION_CLOSE = "org.fasola.fasolaminutes.action.STOP";

    /** Broadcast sent when the song is changed */
    public static final String BROADCAST_NEW_SONG = "org.fasola.fasolaminutes.mediaBroadcast.SONG";
    /** Broadcast sent when the {@link MediaPlayer} is loading */
    public static final String BROADCAST_LOADING = "org.fasola.fasolaminutes.mediaBroadcast.LOADING";
    /** Broadcast sent when the {@link MediaPlayer} is prepared */
    public static final String BROADCAST_PREPARED = "org.fasola.fasolaminutes.mediaBroadcast.PREPARED";
    /** Broadcast sent when the {@link MediaPlayer} has started playing */
    public static final String BROADCAST_PLAYING = "org.fasola.fasolaminutes.mediaBroadcast.PLAYING";
    /** Broadcast sent when the {@link MediaPlayer} is paused */
    public static final String BROADCAST_PAUSED = "org.fasola.fasolaminutes.mediaBroadcast.PAUSED";
    /** Broadcast sent when playback (of a single song) is completed */
    public static final String BROADCAST_COMPLETED = "org.fasola.fasolaminutes.mediaBroadcast.COMPLETED";
    /** Broadcast sent on {@link MediaPlayer} error */
    public static final String BROADCAST_ERROR = "org.fasola.fasolaminutes.mediaBroadcast.ERROR";

    /** All broadcast actions. */
    public static final String[] BROADCAST_ALL = {
            BROADCAST_NEW_SONG, BROADCAST_LOADING, BROADCAST_PREPARED, BROADCAST_PLAYING,
            BROADCAST_PAUSED, BROADCAST_COMPLETED, BROADCAST_ERROR
    };

    MediaPlayer mMediaPlayer;
    Control mControl;
    boolean mIsPrepared;
    boolean mIsLoading;
    boolean mShouldPlay; // Should we play the song once it is prepared?
    Playlist.Song mSong; // Prepared song
    int mErrorCount; // Number of sequential errors
    NotificationManager mNotificationManager;
    Notification mNotification;
    private static final int NOTIFICATION_ID = 1;
    AudioManager mAudioManager;

    MediaSessionCompat mMediaSession;

    // Singleton
    static PlaybackService mInstance;

    /**
     * Returns the {@link PlaybackService} singleton or {@code null} if it is not running
     *
     * <p>Call {@link Context#startService(Intent)} to create a PlaybackService
     */
    public static PlaybackService getInstance() {
        return mInstance;
    }

    //region Lifecycle functions
    //---------------------------------------------------------------------------------------------
    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mControl = new Control(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mObserver.registerBroadcastReceiver(getApplicationContext());
        mObserver.registerPlaylistObserver();
        ComponentName receiver = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());
        mMediaSession = new MediaSessionCompat(this, "PlaybackService", receiver, null);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction(): null;
        if (action == null)
            return START_NOT_STICKY;
        // Enqueue/play
        else if (action.equals(ACTION_PLAY_MEDIA) || action.equals(ACTION_ENQUEUE_MEDIA)) {
            int play = -1;
            if (action.equals(ACTION_PLAY_MEDIA))
                play = intent.getIntExtra(EXTRA_PLAY_INDEX, 0);
            if (intent.hasExtra(EXTRA_LEAD_ID))
                enqueueLead(play, C.SongLeader.leadId, intent.getLongExtra(EXTRA_LEAD_ID, -1));
            else if (intent.hasExtra(EXTRA_URL))
                enqueueLead(play, C.SongLeader.audioUrl, intent.getStringExtra(EXTRA_URL));
            else if (intent.hasExtra(EXTRA_URL_LIST))
                enqueueLead(play, C.SongLeader.audioUrl, (Object[])intent.getStringArrayExtra(EXTRA_URL_LIST));
        }
        // Controls
        else if (action.equals(ACTION_PLAY)) {
            start();
        }
        // Notification controls
        else if (action.equals(ACTION_PLAY_PAUSE)) {
            if (isPaused())
                mControl.start();
            else
                mControl.pause();
        }
        else if (action.equals(ACTION_NEXT)) {
            mControl.next();
        }
        else if (action.equals(ACTION_CLOSE)) {
            stop();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            pause();
            mMediaSession.setActive(false);
        }
        else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            start();
            mMediaSession.setActive(true);
        }
        else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            stop();
        }
    }

    @Override
    public void onDestroy() {
        stop();
        mObserver.unregister();
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
    //---------------------------------------------------------------------------------------------
    //endregion


    /** Returns {@code true} if this service is running */
    public static boolean isRunning() {
        return mInstance != null;
    }

    /** Returns {@code true} if the {@link MediaPlayer} is prepared and ready to play */
    public boolean isPrepared() {
        return mIsPrepared;
    }

    /** Returns {@code true} if the {@link MediaPlayer} is loading (not prepared or buffering) */
    public boolean isLoading() {
        return mIsLoading;
    }
    // region MediaPlayerControl overrides
    //---------------------------------------------------------------------------------------------
    @Override
    public void start() {
        mShouldPlay = true;
        if (isPrepared()) {
            int result = mAudioManager.requestAudioFocus(this,
                                                         AudioManager.STREAM_MUSIC,
                                                         AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                ensurePlayer().start();
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_PLAYING));
                mMediaSession.setActive(true);
            }
        }
        else {
            if (Playlist.getInstance().getCurrent() == null)
                Playlist.getInstance().moveToFirst();
            prepare();
        }
        updateNotification();
        updateMediaSession();
    }

    @Override
    public void pause() {
        mShouldPlay = false;
        if (isPrepared())
            ensurePlayer().pause();
        updateNotification();
        updateMediaSession();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_PAUSED));
    }

    /**
     * Stop playback, release resources, and stop the service
     * This is not a MediaPlayerControl override
     */
    public void stop() {
        mSong = null;
        mAudioManager.abandonAudioFocus(this);
        mMediaSession.setActive(false);
        mMediaSession.release();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mShouldPlay = false;
        mIsPrepared = false;
        mIsLoading = false;
        mNotification = null;
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_PAUSED));
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int getDuration() {
        return isPrepared() ? ensurePlayer().getDuration() : 0;
    }

    @Override
    public int getCurrentPosition() {
        return isPrepared() ? ensurePlayer().getCurrentPosition() : 0;
    }

    @Override
    public void seekTo(int i) {
        if (isPrepared()) {
            ensurePlayer().seekTo(i);
            updateNotification();
            updateMediaSession();
        }
    }

    @Override
    public boolean isPlaying() {
        return isPrepared() && mMediaPlayer.isPlaying();
    }

    /** Returns the currently playing song. */
    public Playlist.Song getSong() {
        return mSong;
    }

    public boolean isPaused() {
        return ! mShouldPlay;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return isPrepared();
    }

    @Override
    public boolean canSeekForward() {
        return isPrepared();
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
    //---------------------------------------------------------------------------------------------
    //endregion

    /** Updates the current song from the playlist position. */
    public void updateSong() {
        mSong = Playlist.getInstance().getCurrent();
        ensurePlayer();
        if (mMediaPlayer.isPlaying())
            mMediaPlayer.stop();
        mMediaPlayer.reset();
        mIsPrepared = false;
        mIsLoading = false;
        updateNotification();
        updateMediaSession();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_NEW_SONG));
    }

    /**
     * Prepares and plays the current song in the playlist (async)
     *
     * <p>Unless pause() is called afterwards, the song will start playing once it is prepared
     *
     * @return {@code true} if there is a song to play or {@code false} if the playlist is empty
     * @see #onPrepared(MediaPlayer)
     */
    public boolean prepare() {
        updateSong();
        if (mSong == null)
            return false;
        // Prepare player
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_LOADING));
        mIsLoading = true;
        try {
            mMediaPlayer.setDataSource(mSong.url);
        } catch (IOException | IllegalStateException e) {
            // TODO: something useful... a broadcast?
            Log.e(TAG, "Exception with url: " + mSong.url);
        }
        mMediaPlayer.prepareAsync();
        return true;
    }

    /**
     * Enqueues and optionally starts playback of one or more songs
     *
     * <p>This method queries (async) the database for songs and adds them to the playlist
     *
     * @param playIndex index in {@code args} to start playing, or -1 to ignore
     * @param column {@link SQL.Column} or {@code String} column name for a WHERE clause
     * @param args   values for the IN predicate for a WHERE clause
     * @see Playlist
     * @see Playlist#getSongQuery(Object, Object...)
     * @see Playlist#addAll(Cursor)
     */
    public void enqueueLead(final int playIndex, final SQL.Column column, final Object... args) {
        MinutesLoader loader = new MinutesLoader(Playlist.getSongQuery(column, args));
        loader.startLoading(new MinutesLoader.FinishedCallback() {
            @Override
            public void onLoadFinished(Cursor cursor) {
                // Add all songs in args to the playlist in order.
                // Doing this using SQL would require a temp table and an extra join.
                // Creating a map and sorting manually is a lot faster.

                // Map audioUrl or leadId to Playlist.Song
                if (! cursor.moveToFirst())
                    return;
                int colIndex = cursor.getColumnIndex(column.getKey());
                HashMap<Object, Playlist.Song> songMap = new HashMap<>();
                do {
                    songMap.put(cursor.getString(colIndex), new Playlist.Song(cursor));
                } while (cursor.moveToNext());

                // Put songs in order
                ArrayList<Playlist.Song> songs = new ArrayList<>(args.length);
                for (Object arg : args)
                    songs.add(songMap.get(arg));

                // Add to the playlist and start playback
                Playlist pl = Playlist.getInstance();
                if (playIndex > -1) {
                    // If we're just playing songs, remove all previous songs from the playlist
                    pl.replaceWith(songs);
                    pl.moveToPosition(playIndex);
                    prepare();
                }
                else {
                    pl.addAll(songs);
                }
            }
        });
    }

    /**
     * Constructs a MediaPlayer if necessary
     *
     * @return mMediaPlayer for chaining
     */
    private MediaPlayer ensurePlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnInfoListener(this);
        }
        return mMediaPlayer;
    }

    /** Creates a new notification or returns an existing notification
     *
     * @return {@link Notification} that controls playback
     */
    private Notification createNotification() {
        // RemoteViews
        RemoteViews remote = new RemoteViews(getPackageName(), R.layout.notification_playback);
        Intent playIntent = new Intent(ACTION_PLAY_PAUSE, null, this, PlaybackService.class);
        remote.setOnClickPendingIntent(R.id.play_pause, PendingIntent.getService(
                this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT
        ));
        Intent nextIntent = new Intent(ACTION_NEXT, null, this, PlaybackService.class);
        remote.setOnClickPendingIntent(R.id.next, PendingIntent.getService(
                this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT
        ));
        Intent closeIntent = new Intent(ACTION_CLOSE, null, this, PlaybackService.class);
        remote.setOnClickPendingIntent(R.id.close, PendingIntent.getService(
                this, 0, closeIntent, PendingIntent.FLAG_UPDATE_CURRENT
        ));
        // Main Notification Intent
        Intent intent = new Intent(this, NowPlayingActivity.class);
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

    boolean mHasMainTask = true;
    public void setMainTaskRunning(boolean isRunning) {
        // Stop if the app is closing and playback is paused
        if (! isRunning && isPaused())
            stop();
        // Update notification to use synthesized back stack if the app is exiting
        else if (isRunning != mHasMainTask) {
            mHasMainTask = isRunning;
            updateNotification();
        }
    }

    /** Updates the {@link Notification} with the current playing status
     *
     * <p>If no notification exists this service is in the background.  In this case,
     * {@link #startForeground(int, Notification)} is called, and a new notification is created.
     *
     * @see #createNotification()
     */
    private void updateNotification() {
        Notification notification = mNotification != null ? mNotification : createNotification();
        // Update content
        RemoteViews remote = notification.contentView;
        remote.setTextViewText(R.id.title, mSong != null ? mSong.name : "");
        remote.setTextViewText(R.id.singing, mSong != null ? mSong.singing : "");
        remote.setImageViewResource(R.id.play_pause, isPlaying()
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);
        remote.setViewVisibility(R.id.play_pause, isLoading() ? View.GONE : View.VISIBLE);
        remote.setViewVisibility(R.id.loading, isLoading() ? View.VISIBLE : View.GONE);
        // Update pending intent
        if (mHasMainTask) {
            // Launch NowPlayingActivity normally
            Intent intent = new Intent(this, NowPlayingActivity.class);
            notification.contentIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        else {
            // Synthesize stack MainActivity -> NowPlayingActivity
            TaskStackBuilder stack = TaskStackBuilder.create(getApplicationContext());
            stack.addNextIntent(new Intent(this, MainActivity.class));
            stack.addNextIntent(new Intent(this, NowPlayingActivity.class));
            notification.contentIntent = stack.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        // Show or update notification
        if (mNotification == null) {
            Log.v(TAG, "Starting foreground service");
            startForeground(NOTIFICATION_ID, notification);
            mNotification = notification;
        }
        else
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    private void updateMediaSession() {
        Playlist playlist = Playlist.getInstance();
        // Get playback state
        PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder();
        if (isPlaying()) {
            state.setState(PlaybackStateCompat.STATE_PLAYING, getCurrentPosition(), 1);
        }
        else if (mSong != null && mSong.status == Playlist.Song.STATUS_ERROR) {
            state.setState(PlaybackStateCompat.STATE_ERROR, getCurrentPosition(), 1);
        }
        else if (isPaused()) {
            state.setState(PlaybackStateCompat.STATE_PAUSED, getCurrentPosition(), 1);
        }
        else if (isLoading()) {
            if (isPrepared())
                state.setState(PlaybackStateCompat.STATE_BUFFERING, getCurrentPosition(), 1);
            else
                state.setState(PlaybackStateCompat.STATE_CONNECTING, getCurrentPosition(), 1);
        }
        state.setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                (playlist.hasNext() ? PlaybackStateCompat.ACTION_SKIP_TO_NEXT : 0) |
                (playlist.hasPrevious() ? PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS : 0)
        );
        mMediaSession.setPlaybackState(state.build());
        // Set metadata
        if (mSong != null)
            mMediaSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mSong.singing)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, mSong.leaders)
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mSong.name)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration())
                    .putString(MediaMetadataCompat.METADATA_KEY_DATE, mSong.date)
                    .build());
        else
            mMediaSession.setMetadata(new MediaMetadataCompat.Builder().build());
    }

    /**
     * Observer that pauses playback if the song is removed from the playlist
     */
    PlaylistObserver mObserver = new PlaylistObserver() {
        {
            setFilter(Intent.ACTION_HEADSET_PLUG);
        }

        boolean mWasPlugged = false;

        // Pause playback when song is removed from playlist
        @Override
        public void onPlaylistChanged() {
            if (mSong != null && ! Playlist.getInstance().contains(mSong))
                pause();
        }

        // Pause playback when headset is unplugged
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                boolean isPlugged = intent.getIntExtra("state", 0) > 0;
                if (mWasPlugged && ! isPlugged)
                    pause();
                mWasPlugged = isPlugged;
            }
        }
    };

    //region MediaPlayer Callbacks
    //---------------------------------------------------------------------------------------------
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.v(TAG, "Prepared; starting playback");
        mIsPrepared = true;
        mIsLoading = false;
        mErrorCount = 0;
        if (mSong != null)
            mSong.status = Playlist.Song.STATUS_OK;
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_PREPARED));
        if (mShouldPlay)
            start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.v(TAG, "Complete");
        mIsPrepared = false;
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_COMPLETED));
        // Start the next
        if (Playlist.getInstance().moveToNext() != null)
            prepare();
        else {
            Log.v(TAG, "End of playlist: stopping service");
            stop();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Error: " + String.valueOf(what));
        if (mSong != null)
            mSong.status = Playlist.Song.STATUS_ERROR;
        mIsPrepared = false;
        ++mErrorCount;
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_ERROR));
        if (mErrorCount >= ERROR_LIMIT) {
            mErrorCount = 0;
            return false; // Give up on this song; move to the next
        }
        // Try to load the same song a few times
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mMediaPlayer != null)
                    prepare();
            }
        }, ERROR_DELAY_MS);
        return true;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        // Send broadcasts based on buffering state
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            mIsLoading = true;
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_LOADING));
            updateNotification();
            updateMediaSession();
            return true;
        }
        else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            mIsLoading = false;
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_PREPARED));
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_PLAYING));
            updateNotification();
            updateMediaSession();
            return true;
        }
        return false;
    }

    //---------------------------------------------------------------------------------------------
    //endregion

    /**
     * A MediaPlayerControl implementation that wraps the PlaybackService singleton
     *
     * <p>If PlaybackService is not running, methods are either a no-op or return default values
     */
    public static class Control implements MediaController.MediaPlayerControl {
        /** Above this threshold PREV actually restarts the current song */
        public static final int RESTART_THRESHOLD = 15000;

        Context mContext;

        /**
         * Constructs a new MediaPlayerControl
         *
         * @param context A {@link Context} that will be used to start {@link PlaybackService}
         *                when {@link #start} is called.
         */
        public Control(Context context) {
            mContext = context.getApplicationContext();
        }

        /** NextListener for MediaController
         *
         * @see MediaController#setPrevNextListeners(View.OnClickListener, View.OnClickListener)
         */
        public View.OnClickListener nextListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        };

        /** NextListener for MediaController
         *
         * @see MediaController#setPrevNextListeners(View.OnClickListener, View.OnClickListener)
         */
        public View.OnClickListener prevListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previous();
            }
        };

        @Override
        public boolean canPause() {
            return isRunning() && getInstance().canPause();
        }

        @Override
        public void start() {
            if (isRunning())
                getInstance().start();
            else {
                // Start the service and play
                Intent intent = new Intent(mContext, PlaybackService.class);
                intent.setAction(PlaybackService.ACTION_PLAY);
                mContext.startService(intent);
            }
        }

        /**
         * Play a particular song in the playlist
         *
         * @param pos playlist position
         */
        public void start(int pos) {
            Playlist.getInstance().moveToPosition(pos);
            if (isRunning())
                getInstance().updateSong();
            start();
        }

        public void next() {
            Playlist.getInstance().moveToNext();
            if (isRunning()) {
                getInstance().updateSong();
                if (! getInstance().isPaused())
                    start();
            }
        }

        public void previous() {
            if (getCurrentPosition() > RESTART_THRESHOLD)
                seekTo(0);
            else {
                Playlist.getInstance().moveToPrev();
                if (isRunning()) {
                    getInstance().updateSong();
                    if (! getInstance().isPaused())
                        start();
                }
            }
        }

        @Override
        public void pause() {
            if (isRunning()) getInstance().pause();
        }

        @Override
        public int getDuration() {
            return isRunning() ? getInstance().getDuration() : 0;
        }

        @Override
        public int getCurrentPosition() {
            return isRunning() ? getInstance().getCurrentPosition() : 0;
        }

        @Override
        public void seekTo(int pos) {
            if (isRunning()) getInstance().seekTo(pos);
        }

        @Override
        public boolean isPlaying() {
            return isRunning() && getInstance().isPlaying();
        }

        @Override
        public int getBufferPercentage() {
            return isRunning() ? getInstance().getBufferPercentage() : 0;
        }

        @Override
        public boolean canSeekBackward() {
            return isRunning() && getInstance().canSeekBackward();
        }

        @Override
        public boolean canSeekForward() {
            return isRunning() && getInstance().canSeekForward();
        }

        @Override
        public int getAudioSessionId() {
            return isRunning() ? getInstance().getAudioSessionId() : 0;
        }
    }

    //region playSong/Singing overrides
    //---------------------------------------------------------------------------------------------
    /**
     * Plays or enqueues a list of urls from the beginning.
     *
     * @param context a context
     * @param action PlaybackService.ACTION enums (Usually PLAY_MEDIA or ENQUEUE_MEDIA)
     * @param urls array of urls
     */
    public static void playSongs(Context context, String action, String... urls) {
        playSongs(context, action, 0, urls);
    }

    /**
     * Plays or enqueues a Cursor from the beginning.
     *
     * @param context a context
     * @param action PlaybackService.ACTION enums (Usually PLAY_MEDIA or ENQUEUE_MEDIA)
     * @param cursor cursor with {@link CursorListFragment#AUDIO_COLUMN} column
     */
    public static void playSongs(Context context, String action, Cursor cursor) {
        playSongs(context, action, 0, cursor);
    }

    /**
     * Plays or enqueues a singing from the beginning.
     *
     * @param context a context
     * @param action PlaybackService.ACTION enums (Usually PLAY_MEDIA or ENQUEUE_MEDIA)
     * @param singingId singing id
     */
    public static void playSinging(final Context context, final String action, long singingId) {
        // Query for songs
        SQL.Query query = C.SongLeader.select(C.SongLeader.leadId)
                            .select(C.SongLeader.audioUrl).as(CursorListFragment.AUDIO_COLUMN)
                            .where(C.SongLeader.singingId, "=", singingId)
                                .and(C.SongLeader.audioUrl, "IS NOT", "NULL")
                            .group(C.SongLeader.leadId)
                            .order(C.SongLeader.singingOrder, "ASC");
        // Start query and play when finished
        MinutesLoader loader = new MinutesLoader(query) {
            @Override
            public void onLoadFinished(Cursor cursor) {
                playSongs(context, action, cursor);
            }
        };
        loader.startLoading();
    }

    /**
     * Plays or enqueues all songs in a Cursor (with recording urls).
     *
     * @param context a context
     * @param action PlaybackService.ACTION enums (Usually PLAY_MEDIA or ENQUEUE_MEDIA)
     * @param position position in {@code cursor} to start playback
     * @param cursor cursor with {@link CursorListFragment#AUDIO_COLUMN} column
     */
    public static void playSongs(Context context, String action, int position, Cursor cursor) {
        int urlColumn = cursor.getColumnIndex(CursorListFragment.AUDIO_COLUMN);
        if (! cursor.moveToFirst())
            return;
        // Make a list of urls to add
        List<String> urls = new ArrayList<>();
        // Index of song to play in urls list
        // NB: If any songs are missing urls, they will not be added to the urls list, and thus
        // playIndex is not always the same as (list) position
        int playIndex = 0;
        do {
            if (! cursor.isNull(urlColumn)) {
                urls.add(cursor.getString(urlColumn)); // Enqueue next songs
                if (cursor.getPosition() == position)
                    playIndex = urls.size() - 1;
            }
        }
        while (cursor.moveToNext());
        // Send the intent
        if (urls.isEmpty())
            Log.e("CursorListFragment", "No recordings; ImageView should have been hidden");
        else
            playSongs(context, action, playIndex, urls.toArray(new String[urls.size()]));
    }

    /**
     * Plays or enqueues a list of songs and notifies the user with a toast.
     *
     * @param context a context
     * @param action PlaybackService.ACTION enums (Usually PLAY_MEDIA or ENQUEUE_MEDIA)
     * @param playIndex index in the url list of the first song to play
     * @param urls array of urls
     */
    public static void playSongs(Context context, String action, int playIndex, String... urls) {
        // Send the intent
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(action);
        if (urls.length > 1)
            intent.putExtra(PlaybackService.EXTRA_URL_LIST, urls);
        else
            intent.putExtra(PlaybackService.EXTRA_URL, urls[0]);
        intent.putExtra(PlaybackService.EXTRA_PLAY_INDEX, playIndex);
        context.startService(intent);
        // Show toast
        String message = context.getResources().getQuantityString(
            action.equals(PlaybackService.ACTION_PLAY_MEDIA) ?
                R.plurals.playing_songs:
                R.plurals.enqueuing_songs,
            urls.length,
            urls.length
        );
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    //---------------------------------------------------------------------------------------------
    //endregion playSong/Singing overrides
}