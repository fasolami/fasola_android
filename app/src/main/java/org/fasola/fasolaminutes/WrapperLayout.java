package org.fasola.fasolaminutes;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * A Layout
 * Adapted from http://stackoverflow.com/a/8235542
 */
public class WrapperLayout extends FrameLayout {
    // The FrameLayout that holds the content
    private ViewGroup mContentContainer;

    public WrapperLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        // Read wrapper_layout custom attribute
        int wrapperLayoutId;
        TypedArray styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.WrapperLayout);
        wrapperLayoutId = styledAttributes.getResourceId(R.styleable.WrapperLayout_wrapper_layout, 0);
        styledAttributes.recycle();

        if (wrapperLayoutId != 0) {
            View inflatedLayout = View.inflate(context, wrapperLayoutId, this);
            mContentContainer = (ViewGroup)inflatedLayout.findViewById(R.id.wrapper_content);
        }
        else {
            Log.e("Error", "WrapperLayout: Error reading custom attributes from XML. wrapper_layout = " + wrapperLayoutId);
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params)
    {
        if(mContentContainer == null)
            // Still inflating the wrapper
            super.addView(child, index, params);
        else
            // Inflating content
            mContentContainer.addView(child, index, params);
    }
}
