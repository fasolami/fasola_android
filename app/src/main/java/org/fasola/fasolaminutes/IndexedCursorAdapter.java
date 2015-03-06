package org.fasola.fasolaminutes;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * A CursorAdapter and StickyListHeadersAdapter that implements SectionIndexer
 */
public class IndexedCursorAdapter extends SimpleCursorAdapter implements SectionIndexer, StickyListHeadersAdapter {
    public static final String INDEX_COLUMN = "indexer_column";
    protected LetterIndexer mIndexer;
    protected Object[] mSections;
    protected LayoutInflater mInflater;

    public IndexedCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flag) {
        super(context, layout, c, from, to, flag);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
        return cursor != null && cursor.getColumnIndex(INDEX_COLUMN) != -1;
    }

    // Find the index column
    protected int getIndexColumn() {
        Cursor cursor = getCursor();
        if (cursor == null)
            return -1;
        int col = cursor.getColumnIndex(INDEX_COLUMN);
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
        return oldCursor;
    }

    // Update the indexer when the cursor changes
    protected void updateIndexerCursor() {
        if (! hasIndexer())
            return;
        Cursor cursor = getCursor();
        int column = getIndexColumn();
        if (column != -1) {
            // Update the indexer cursor/columns
            mIndexer.setCursor(cursor);
            mIndexer.setColumnIndex(column);
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

    // StickyListHeadersAdapter overrides
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        // Create the view
        if (convertView == null)
            convertView = mInflater.inflate(R.layout.sticky_list_header, parent, false);
        // Set the text
        TextView text = (TextView) convertView.findViewById(android.R.id.text1);
        text.setText(getSections()[getSectionForPosition(position)].toString());
        return convertView;
    }

    public long getHeaderId(int position) {
        return getSectionForPosition(position);
    }
}
