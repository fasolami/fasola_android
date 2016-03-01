package org.fasola.fasolaminutes;

import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.AbsListView;

import java.lang.reflect.Field;

/**
 * Fix old FastScroller class (< KitKat) to work with long text.
 */
public class FastScrollWrapper {
    /**
     * Wraps FastScroller background in a class that resizes the background and ellipsizes text.
     */
    public static void wrap(AbsListView list) {
        if (! list.isFastScrollEnabled())
            return;
        try {
            Object fastScroll = getField(AbsListView.class, "mFastScroller").get(list);
            Field overlayField = getField(fastScroll, "mOverlayDrawable");
            Drawable background = (Drawable) overlayField.get(fastScroll);
            if (background instanceof FastScrollDrawable)
                return; // Already wrapped
            overlayField.set(fastScroll, new FastScrollDrawable(background, fastScroll));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // getDeclaredField helper functions
    private static Field getField(Class<?> cls, String field) throws NoSuchFieldException {
        Field f = cls.getDeclaredField(field);
        f.setAccessible(true);
        return f;
    }

    private static Field getField(Object obj, String field) throws NoSuchFieldException {
        return getField(obj.getClass(), field);
    }

    private static Object getObject(Object obj, String field) throws NoSuchFieldException, IllegalAccessException {
        return getField(obj, field).get(obj);
    }

    // Wrapper class
    private static class FastScrollDrawable extends LayerDrawable {
        Object mFastScroller;
        Field mTextField;
        RectF mOverlayPos;
        TextPaint mPaint;
        float mPadding;

        public FastScrollDrawable(Drawable background, Object fastScroller) {
            super(new Drawable[]{background});
            mFastScroller = fastScroller;
            try {
                mTextField = getField(fastScroller, "mSectionText");
                mOverlayPos = (RectF)getObject(fastScroller, "mOverlayPos");
                mPaint = new TextPaint((Paint)getObject(fastScroller, "mPaint"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Default fastscroll_overlay_padding is 16dp
            DisplayMetrics m = MinutesApplication.getContext().getResources().getDisplayMetrics();
            mPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, m);
        }

        @Override
        public void setBounds(int left, int top, int right, int bottom) {
            try {
                // Ellipsize text
                String text = (String)mTextField.get(mFastScroller);
                text = TextUtils.ellipsize(text, mPaint, mOverlayPos.right - mPadding * 2,
                        TextUtils.TruncateAt.MIDDLE).toString();
                mTextField.set(mFastScroller, text);
                // Adjust bounds to fit text
                float width = Math.max(mOverlayPos.height(), mPaint.measureText(text) + mPadding * 2);
                mOverlayPos.right += mPadding;
                mOverlayPos.left = right - width;
                super.setBounds((int)mOverlayPos.left, top, (int)mOverlayPos.right, bottom);
            } catch (Exception e) {
                e.printStackTrace();
                super.setBounds(left, top, right, bottom);
            }
        }
    }
}