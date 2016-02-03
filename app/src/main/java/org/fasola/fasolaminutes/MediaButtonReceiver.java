package org.fasola.fasolaminutes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

/** BroadcastReceiver that handles MEDIA_BUTTON events (e.g. lock screen). */
public class MediaButtonReceiver extends BroadcastReceiver {
    PlaybackService.Control mControl = new PlaybackService.Control(MinutesApplication.getContext());

    @Override
    public void onReceive(Context context, Intent intent) {
        Context c = context.getApplicationContext();
        if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        PlaybackService service = PlaybackService.getInstance();
                        if (service.isPaused())
                            mControl.start();
                        else
                            mControl.pause();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        mControl.previous();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        mControl.next();
                        break;
                }
            }
        }
    }
}