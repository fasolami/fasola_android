/*
 * This file is part of FaSoLa Minutes for Android.
 * Copyright (c) 2016 Mike Richards. All rights reserved.
 */

package org.fasola.fasolaminutes;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.MediaController;

import java.lang.reflect.Field;

/**
 * A MediaController that works in an xml layout
 *
 * MediaController seems to have a bug where the progress bar is not updated
 * if the progress bar is not continuously updated when used in an xml layout.
 *
 * The SHOW_PROGRESS handler checks mShowing to determine if it should fire
 * another SHOW_PROGRESS event.  mShowing == false in the xml layout, so
 * here we force it to true using reflection.
 *
 * hide() may be called in unwanted places, and as it usually checks that
 * mShowing == true before it does anything, it is overridden as a no-op.
 */
public class MediaControllerXml extends MediaController {

    public MediaControllerXml(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        setShowing(true);
        // Confusingly, setAnchorView() inflates the internal media_controller layout.  A null
        // anchor must be passed so that the OnLayoutChangeListener (which modifies a non-existent
        // floating window) is not connected.
        setAnchorView(null);
    }

    @Override
    public void hide() {
        // Do nothing; this MediaController is always shown
    }

    private void setShowing(boolean showing) {
        try {
            Field f = MediaController.class.getDeclaredField("mShowing");
            f.setAccessible(true);
            f.setBoolean(this, showing);
        } catch (NoSuchFieldException e) {
            Log.e("MediaControllerXml", "NoSuchField");
        } catch (IllegalAccessException e) {
            Log.e("MediaControllerXml", "IllegalAccess");
        }
    }
}
