# Convert files in the md directory into strings_help.xml

from datetime import date
import os
import glob
import markdown
import re

out_file = '../app/src/main/res/values/strings_help.xml'
print "Writing strings to", out_file
with open(out_file, 'wb') as out:
    out.write('''<?xml version="1.0" encoding="utf-8"?>
<!--
  ~ This file is part of FaSoLa Minutes for Android.
  ~ Copyright (c) %d Mike Richards. All rights reserved.
  -->

''' % date.today().year)
    out.write('<!-- Created automatically by md2xml.py -->\n')
    out.write('<resources>\n')

    for filename in glob.glob('md/*.md'):
        id = 'help_' + os.path.splitext(os.path.basename(filename))[0]
        print id
        with open(filename, 'rb') as f:
            text = f.read().decode('utf-8')
            html = markdown.markdown(text)
            # Clean up the html for android
            html = html.replace(' alt=""', '')
            html = html.replace('strong>', 'b>')
            html = html.replace('em>', 'i>')
            html = html.replace(' />', '>')
            # The first line of links should not be wrapped in a <p>
            if html.startswith('<p><b><a'):
                html = re.sub(r'^<p>(<b><a.*?</b>)</p>', r'\1', html)
            html = html.replace("'", r"\'")
            # write to file
            out.write('    <string name="%s" formatted="false">\n' %id)
            out.write('<![CDATA[\n')
            out.write(html.encode('utf-8'))
            out.write('\n')
            out.write(']]>\n')
            out.write('    </string>\n')

    out.write('</resources>\n')

print "done"