/*
 * This file is part of FaSoLa Minutes for Android.
 * Copyright (c) 2016 Mike Richards. All rights reserved.
 */

package org.fasola.fasolaminutes;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NowPlayingView extends LinearLayout {
    ImageButton mPlayPause;
    TextView mTitle;
    TextView mSubtitle;
    Drawable mProgressDrawable;
    PlaybackService.Control mPlayer;

    public NowPlayingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Inflate the layout
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_now_playing, this, true);
        setBackgroundResource(R.drawable.now_playing_progress);
        mProgressDrawable = ((LayerDrawable) getBackground()).findDrawableByLayerId(android.R.id.progress);
        setClickable(true);
        // Set members
        mTitle = (TextView) findViewById(R.id.nowplaying_title);
        mSubtitle = (TextView) findViewById(R.id.nowplaying_subtitle);
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

    PlaylistObserver mObserver = new PlaylistObserver() {
        @Override
        public void onChanged() {
            PlaybackService service = PlaybackService.getInstance();
            Playlist.Song song = service != null ? service.getSong() : null;
            if (song == null) {
                setVisibility(View.GONE);
                return;
            }
            setVisibility(View.VISIBLE);
            mTitle.setText(String.format("%s  -  %s", song.name, song.leaders));
            mSubtitle.setText(String.format("%s %s", song.year, song.singing));
            mPlayPause.setImageResource(mPlayer.isPlaying() ?
                    R.drawable.ic_pause :
                    R.drawable.ic_play_arrow);
            updateProgress();
            // Error icon
            int iconResource = 0;
            if (song.status == Playlist.Song.STATUS_ERROR)
                iconResource = R.drawable.ic_warning_amber_18dp;
            mTitle.setCompoundDrawablesWithIntrinsicBounds(iconResource, 0, 0, 0);
            // Loading animation
            if (service.isLoading()) {
                findViewById(R.id.loading).setVisibility(View.VISIBLE);
                findViewById(R.id.play_pause).setVisibility(View.GONE);
            }
            else {
                findViewById(R.id.loading).setVisibility(View.GONE);
                findViewById(R.id.play_pause).setVisibility(View.VISIBLE);
            }
        }
    };

    /** Update the progress bar and start the progress runnable. */
    public void updateProgress() {
        if (mProgressDrawable == null)
            return;
        if (mPlayer.getDuration() > 0)
            mProgressDrawable.setLevel(mPlayer.getCurrentPosition() * 10000 / mPlayer.getDuration());
        else
            mProgressDrawable.setLevel(0);
        if (mPlayer.isPlaying())
            startProgress();
        else
            stopProgress();
    }

    boolean mRunnableIsRunning; // Flag to prevent multiple postDelayed of the same runnable

    // Start progress bar runnable if runnable was not already started
    private void startProgress() {
        if (! mRunnableIsRunning) {
            mRunnableIsRunning = true;
            postDelayed(mProgressRunnable, 1000);
        }
    }

    // Stop progress bar runnable
    private void stopProgress() {
        mRunnableIsRunning = false;
        removeCallbacks(mProgressRunnable);
    }

    Runnable mProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPlayer.isPlaying()) {
                mRunnableIsRunning = true;
                postDelayed(mProgressRunnable, 1000);
            }
            updateProgress();
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
        stopProgress();
        mObserver.unregister();
        super.onDetachedFromWindow();
    }
}
