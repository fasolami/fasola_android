/*
 * This file is part of FaSoLa Minutes for Android.
 * Copyright (c) 2016 Mike Richards. All rights reserved.
 */

package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An indexer that uses a full string instead of just the first letter.
 */
public class StringIndexer extends LetterIndexer {
    protected java.text.Collator mCollator;

    /**
     * Creates a StringIndexer using specified strings.
     *
     * @param cursor the data cursor
     * @param sortedColumnIndex the column to index
     * @param sections strings to use for indexing and for section headers
     */
    public StringIndexer(Cursor cursor, int sortedColumnIndex, String[] sections) {
        // Init the AlphabetIndexer with the custom alphabet
        super(cursor, sortedColumnIndex, makeAlphabet(sections.length));
        // Get a Collator for the current locale for string comparisons.
        mCollator = java.text.Collator.getInstance();
        mCollator.setStrength(java.text.Collator.PRIMARY);
        setSections(sections);
        mIsSorted = true;
    }

    /**
     * Creates a StringIndexer using all strings found in the cursor.
     *
     * @param cursor the data cursor
     * @param sortedColumnIndex the column to index
     */
    public StringIndexer(Cursor cursor, int sortedColumnIndex) {
        this(cursor, sortedColumnIndex, makeSections(cursor, sortedColumnIndex));
        // Check to see if sections are sorted
        mIsSorted = true;
        String[] sections = getStringSections();
        if (sections.length > 1) {
            int sortCheck = mCollator.compare(sections[0], sections[1]);
            for (int i=2; i<sections.length; ++i) {
                if (mCollator.compare(sections[i-1], sections[i]) != sortCheck) {
                    mIsSorted = false;
                    break;
                }
            }
            // If sections are currently sorted desc, reverse them
            if (mIsSorted && sortCheck == 1) {
                mAlphabet = new StringBuilder(mAlphabet).reverse().toString();
                List<String> sectionReverser = Arrays.asList(sections);
                Collections.reverse(sectionReverser);
                sectionReverser.toArray(sections);
            }
        }
    }

    /** Gets sections casted to a string array */
    protected String[] getStringSections() {
        return (String[])getSections();
    }

    /** Override this function instead of {@link #compare(String, String)} */
    protected int compare(String word, int index) {
        if (mIsSorted) {
            String word2 = getStringSections()[index];
            return mCollator.compare(word, word2);
        }
        else {
            String[] sections = getStringSections();
            for (int i=0; i<sections.length; ++i) {
                if (sections[i].equals(word))
                    return i - index;
            }
        }
        return 1;
    }

    @Override
    protected int compare(@NonNull String word, String index) {
        // Compare using index as an integer, and reverse if sort order is DESC
        return compare(word, index.codePointAt(0)) * (mIsDesc ? -1 : 1);
    }

    /**
     * Makes section labels using all strings in the cursor.
     *
     * @param cursor data cursor
     * @param sortedIndexColumn column to use (must be sorted)
     * @return section labels
     */
    protected static String[] makeSections(@NonNull Cursor cursor, int sortedIndexColumn) {
        if (! cursor.moveToFirst())
            return new String[0];
        // Find all strings
        String last = "";
        ArrayList<String> sections = new ArrayList<>();
        do {
            String current = cursor.getString(sortedIndexColumn);
            if (! current.equals(last)) {
                last = current;
                sections.add(current);
            }
        } while(cursor.moveToNext());
        return sections.toArray(new String[sections.size()]);
    }

    /**
     * Makes an alphabet String of unicode code points from 0 to {@code nSections}.
     *
     * <p>Code points are used as indices for the actual sections.
     *
     * @param nSections number of sections needed
     * @return String of unicode points (starting from 0)
     */
    protected static String makeAlphabet(int nSections) {
        int[] intArray = new int[nSections];
        for (int i = 0; i < nSections; ++i)
            intArray[i] = i;
        // Turn it into a (UTF-16) String
        return new String(intArray, 0, intArray.length);
    }
}
