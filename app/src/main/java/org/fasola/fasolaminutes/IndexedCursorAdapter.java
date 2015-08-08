package org.fasola.fasolaminutes;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * A CursorAdapter and StickyListHeadersAdapter that implements SectionIndexer
 */
public class IndexedCursorAdapter extends SimpleCursorAdapter implements SectionIndexer, StickyListHeadersAdapter {
    protected LetterIndexer mIndexer;
    protected Object[] mSections;
    protected LayoutInflater mInflater;
    boolean mAreHeadersVisible = true;
    int mHighlight = -1;
    int mAudioColumn = -1;
    View.OnClickListener mClickListener;
    View.OnLongClickListener mLongClickListener;

    public IndexedCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flag) {
        super(context, layout, c, from, to, flag);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public IndexedCursorAdapter(Context context, int layout) {
        this(context, layout, null, null, null, 0);
    }

    // Call immediately after constructing
    public void setPlayClickListeners(View.OnClickListener click, View.OnLongClickListener longClick) {
        mClickListener = click;
        mLongClickListener = longClick;
    }

    // Custom labels for the indexer
    public void setSections(String... sections) {
        mSections = sections;
    }

    // Set a SectionIndexer
    public void setIndexer(LetterIndexer indexer) {
        mIndexer = indexer;
        updateIndexerCursor();
    }

    public boolean hasIndexer() { return mIndexer != null; }

    // Does this cursor have an index column?
    public boolean hasIndex() {
        return hasIndex(getCursor());
    }

    public static boolean hasIndex(Cursor cursor) {
        return cursor != null && cursor.getColumnIndex(SQL.INDEX_COLUMN) != -1;
    }

    // Find the index column
    protected int getIndexColumn() {
        return getIndexColumn(getCursor());
    }

    public static int getIndexColumn(Cursor cursor) {
        if (cursor == null)
            return -1;
        int col = cursor.getColumnIndex(SQL.INDEX_COLUMN);
        if (col != -1)
            return col;
        // Try first column
        if (cursor.getColumnCount() > 1)
            return 1;
        // Nothing
        return -1;
    }

    // Automatically update indexer when the cursor is changed
    @Override
    public Cursor swapCursor(Cursor newCursor) {
        Cursor oldCursor = super.swapCursor(newCursor);
        updateIndexerCursor();
        updateAudioColumn();
        return oldCursor;
    }

    private void updateAudioColumn() {
        Cursor cursor = getCursor();
        if (cursor == null)
            mAudioColumn = -1;
        else
            mAudioColumn = cursor.getColumnIndex(CursorListFragment.AUDIO_COLUMN);
    }

    // Update the indexer when the cursor changes
    private void updateIndexerCursor() {
        // Update indexer
        if (! hasIndexer())
            return;
        Cursor cursor = getCursor();
        int column = getIndexColumn(cursor);
        if (column != -1) {
            // Update the indexer cursor/columns
            mIndexer.setCursor(cursor, column);
        }
        else
            mIndexer.setCursor(null);
    }
    // SectionIndexer overrides
    @Override
    public int getPositionForSection(int sectionIndex) {
        return mIndexer.getPositionForSection(sectionIndex);
    }

    @Override
    public int getSectionForPosition(int position) {
        return mIndexer.getSectionForPosition(position);
    }

    @Override
    public Object[] getSections() {
        if (mSections != null)
            return mSections;
        return mIndexer.getSections();
    }

    public void showHeaders(boolean show) {
        mAreHeadersVisible = show;
    }

    public boolean areHeadersShown() {
        return hasIndexer() && mAreHeadersVisible;
    }

    // StickyListHeadersAdapter overrides
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        if (! areHeadersShown())
            return new View(parent.getContext());
        // Create the view
        // If convertView is not a ViewGroup, it was created as an empty View above
        if (convertView == null || ! (convertView instanceof ViewGroup))
            convertView = mInflater.inflate(R.layout.sticky_list_header, parent, false);
        // Set the text
        int section = getSectionForPosition(position);
        String text = getSections()[section].toString();
        // Chop off [] at the beginning of the header string
        // Use the following construct to sort StringIndexer:
        // [1]One
        // [2]Two
        // [3]Three
        if (! text.isEmpty() && text.charAt(0) == '[') {
            int pos = text.indexOf(']');
            if (pos != -1)
                text = text.substring(pos+1);
        }
        TextView headerView = (TextView) convertView.findViewById(R.id.list_header_text);
        headerView.setText(text);
        // Set count
        int sectionCount = mIndexer.getCountForSection(section);
        TextView countView = (TextView) convertView.findViewById(R.id.list_header_count);
        countView.setText(String.valueOf(sectionCount));
        return convertView;
    }

    public long getHeaderId(int position) {
        // Only create a single header (which will be empty) if headers are invisible
        if (areHeadersShown())
            return getSectionForPosition(position);
        return 0;
    }

    // Highlight
    public int getHighlight() {
        return mHighlight;
    }

    public void setHighlight(int position) {
        mHighlight = position;
    }

    // Play button

    // Change layout if Cursor has an audioUrl
    protected View addOrRemoveImage(View view, ViewGroup parent) {
        if (mAudioColumn > -1) {
            if (view.getId() != R.id.play_image_layout) {
                // Use the layout with an image button
                LinearLayout layout = (LinearLayout)mInflater.inflate(R.layout.play_image_list_item, parent, false);
                layout.addView(view, 0, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                view = layout;
                View image = view.findViewById(R.id.play_image);
                image.setOnClickListener(mClickListener);
                image.setOnLongClickListener(mLongClickListener);
            }
            // Change image visibility by audioUrl column
            view.findViewById(R.id.play_image).setVisibility(
                    getCursor().isNull(mAudioColumn) ? View.GONE : View.VISIBLE);
        }
        else if (view.getId() == R.id.play_image_layout) {
            // Remove the layout and return the original layout
            view = ((LinearLayout)view).getChildAt(0);
        }
        return view;
    }

    // The custom view: check for a play button and highlight
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = addOrRemoveImage(super.getView(position, convertView, parent), parent);
        if (position == mHighlight)
            view.setBackgroundResource(R.color.tab_background);
        else
            view.setBackgroundColor(Color.TRANSPARENT);
        return view;
    }
}
