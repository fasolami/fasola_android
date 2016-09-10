/*
 * This file is part of FaSoLa Minutes for Android.
 * Copyright (c) 2016 Mike Richards. All rights reserved.
 */

package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.support.annotation.NonNull;

/**
 * An indexer with sections between a range of integers.
 *
 * <p>For example, using a range indexer with {@code min=1} and {@code max=10} will yield the
 * following sections: {@code "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"}.
 */
public class RangeIndexer extends StringIndexer {
    /** Creates an indexer using min and max values from the cursor. */
    public RangeIndexer(Cursor cursor, int sortedColumnIndex, int min, int max) {
        // Init the StringIndexer with the custom sections array
        super(cursor, sortedColumnIndex, makeSections(min, max));
    }

    /** Creates an indexer using min and max values from the cursor. */
    public RangeIndexer(@NonNull Cursor cursor, int sortedColumnIndex) {
        super(cursor, sortedColumnIndex, makeSections(cursor, sortedColumnIndex));
    }

    /**
     * Makes section labels from min and max
     *
     * @param min lower range
     * @param max upper range
     * @return section labels
     */
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

    /**
     * Makes section labels using min and max values from the cursor.
     *
     * @param cursor data cursor
     * @param sortedIndexColumn column to use (must be sorted)
     * @return section labels
     */
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
}
