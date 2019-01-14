package org.fasola.fasolaminutes;

import android.os.Build;
import android.os.Bundle;

public class SongFilterActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_song_filter);
        // This should be set in the style, but apparently we can't use this in xml
        // See: https://issuetracker.google.com/issues/37036728
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(R.id.key_layout).setClipToOutline(true);
            findViewById(R.id.major_minor_layout).setClipToOutline(true);
            findViewById(R.id.time_layout).setClipToOutline(true);
            findViewById(R.id.page_orientation_layout).setClipToOutline(true);
            findViewById(R.id.page_left_right_layout).setClipToOutline(true);
        }
    }
}