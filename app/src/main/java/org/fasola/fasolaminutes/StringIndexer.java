package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.support.annotation.NonNull;

import java.util.ArrayList;

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
        setSections(sections);
        // Get a Collator for the current locale for string comparisons.
        mCollator = java.text.Collator.getInstance();
        mCollator.setStrength(java.text.Collator.PRIMARY);
    }

    /**
     * Creates a StringIndexer using all strings found in the cursor.
     *
     * @param cursor the data cursor
     * @param sortedColumnIndex the column to index
     */
    public StringIndexer(Cursor cursor, int sortedColumnIndex) {
        this(cursor, sortedColumnIndex, makeSections(cursor, sortedColumnIndex));
    }

    /** Gets sections casted to a string array */
    protected String[] getStringSections() {
        return (String[])getSections();
    }

    /** Override this function instead of {@link #compare(String, String)} */
    protected int compare(String word, int index) {
        String word2 = getStringSections()[index];
        return mCollator.compare(word, word2);
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
