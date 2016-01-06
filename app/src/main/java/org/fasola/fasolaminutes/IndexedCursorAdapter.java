package org.fasola.fasolaminutes;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * A CursorAdapter and StickyListHeadersAdapter that implements SectionIndexer.
 *
 * <p>A recording icon (and optional count) are show when a record contains
 * {@link CursorListFragment#AUDIO_COLUMN}.
 *
 * <p>List items can be highlighted using {@link #setHighlight}.
 *
 * <p>Headers can be hidden or shown using {@link #showHeaders}.
 */
public class IndexedCursorAdapter extends SimpleCursorAdapter implements SectionIndexer, StickyListHeadersAdapter {
    protected LetterIndexer mIndexer;
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

    /** Sets recording icon click listeners */
    public void setPlayClickListeners(View.OnClickListener click, View.OnLongClickListener longClick) {
        mClickListener = click;
        mLongClickListener = longClick;
    }

    /** Sets the SectionIndexer */
    public void setIndexer(LetterIndexer indexer) {
        mIndexer = indexer;
        updateIndexerCursor();
    }

    /** Does this Adapter have a SectionIndexer? */
    public boolean hasIndexer() { return mIndexer != null; }

    /** Does this cursor have an index column? */
    public boolean hasIndex() {
        return getCursor() != null && getCursor().getColumnIndex(SQL.INDEX_COLUMN) != -1;
    }

    /** Returns the index of the {@code SQL.INDEX_COLUMN} or -1 */
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

    /** Sets the highlighted record index. */
    public void setHighlight(int position) {
        mHighlight = position;
    }

    /** Gets the highlighted record index. */
    public int getHighlight() {
        return mHighlight;
    }

    /** Shows/hides StickyList headers. */
    public void showHeaders(boolean show) {
        mAreHeadersVisible = show;
    }

    /** Are StickyList headers shown? */
    public boolean areHeadersShown() {
        return hasIndexer() && mAreHeadersVisible;
    }

    // Updates indexer and audio column when the cursor is changed.
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

    // Updates the indexer when the cursor changes.
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
        return mIndexer != null ? mIndexer.getSectionLabels() : null;
    }

    // Shows/hides recording icon and sets highlight.
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = addOrRemoveImage(super.getView(position, convertView, parent), parent);
        if (position == mHighlight)
            view.setBackgroundResource(R.color.tab_background);
        else
            view.setBackgroundColor(Color.TRANSPARENT);
        return view;
    }

    /**
     * Shows or hides the recording icon.
     *
     * <p>Icon is show for records with a non-null/zero {@link CursorListFragment#AUDIO_COLUMN}.
     * If {@code AUDIO_COLUMN} is a number, the value is displayed below the icon.
     *
     * @param view original view
     * @param parent parent view
     * @return the modified view
     */
    protected View addOrRemoveImage(View view, ViewGroup parent) {
        if (mAudioColumn > -1) {
            if (view.getId() != R.id.play_image_layout) {
                // Use the layout with an image button
                LinearLayout layout = (LinearLayout)mInflater.inflate(R.layout.play_image_list_item, parent, false);
                layout.addView(view, 0, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                view = layout;
                View image = view.findViewById(R.id.play_image);
                if (mClickListener != null)
                    image.setOnClickListener(mClickListener);
                if (mLongClickListener != null)
                    image.setOnLongClickListener(mLongClickListener);
            }
            // Change image visibility by audio column
            String label = getCursor().getString(mAudioColumn);
            ImageView playImage = (ImageView)view.findViewById(R.id.play_image);
            TextView recordingCount = (TextView)view.findViewById(R.id.recording_count);
            if (label == null || label.equals("0")) {
                playImage.setVisibility(View.GONE);
                recordingCount.setVisibility(View.GONE);
            }
            else {
                playImage.setVisibility(View.VISIBLE);
                // label is either a recording count or a url, but we only want to display counts.
                // Assume urls are longer and numbers are shorter.
                if (label.length() < 10) {
                    recordingCount.setText(label);
                    recordingCount.setVisibility(View.VISIBLE);
                }
                else {
                    recordingCount.setVisibility(View.GONE);
                }
            }

        }
        else if (view.getId() == R.id.play_image_layout) {
            // Remove the layout and return the original layout
            view = ((LinearLayout)view).getChildAt(0);
        }
        return view;
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
        TextView headerView = (TextView) convertView.findViewById(R.id.list_header_text);
        headerView.setText(text);
        // Set count
        int sectionCount = mIndexer.getCountForSection(section);
        TextView countView = (TextView) convertView.findViewById(R.id.list_header_count);
        countView.setText(String.valueOf(sectionCount));
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        if (areHeadersShown())
            return getSectionForPosition(position);
        // Create a single empty header (with id=0) if headers are invisible.
        return 0;
    }
}
