package org.fasola.fasolaminutes;

import android.database.Cursor;

/**
 * A Hacked up version of AlphabetIndexer that uses int bins instead of a String alphabet
 */
public class BinIndexer extends StringIndexer {
    protected int[] mBins;

    // Make section labels from bins
    protected static String[] makeSections(int[] bins) {
        String[] sections = new String[bins.length];
        for (int i = 0; i < bins.length; i++)
            sections[i] = Integer.toString(bins[i]);
        // Check for MIN_VALUE for bins "< n"
        if (bins.length > 1 && bins[0] == Integer.MIN_VALUE)
            sections[0] = "< " + sections[1];
        return sections;
    }

    /**
     * Create an Indexer with sections at each bin where bins is an array of bin values.
     * To create bins for all < a number or all > a number, use Integer.MIN_VALUE and Integer.MAX_VALUE
     */
    public BinIndexer(Cursor cursor, int sortedColumnIndex, int[] bins) {
        // Init the StringIndexer with the custom sections array
        super(cursor, sortedColumnIndex, makeSections(bins));
        // Set bins
        mBins = new int[bins.length + 1];
        System.arraycopy(bins, 0, mBins, 0, bins.length);
        // Add an extra bin boundary for the upper limit
        mBins[mBins.length - 1] = Integer.MAX_VALUE;
    }

    @Override
    protected int compare(String word, String letter) {
        // Word is an integer stored as a string
        int n = Integer.parseInt(word);
        // letter is a unicode code point used as an index to mBins
        int index = letter.codePointAt(0);
        // Using bins
        int low = mBins[index];
        int high = mBins[index + 1];
        return n < low ? -1 : (n > high ? 1 : 0);
    }
}
