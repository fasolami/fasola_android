package org.fasola.fasolaminutes;

import android.database.Cursor;

/**
 * A Hacked up version of AlphabetIndexer that uses a full String instead of just the first letter
 */
public class StringIndexer extends LetterIndexer {
    protected String[] mSections;
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
        mSections = sections;
        // Get a Collator for the current locale for string comparisons.
        mCollator = java.text.Collator.getInstance();
        mCollator.setStrength(java.text.Collator.PRIMARY);
    }

    @Override
    public Object[] getSections() {
        return mSections;
    }

    @Override
    protected int compare(String word, String index) {
        // Decode index from char to int to use as the actual index in mSections
        String word2 = mSections[index.codePointAt(0)];
        return mCollator.compare(word, word2);
    }
}
