package org.fasola.fasolaminutes;

import android.content.Context;
import android.database.Cursor;
import android.widget.AlphabetIndexer;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;

// A simple implementation of an alphabetically indexed cursor adapter
public class IndexedCursorAdapter extends SimpleCursorAdapter implements SectionIndexer {
    public static final String INDEX_COLUMN = "indexer_column";
    protected AlphabetIndexer mAlphabetIndexer;
    protected Object[] mSections;

    public IndexedCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flag) {
        super(context, layout, c, from, to, flag);
        // Default alphabet indexer
        initIndexer(c.getColumnCount() > 1 ? 1 : 0, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    public void initIndexer(int sortedColumnIndex, CharSequence alphabet) {
        mAlphabetIndexer = new AlphabetIndexer(getCursor(), sortedColumnIndex, alphabet);
    }

    // Init assuming we have an INDEXER column
    public void initIndexer(CharSequence alphabet) {
        try {
            initIndexer(getCursor().getColumnIndexOrThrow(INDEX_COLUMN), alphabet);
        } catch(IllegalArgumentException ex) {
            // Try first column
            if (getCursor().getColumnCount() > 1)
                initIndexer(1, alphabet);
        }
    }

    // Custom labels for the indexer
    public void setSections(String... sections) {
        mSections = sections;
    }

    // SectionIndexer overrides
    @Override
    public int getPositionForSection(int sectionIndex) {
        return mAlphabetIndexer.getPositionForSection(sectionIndex);
    }

    @Override
    public int getSectionForPosition(int position) {
        return mAlphabetIndexer.getSectionForPosition(position);
    }

    @Override
    public Object[] getSections() {
        if (mSections != null)
            return mSections;
        return mAlphabetIndexer.getSections();
    }
}
