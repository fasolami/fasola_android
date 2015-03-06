package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.widget.AlphabetIndexer;

/**
 * A subclass of up AlphabetIndexer that allows changing the column index
 * This is used as a base class for StringIndexer, and is used in IndexedCursorAdapter
 */
public class LetterIndexer extends AlphabetIndexer {
    public LetterIndexer(Cursor cursor, int sortedColumnIndex, CharSequence alphabet) {
        super(cursor, sortedColumnIndex, alphabet);
    }

    public void setColumnIndex(int column) {
        mColumnIndex = column;
    }
}
