package org.fasola.fasolaminutes;

import android.database.Cursor;

/**
 * An indexer that uses integer bins.
 *
 * <p>Sections are specified using the bottom range of the bin.
 * For example, using {@code bins={1, 2, 3, 4}}, the following sections will be created:
 * <ul><li>{@code "1" -> from 1 to 2}
 * <li>{@code "2" -> from 2 to 3}
 * <li>{@code "3" -> from 3 to 4}
 * <li>{@code "4" -> 4 and above}
 * </ul>
 *
 * <p>To create a < 1 bin in this example, use {@code Integer.MIN_VALUE} as the first bin.
 */
public class BinIndexer extends StringIndexer {
    protected int[] mBins;

    /**
     * Creates an Indexer with sections for each bin.
     *
     * @param cursor the data cursor
     * @param sortedColumnIndex the column to index
     * @param bins lower range for each bin
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

    /**
     * Creates a BinIndexer using equal interval sections.
     *
     * @param cursor the data cursor
     * @param sortedColumnIndex the column to index
     * @param sectionCount number of sections
     */
    public static BinIndexer equalIntervals(Cursor cursor, int sortedColumnIndex, int sectionCount) {
        // Init the StringIndexer with the custom sections array
        return new BinIndexer(cursor, sortedColumnIndex, makeBins(cursor, sortedColumnIndex, sectionCount));
    }

    @Override
    protected int compare(String word, int index) {
        // Word is an integer stored as a string
        int n = Integer.parseInt(word);
        // Using bins
        int low = mBins[index];
        int high = mBins[index + 1];
        return n < low ? -1 : (n > high ? 1 : 0);
    }

    /**
     * Makes section labels from a list of bin breaks.
     *
     * @param bins array of bin breaks
     * @return section labels
     */
    protected static String[] makeSections(int[] bins) {
        String[] sections = new String[bins.length];
        for (int i = 0; i < bins.length; i++)
            sections[i] = Integer.toString(bins[i]);
        // Check for MIN_VALUE for bins "< n"
        if (bins.length > 1 && bins[0] == Integer.MIN_VALUE)
            sections[0] = "< " + sections[1];
        return sections;
    }

    /** Makes bins using equal intervals */
    protected static int[] makeBins(Cursor cursor, int sortedColumnIndex, int sectionCount) {
        int[] bins = new int[sectionCount];
        // Get min/max
        int pos = cursor.getPosition();
        if (! cursor.moveToFirst())
            return new int[0];
        double first = cursor.getDouble(sortedColumnIndex);
        cursor.moveToLast();
        double last = cursor.getDouble(sortedColumnIndex);
        cursor.moveToPosition(pos);
        // Calculate bin size and first bin
        // All numbers will be rounded to the binSize order of magnitude
        // For example: if min = 40, max = 2000, and sectionCount = 4
        // bin size is (2000 - 40)/4 = 490  [rounded to the nearest 100] = 500
        double binSize = Math.abs(first - last) / sectionCount;
        double factor = Math.pow(10, Math.floor(Math.log10(binSize)));
        binSize = Math.floor((binSize / factor) + 0.5) * factor;
        double start = Math.floor((Math.min(first, last) / factor) + 0.5) * factor;
        // Make bins
        for (int i = 0; i < bins.length; i++)
            bins[i] = (int)(start + binSize * i);
        return bins;
    }
}
