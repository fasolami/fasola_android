/*
 * This file is part of FaSoLa Minutes for Android.
 * Copyright (c) 2016 Mike Richards. All rights reserved.
 */

package org.fasola.fasolaminutes;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.LeadingMarginSpan;
import android.text.style.URLSpan;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ScrollView;
import android.widget.TextView;

import org.xml.sax.XMLReader;

import java.util.Stack;

public class HelpActivity extends BaseActivity {
    public final static String EXTRA_HELP_ID = "org.fasola.fasolaminutes.EXTRA_HELP_ID";
    TextView mText;
    ScrollView mScroller;
    Stack<Pair<Integer, Integer>> mLinkStack = new Stack<>(); // help id, scroll position
    int mCurrentid = -1;
    int mLineHeight;

    // Start help with this help item
    static boolean start(Context context, int id) {
        Intent intent = new Intent(context, HelpActivity.class);
        intent.putExtra(EXTRA_HELP_ID, id);
        try {
            context.startActivity(intent);
            return true;
        } catch(ActivityNotFoundException e) {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Help");
        setContentView(R.layout.activity_help);
        mText = (TextView)findViewById(R.id.help_text);
        mScroller = (ScrollView)findViewById(R.id.scroller);
        mText.setMovementMethod(mLinkHandler);
        mLineHeight = mText.getLineHeight();
        int id = getIntent().getIntExtra(EXTRA_HELP_ID, -1);
        if (id != -1)
            navigateTo(id);
    }

    private void navigateTo(int id, final int scrollPos) {
        if (mCurrentid != -1)
            mLinkStack.push(Pair.create(mCurrentid, mScroller.getScrollY()));
        mCurrentid = id;
        mText.setText(Html.fromHtml(getResources().getString(id), mImageGetter, mTagHandler));
        // Need to let TextView recalc its size before we can actually scroll
        // This unfortunately creates a little flash between when the new text is rendered
        // and the scroll happens, but I don't see a way around it.
        if (scrollPos <= (mText.getMeasuredHeight() - mScroller.getMeasuredHeight())) {
            // If we have enough height in the TextView to do the scroll now, let's do it
            mScroller.scrollTo(0, scrollPos);
        } else {
            // Otherwise we have to wait until after the layout is complete
            mText.post(new Runnable() {
                @Override
                public void run() {
                    mScroller.scrollTo(0, scrollPos);
                }
            });
        }
    }

    private void navigateTo(int id) {
        navigateTo(id, 0);
    }

    @Override
    public void onUpPressed() {
        mLinkStack.clear(); // Clear the stack so we actually go up
        super.onUpPressed();
    }

    @Override
    public void onBackPressed() {
        if (mLinkStack.isEmpty()) {
            super.onBackPressed();
            return;
        }
        // Navigate back
        mCurrentid = -1; // Don't add this to the navigation stack
        Pair<Integer, Integer> pair = mLinkStack.pop();
        navigateTo(pair.first, pair.second);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret = super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_help_activity, menu);
        return ret;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_about) {
            navigateTo(R.string.help_about);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    LinkMovementMethod mLinkHandler = new LinkMovementMethod() {
        public void onLinkClick(String url) {
            if (url.startsWith("http")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            } else {
                String resource = "help_" + url;
                int id = getResources().getIdentifier(resource, "string", HelpActivity.this.getPackageName());
                if (id != 0)
                    navigateTo(id);
            }
        }

        // Lifted from http://stackoverflow.com/questions/1697084/handle-textview-link-click-in-my-android-app
        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP)
                return super.onTouchEvent(widget, buffer, event);

            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);
            if (link.length != 0) {
                onLinkClick(link[0].getURL());
            }
            return true;
        }
    };

    private Html.TagHandler mTagHandler = new Html.TagHandler() {
        int mListItemIndentSp = 15;
        int mBulletIndentSp = 10;
        Stack<LeadingMarginSpan> mListIndents = new Stack<>();

        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            if (tag.equalsIgnoreCase("li")) {
                if (opening) {
                    handleNewline(output);
                    int pos = output.length();
                    output.append("\u2022 ");
                    int first = sp2px((mListIndents.size() + 1) * mListItemIndentSp);
                    int rest = first + sp2px(mBulletIndentSp);
                    LeadingMarginSpan indent = new LeadingMarginSpan.Standard(first, rest);
                    mListIndents.push(indent);
                    output.setSpan(indent, pos, pos, Spannable.SPAN_MARK_MARK);
                } else {
                    handleNewline(output);
                    LeadingMarginSpan indent = mListIndents.pop();
                    int end = output.length();
                    int start = output.getSpanStart(indent);
                    output.removeSpan(indent);
                    if (start != end)
                        output.setSpan(indent, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        private int sp2px(int sp) {
            return (int)(sp * getResources().getDisplayMetrics().scaledDensity);
        }

        // Make sure there is a newline at the end of the text
        private void handleNewline(Editable text) {
            int len = text.length();
            if (len >= 1 && text.charAt(len - 1) != '\n')
                text.append('\n');
        }
    };

    private Html.ImageGetter mImageGetter = new Html.ImageGetter() {
        public Drawable getDrawable(String source) {
            String srcSplit[] = source.split("\\|");
            int id = getResources().getIdentifier(srcSplit[0], "drawable", HelpActivity.this.getPackageName());
            if (id == 0)
                return null;
            // Make a copy of the drawable since we're adding a filter
            Drawable d = getResources().getDrawable(id).getConstantState().newDrawable().mutate();
            d.setColorFilter(new PorterDuffColorFilter(0xff000000, PorterDuff.Mode.SRC_ATOP));
            // source can be specified as "source|nn%" for scaling
            double factor = 1;
            if (srcSplit.length > 1 && srcSplit[1].endsWith("%")) {
                try {
                    double percent = Double.parseDouble(srcSplit[1].substring(0, srcSplit[1].length() - 1));
                    factor = percent / 100.;
                } catch(NumberFormatException e) {
                    // Factor is still 1
                }
            }
            // scale to line height
            double heightFactor = (double)mLineHeight / ((double)d.getIntrinsicHeight());
            factor *= heightFactor;
            int w = (int)((double)d.getIntrinsicWidth() * factor);
            int h = (int)((double)d.getIntrinsicHeight() * factor);
            d.setBounds(0, 0, w, h);
            return d;
        }
    };
}
