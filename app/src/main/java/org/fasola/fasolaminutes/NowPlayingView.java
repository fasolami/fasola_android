package org.fasola.fasolaminutes;

import android.content.Context;
import android.content.Intent;
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
        inflater.inflate(R.layout.view_now_playing, this, true);
        setBackgroundColor(getResources().getColor(R.color.fasola_foreground));
        setClickable(true);
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
                getContext().startActivity(new Intent(getContext(), NowPlayingActivity.class));
            }
        });
    }

    protected void updateButton() {
        mPlayPause.setImageResource(mPlayer.isPlaying() ?
                R.drawable.ic_pause :
                R.drawable.ic_play_arrow);
    }

    PlaylistObserver mObserver = new PlaylistObserver() {
        @Override
        public void onChanged() {
            Playlist.Song song = Playlist.getInstance().getCurrent();
            if (! PlaybackService.isRunning() || song == null) {
                setVisibility(View.GONE);
                return;
            }
            setVisibility(View.VISIBLE);
            mText.setText(String.format("%s  -  %s %s", song.name, song.year, song.singing));
            updateButton();
            // Handle broadcasts
            if (getAction() != null) {
                // Error indicator
                if (getAction().equals(PlaybackService.BROADCAST_PREPARED))
                    mText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                else if (getAction().equals(PlaybackService.BROADCAST_ERROR))
                    mText.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_warning_amber_18dp, 0, 0, 0);
                // Loading animation
                else if (getAction().equals(PlaybackService.BROADCAST_LOADING)) {
                    findViewById(R.id.loading).setVisibility(View.VISIBLE);
                    findViewById(R.id.play_pause).setVisibility(View.GONE);
                }
                else if (getAction().equals(PlaybackService.BROADCAST_PLAYING)) {
                    findViewById(R.id.loading).setVisibility(View.GONE);
                    findViewById(R.id.play_pause).setVisibility(View.VISIBLE);
                }
            }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mObserver.registerAll(getContext(), PlaybackService.BROADCAST_ALL);
        mObserver.onChanged();
    }

    @Override
    protected void onDetachedFromWindow() {
        mObserver.unregister();
        super.onDetachedFromWindow();
    }
}
