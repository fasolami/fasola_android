"""Find typos and alternate names for leaders in the minutes database.

Run through each replacement in STEPS and print matches each time. Each step
is more likely to return incorrect matches than the previous step.
"""

from __future__ import print_function
import sqlite3
import re, string

DATABASE_PATH = r'app/src/main/assets_/databases/minutes.db'

# ('Description', 'pattern', 'replace")
STEPS = (
    ('Uppercase I -> Lowercase L',
        r'(\S)I', r'\1l'),
    ('Lowercase',
        '.*', lambda match: match.group().lower()),
    ('Add space after "." for abbreviations',
        r'\.(\S)', r'. \1'),
    ('Smart apostrophes -> straight apostrophes',
        u'\u2019', "'"),
    ('Hyphens -> spaces',
        r'\-', r' '),
    ('Remove punctuation',
        '[' + re.escape(string.punctuation) + ']', ''),
    ('Remove Trailing E and H',
        r'[eh]\b', r''),
    ('Remove Trailing ES',
        r'es\b', r's'),
    ('Double letters -> single letters',
        r'(.)\1+', r'\1'),
    ('ey/ye -> y',
        'ey|ye', 'y'),
    ('ei/ie/y -> e',
        'ei|ie|y', 'e'),
    ('Replace multiple vowels',
        '([aeiouy])[aeiouy]+', r'\1'),
    ('Wildcard vowels',
        '[aeiouy]', '*'),
)

class Node:
    def __init__(self, name, lead_count):
        self.key = name
        self.canonical_name = name
        self.names = [name + " (" + str(lead_count) + ")"]
        self.lead_count = lead_count

    def combine(self, other):
        if self.lead_count < other.lead_count:
            return other.combine(self)
        # Combine nodes
        self.names.extend(other.names)
        self.lead_count += other.lead_count
        # Copy over lead_count and canonical_name to the other node
        other.lead_count = self.lead_count
        other.canonical_name = self.canonical_name
        return self

def get_names():
    """Return a dict of Nodes from the leaders table
    
    Dict format is {key: Node(name, lead_count)}
    """
    db = sqlite3.connect(DATABASE_PATH)
    cursor = db.execute("SELECT name, lead_count FROM leaders");
    names = {}
    for name, lead_count in cursor.fetchall():
        node = Node(name, lead_count)
        names[node.key] = node
    cursor.close()
    return names

def process_name(name, step=-1):
    """Transform a name using replacement rules defined in STEPS.
    
    If step is given, apply just that step, otherwise apply all steps.
    """
    if step == -1:
        # Do all of the levels
        for desc, pat, replace in STEPS:
            name = re.sub(pat, replace, name)
        return name
    else:
        # Single step
        desc, pat, replace = STEPS[step]
        return re.sub(pat, replace, name)

def get_nonascii(name):
    """Return a string stripped of all non-ascii characters.
    
    Used to check for missing unicode characters in the replacement table.
    """
    return re.sub(r'[\x00-\x80]', '', name)

if __name__ == '__main__':
    name_dict = get_names()
    total_changes = 0
    for step in xrange(len(STEPS)):
        name_list = name_dict.values()
        name_dict = {}
        matches = {} # name_dict entries that have been combined
        changes = 0
        # Run through the nodes and combine
        for node in name_list:
            node.key = process_name(node.key, step)
            try:
                name_dict[node.key] = node.combine(name_dict[node.key])
                matches[node.key] = node
                changes += 1
            except KeyError:
                name_dict[node.key] = node
        total_changes += changes
        # Print any changes
        print('Pass %d: %s' % (step + 1, STEPS[step][0]))
        print('=' * 60)
        for key in matches:
            node = name_dict[key]
            print(repr(node.key), repr(node.names))
        print()
        print('%d changes' % changes if changes > 0 else '[No changes]')
        raw_input('Press any key to continue...')
        print()
    print('Done')
    print('Total changes: %d' % total_changes)
