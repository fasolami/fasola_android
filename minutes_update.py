"""Script to update schema from the iOS database"""
import sqlite3
import os, sys, re

FORCE_UPDATE = True
COMMIT_CHANGES = True
DATABASE_PATH = r'app/src/main/assets/databases/minutes.db'

dirname = os.path.dirname(sys.argv[0])
dbname = os.path.join(dirname, DATABASE_PATH)
print "Opening database: %r" % dbname
db = sqlite3.connect(dbname, detect_types=sqlite3.PARSE_DECLTYPES)

class Action(object):
    desc = None
    sql = None
    column = None
    group = None
    DONE = False
    skip = False

    def run(self):
        """Override this in subclasses"""
        if self.sql:
            return self.run_sql()

    def condition(self):
        """Override in subclass and return True to run this action"""
        if self.column:
            # Check for column already exists
            match = re.match(
                r'ALTER\s+TABLE\s+(\w+)\s+ADD\s+COLUMN\s+(\w+)\s+',
                self.column, re.I)
            if not match:
                raise Exceition("Bad ALTER TABLE column syntax")
            return not self.col_exists(match.group(1), match.group(2))
        else:
            return True

    def _run(self):
        """The actual run command"""
        print self.desc
        print '=' * 80
        if self.skip or not (FORCE_UPDATE or self.condition()):
            print "(skipping)"
            print
            return
        # add a column if specified
        if self.column:
            self.create_column()
        # run the action
        count = self.run()
        if count is not None:
            print "updated %d records" % count
        self.DONE = True
        print "done"
        print

    def run_sql(self, sql=None):
        """Run a statement or file and return the number of affected rows"""
        sql = sql or self.sql
        if sql:
            if sql.endswith('.sql'):
                return self.execute_file(os.path.join(dirname, sql))
            else:
                return db.execute(sql).rowcount

    def create_column(self):
        try:
            db.execute(self.column)
        except sqlite3.OperationalError as e:
            if not e.message.startswith('duplicate column'):
                print "Error:", e

    def execute_file(self, filename):
        try:
            count = 0
            with open(filename, 'r') as f:
                for line in re.split(r';\r?\n', f.read()):
                    count += db.execute(line.strip().decode('utf-8')).rowcount
            return count
        except IOError as e:
            print "Error", e

    def col_exists(self, table, col):
        """Test whether this column exists in the specified table"""
        try:
            db.execute("SELECT %s FROM %s" % (col, table))
            return True
        except sqlite3.OperationalError:
            return False

# ----------------------------------------------------------------------------
# Fix code page problems
# ----------------------------------------------------------------------------
class CodePage(Action):
    desc = "Fixing code page problems (try UTF-8, fallback on Mac Roman)"

    def run(self):
        count = 0

        # codepage fixing function
        def parse_text(text):
            """Parse as either UTF-8 or Mac Roman"""
            if self.DONE:
                return text.decode('utf-8')
            try:
                return text.decode('utf-8')
            except UnicodeDecodeError:
                count += 1
                return text.decode('mac-roman')

        sqlite3.register_converter("TEXT", parse_text)

        for table, in db.execute("SELECT name FROM sqlite_master WHERE type = 'table'"):
            print "fixing table %s..." % table,
            cursor = db.execute("SELECT * FROM %s" % table)
            for row in cursor:
                # Make a list of text fields with their respective values
                id = None
                idfield = None
                fields = []
                values = []
                for desc, value in zip(cursor.description, row):
                    field = desc[0]
                    # Assume first column is id
                    if desc == cursor.description[0]:
                        idfield = field
                        id = value
                    elif isinstance(value, basestring):
                        fields.append(field)
                        values.append(value)
                # No text columns in this table, skip it
                if len(fields) == 0:
                    break
                # Add id field for WHERE clause
                values.append(id)
                # Create SQL statement and execute
                set_stmt = ', '.join(f + "=?" for f in fields)
                stmt = "UPDATE %s SET %s WHERE %s = ?" % (table, set_stmt, idfield)
                db.execute(stmt, values)

            print 'converted %d strings to utf-8' % count

# ----------------------------------------------------------------------------
# Fix literal "\n" text in minutes
# ----------------------------------------------------------------------------
class MinutesNewlines(Action):
    desc = "Replacing literal newlines in minutes text"

    def run(self):
        newline_re = re.compile(r"\s*\\n+\s*")
        count = 0
        for (id, name, location, minutes) in db.execute("SELECT id, Name, Location, Minutes FROM minutes"):
            if "\\n" in name + location + minutes:
                count += 1
                # Set lead_id
                db.execute(
                    "UPDATE minutes SET Name = ?, Location = ?, Minutes = ? WHERE id = ?",
                    (newline_re.sub(" ", name),
                     newline_re.sub(" ", location),
                     newline_re.sub(" ", minutes),
                     id)
                )
        return count


# ----------------------------------------------------------------------------
# Replace Vertical tabs in song lyrics
# ----------------------------------------------------------------------------
class LyricsVT(Action):
    desc = "Replacing vertical tab with newline in lyrics"
    sql = """
        UPDATE songs
        SET SongText = (
            SELECT replace(src.SongText, '', '\n')
            FROM songs src
            WHERE src.id = songs.id
        )
        WHERE songs.SongText LIKE '%%'
    """

# ----------------------------------------------------------------------------
# Replace Vertical tabs in minutes text
# ----------------------------------------------------------------------------
class MinutesVT(Action):
    desc = "Replacing vertical tab with newline in minutes"
    sql = """
        UPDATE minutes
        SET Minutes = (
            SELECT replace(src.Minutes, '', '\n\n')
            FROM minutes src
            WHERE src.id = minutes.id
        )
        WHERE minutes.Minutes LIKE '%%'
    """


# ----------------------------------------------------------------------------
# Add leader columns
# ----------------------------------------------------------------------------
class LastName(Action):
    desc = "Adding 'last_name' column to 'leaders' table"
    column = 'ALTER TABLE leaders ADD COLUMN last_name TEXT DEFAULT NOT NULL'

    def run(self):
        count = 0
        for (id, name) in db.execute("SELECT id, name FROM leaders"):
            db.execute(
                "UPDATE leaders SET last_name = ? WHERE id = ?",
                (name.rsplit(None, 1)[-1], id)
            )
            count += 1
        return count

# ----------------------------------------------------------------------------
# Add lead_id
# ----------------------------------------------------------------------------
class LeadId(Action):
    desc = "Adding 'lead_id' column to 'song_leader_joins' table"
    column = 'ALTER TABLE song_leader_joins ADD COLUMN lead_id INT'
    index = 'lead_index'

    def run(self):
        lead_id = 0
        last_song_singing = None
        for (id, song, singing) in db.execute("SELECT id, song_id, minutes_id FROM song_leader_joins"):
            # increment lead_id when the lead changes (sequential song/singing combo)
            song_singing = "%s_%s" % (song, singing)
            if last_song_singing != song_singing:
                lead_id += 1
                last_song_singing = song_singing
            # Set lead_id
            db.execute(
                "UPDATE song_leader_joins SET lead_id = ? WHERE id = ?",
                (lead_id, id)
            )
        print "Set lead_id for %d leads" % lead_id
        # Add an index
        db.execute("CREATE INDEX IF NOT EXISTS lead_index ON song_leader_joins (lead_id)");

# ----------------------------------------------------------------------------
# Fix song stats (distinct lead_id = one lead, instead of each leader_id)
# ----------------------------------------------------------------------------
# NB: The actual query should look something like this
#        SELECT song_id, minutes.Year, count(DISTINCT lead_id)
#        FROM song_leader_joins
#        JOIN minutes ON minutes.id = song_leader_joins.minutes_id
#        GROUP BY song_id, minutes.Year
#        ORDER BY minutes.Year ASC, count(DISTINCT lead_id) DESC
# but:
#  (a) it takes a while to execute; and
#  (b) it's very hard to get the yearly rank as part of the query,
# so we'll take a different approach

from collections import defaultdict

class FixStats(Action):
    desc = "Fixing table 'song_stats'"

    def run(self):
        ranks = self.build_ranks()
        # values = [(song_id, year, lead_count, rank), ...]
        values = []
        for year in sorted(ranks.keys()):
            rank = 1
            last_count = 0
            for i, (count, song_id) in enumerate(ranks[year]):
                if count != last_count:
                    rank = i + 1
                last_count = count
                values.append((song_id, year, count, rank))
        # Clear and repopulate table
        db.execute("DELETE FROM song_stats")
        db.execute("DELETE FROM sqlite_sequence WHERE name='song_stats'")
        db.executemany("INSERT INTO song_stats (song_id, year, lead_count, rank) VALUES (?, ?, ?, ?)", values)

    def build_ranks(self):
        # ranks[year] = sorted [(count, song_id), ...]
        counts = self.build_counts()
        ranks = defaultdict(list)
        for song_id, yearcount in counts.iteritems():
            for year, count in yearcount.iteritems():
                ranks[year].append((count, song_id))
        for data in ranks.itervalues():
            data.sort(reverse=True)
        return ranks

    def build_counts(self):
        # counts[song_id][year] = count
        counts = {}
        years = db.execute("SELECT DISTINCT year FROM minutes").fetchall()
        for song_id, in db.execute("SELECT id FROM songs"):
            counts[song_id] = {}
            for year, in years:
                counts[song_id][year] = 0

        # Get leads by song and year; compute counts
        cursor = db.execute("""
            SELECT DISTINCT lead_id, song_id, minutes.Year
            FROM song_leader_joins
            JOIN minutes ON song_leader_joins.minutes_id = minutes.id""")
        for lead_id, song_id, year in cursor:
            counts[song_id][year] += 1

        return counts

# ----------------------------------------------------------------------------
# Clean empty string recordings
# ----------------------------------------------------------------------------
class NullAudioUrl(Action):
    desc = "Replacing 'audio_url' empty strings with NULLs in 'song_leader_joins' table"
    sql = """
        UPDATE song_leader_joins
        SET audio_url = NULL
        WHERE audio_url = ''
    """


# ----------------------------------------------------------------------------
# Add RecordingCt
# ----------------------------------------------------------------------------
class RecordingCt(Action):
    desc = "Adding 'RecordingCt' column to 'minutes' table"
    column = 'ALTER TABLE minutes ADD COLUMN RecordingCt INT'
    sql = """
        UPDATE minutes
        SET RecordingCt = (
            SELECT COUNT(DISTINCT song_leader_joins.lead_id)
            FROM song_leader_joins
            WHERE
                song_leader_joins.minutes_id == minutes.id AND
                song_leader_joins.audio_url IS NOT NULL
        )
    """


# ----------------------------------------------------------------------------
# Fix composer and poet data
# ----------------------------------------------------------------------------

def makenames(aFirst, aLast, aDate, bFirst, bLast, bDate, book):
    """Return a poet/composer line with names and dates"""
    def namejoin(a, b):
        return a + ' '  + b if a and b else a + b
    # Make book italic
    if book:
        # Check for "Name's Book Title" and don't italicize Name
        s = re.split(ur'(\u2019s?\s)', book, maxsplit=1)
        if len(s) == 3:
            book = '%s%s<i>%s</i>' % (s[0], s[1], s[2])
        else:
            book = '<i>%s</i>' % book
    names = namelistjoin(
        namejoin(aFirst, aLast),
        namejoin(bFirst, bLast),
        book
    )
    if aDate:
        if names:
            return ', '.join((names, aDate))
        else:
            return aDate
    elif aDate and bDate:
        return '; '.join((
            namejoin(aFirst, aLast) + ', ' + aDate,
            (namejoin(bFirst, bLast) or book) + ', ' + bDate
        ))
    return names

def namelistjoin(*args):
    """Join a list of names like so: a, b & c"""
    names = []
    for arg in args:
        if arg:
            names.append(arg)
    if len(names) > 2:
        return ', '.join(names[:2]) + ' & ' + names[-1]
    else:
        return ' & '.join(names)


class UpdatePoetData(Action):
    desc = "Updating poet data from SQL file"
    sql = 'poet_update.sql'

class AddComposerColumn(Action):
    desc = "Adding 'composer' column to 'songs' table"
    column = "ALTER TABLE songs ADD COLUMN composer TEXT DEFAULT NULL"
    arranged = set([
        '31t', '31b','32t', '33t', '33b', '37b', '41', '46', '51', '53', '62',
        '72b', '74b', '76t', '77t', '79', '80b', '82t', '82b', '87t', '93', '94',
        '95', '96', '97', '98', '99',

        '100', '101t', '102', '104', '108b', '109', '113', '116', '120', '122',
        '123t', '124', '128', '133', '153', '154', '160b', '161', '162', '176t',

        '204', '229', '231', '267', '294',

        '309', '310', '312t', '323b', '332', '334', '335', '338', '339', '341',
        '346', '354b', '378b', '385b', '388', '399b',

        '412', '413', '414', '421', '457', '496',

        '513', '551', '567',
    ])

    def run(self):
        stmt = "SELECT PageNum, Comp1First, Comp1Last, Comp1Date, Comp2First, Comp2Last, Comp2Date, CompBookTitle FROM songs"
        for (page, aFirst, aLast, aDate, bFirst, bLast, bDate, book) in db.execute(stmt):
            name = makenames(
                aFirst, aLast, aDate,
                bFirst, bLast, bDate,
                book
            )
            if page in self.arranged:
                name = 'Arr. ' + name
            db.execute(
                "UPDATE songs SET composer = ? WHERE PageNum = ?",
                (name, page)
            )
        # Additional fixes from this file
        self.run_sql('fix_composer.sql')


class AddPoetColumn(Action):
    desc = "Adding 'poet' column to 'songs' table"
    column = "ALTER TABLE songs ADD COLUMN poet TEXT DEFAULT NULL"
    arranged = set(['101t', '119', '156', '294'])

    def run(self):
        stmt = "SELECT PageNum, Poet1First, Poet1Last, Poet1Date, Poet2First, Poet2Last, Poet2Date, PoetBookTitle FROM songs"
        for (page, aFirst, aLast, aDate, bFirst, bLast, bDate, book) in db.execute(stmt):
            name = makenames(
                aFirst, aLast, aDate,
                bFirst, bLast, bDate,
                book
            )
            if page in self.arranged:
                name = 'Arr. ' + name
            db.execute(
                "UPDATE songs SET poet = ? WHERE PageNum = ?",
                (name, page)
            )
        # Additional fixes from this file
        self.run_sql('fix_poet.sql')

# ----------------------------------------------------------------------------
# Drop the tables we don't use (might use these later)
# ----------------------------------------------------------------------------
class DropTables(Action):
    desc = "Dropping tables we don't need"
    bad_tables = (
        'locations',
        'locations_old',
        'minute_location_singing_joins',
        'leader_name_invalid',
        'singings',
        'leader_name_aliases',
    )

    def run(self):
        for t in self.bad_tables:
            print "    %s" % t
            db.execute("DROP TABLE IF EXISTS %s" % t)

# ----------------------------------------------------------------------------
# Run
# ----------------------------------------------------------------------------

if __name__ == '__main__':
    # I'm assuming __subclasses__ is in order of definition
    for cls in Action.__subclasses__():
        action = cls()
        action._run()

    if COMMIT_CHANGES:
        print "Committing"
        db.commit()
        print "Vacuuming db"
        db.execute("VACUUM")
        db.close()
        print "Done"
