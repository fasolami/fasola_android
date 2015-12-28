package org.fasola.fasolaminutes;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NowPlayingView extends LinearLayout {
    ImageButton mPlayPause;
    TextView mText;

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
        mPlayPause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    DataSetObserver mPlaylistObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
        Playlist playlist = Playlist.getInstance();
        if (! playlist.isEmpty()) {
            setVisibility(View.VISIBLE);
            Playlist.Song song = playlist.getCurrent();
            mText.setText(song != null ? song.name : "");
        }
        else {
            setVisibility(View.GONE);
        }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Playlist.getInstance().registerObserver(mPlaylistObserver);
        Playlist.getInstance().registerPlayingObserver(mPlaylistObserver);
        mPlaylistObserver.onChanged(); // Update
    }

    @Override
    protected void onDetachedFromWindow() {
        Playlist.getInstance().unregisterObserver(mPlaylistObserver);
        Playlist.getInstance().unregisterPlayingObserver(mPlaylistObserver);
        super.onDetachedFromWindow();
    }
}
