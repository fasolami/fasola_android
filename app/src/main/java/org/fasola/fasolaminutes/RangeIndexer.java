package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.support.annotation.NonNull;

/**
 * A StringIndexer that uses a range of ints
 */
public class RangeIndexer extends StringIndexer {
    // Make section labels from min and max
    protected static String[] makeSections(int min, int max) {
        if (max < min) {
            int temp = min;
            min = max;
            max = temp;
        }
        String[] sections = new String[max - min + 1];
        for (int i = 0; i < sections.length; i++)
            sections[i] = Integer.toString(i + min);
        return sections;
    }

    // Make section labels from a cursor
    protected static String[] makeSections(@NonNull Cursor cursor, int sortedIndexColumn) {
        if (cursor.getCount() == 0)
            return new String[0];
        // Find the min and max values
        cursor.moveToFirst();
        int min = cursor.getInt(sortedIndexColumn);
        cursor.moveToLast();
        int max = cursor.getInt(sortedIndexColumn);
        cursor.moveToFirst();
        // Create sections
        return makeSections(min, max);
    }

    /**
     * Create an Indexer with sections at each number in a range
     */
    public RangeIndexer(Cursor cursor, int sortedColumnIndex, int min, int max) {
        // Init the StringIndexer with the custom sections array
        super(cursor, sortedColumnIndex, makeSections(min, max));
    }

    public RangeIndexer(@NonNull Cursor cursor, int sortedColumnIndex) {
        super(cursor, sortedColumnIndex, makeSections(cursor, sortedColumnIndex));
    }
}
