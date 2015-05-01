package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.support.annotation.NonNull;

/**
 * A Hacked up version of AlphabetIndexer that uses a full String instead of just the first letter
 */
public class StringIndexer extends LetterIndexer {
    protected java.text.Collator mCollator;

    // Make an alphabet String of unicode code points 0 - nSections
    protected static String makeAlphabet(int nSections) {
        int[] intArray = new int[nSections];
        for (int i = 0; i < nSections; ++i)
            intArray[i] = i;
        // Turn it into a (UTF-16) String
        return new String(intArray, 0, intArray.length);
    }

    public StringIndexer(Cursor cursor, int sortedColumnIndex, String[] sections) {
        // Init the AlphabetIndexer with the custom alphabet
        super(cursor, sortedColumnIndex, makeAlphabet(sections.length));
        setSections(sections);
        // Get a Collator for the current locale for string comparisons.
        mCollator = java.text.Collator.getInstance();
        mCollator.setStrength(java.text.Collator.PRIMARY);
    }

    protected String[] getStringSections() {
        return (String[])getSections();
    }

    // Subclasses should override this instead of the AlphabetIndexer compare function
    protected int compare(String word, int index) {
        String word2 = getStringSections()[index];
        return mCollator.compare(word, word2);
    }

    @Override
    protected int compare(@NonNull String word, String index) {
        // Compare using index as an integer, and reverse if sort order is DESC
        return compare(word, index.codePointAt(0)) * (mIsDesc ? -1 : 1);
    }
}
