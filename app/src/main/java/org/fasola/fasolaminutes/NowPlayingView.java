package org.fasola.fasolaminutes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NowPlayingView extends LinearLayout {
    ImageButton mPlayPause;
    TextView mText;
    PlaybackService.Control mPlayer;

    public NowPlayingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Inflate the layout
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.now_playing, this, true);
        // Set members
        mText = (TextView) findViewById(R.id.song_title);
        mPlayPause = (ImageButton) findViewById(R.id.play_pause);
        // Events
        mPlayer = new PlaybackService.Control(getContext());
        mPlayPause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayer.isPlaying())
                    mPlayer.pause();
                else
                    mPlayer.start();
            }
        });
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().startActivity(new Intent(getContext(), PlaylistActivity.class));
            }
        });
    }

    protected void updateButton() {
        mPlayPause.setImageResource(mPlayer.isPlaying() ?
                R.drawable.ic_pause :
                R.drawable.ic_play_arrow);
    }

    DataSetObserver mPlaylistObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            Playlist.Song song = Playlist.getInstance().getCurrent();
            if (PlaybackService.isRunning() && song != null) {
                setVisibility(View.VISIBLE);
                mText.setText(String.format("%s  -  %s %s", song.name, song.year, song.singing));
            }
            else {
                setVisibility(View.GONE);
            }
        }
    };

    IntentFilter filter = new IntentFilter();
    {
        filter.addAction(PlaybackService.BROADCAST_PREPARED);
        filter.addAction(PlaybackService.BROADCAST_PLAYING);
        filter.addAction(PlaybackService.BROADCAST_COMPLETED);
        filter.addAction(PlaybackService.BROADCAST_ERROR);
        filter.addAction(PlaybackService.BROADCAST_PAUSED);
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateButton();
            // Add error indicator
            int iconResource = 0;
            if (intent.getAction().equals(PlaybackService.BROADCAST_ERROR))
                iconResource = R.drawable.ic_warning_amber_18dp;
            mText.setCompoundDrawablesWithIntrinsicBounds(iconResource, 0, 0, 0);
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Playlist.getInstance().registerObserver(mPlaylistObserver);
        Playlist.getInstance().registerPlayingObserver(mPlaylistObserver);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);
        mPlaylistObserver.onChanged();
        updateButton();
    }

    @Override
    protected void onDetachedFromWindow() {
        Playlist.getInstance().unregisterObserver(mPlaylistObserver);
        Playlist.getInstance().unregisterPlayingObserver(mPlaylistObserver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
        super.onDetachedFromWindow();
    }
}
