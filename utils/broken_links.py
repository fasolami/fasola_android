# Chek for broken links

import os
import glob
import re
import markdown

files = set()
for filename in glob.glob('md/*.md'):
    id = os.path.splitext(os.path.basename(filename))[0]
    files.add(id)

for filename in glob.glob('md/*.md'):
    id = os.path.splitext(os.path.basename(filename))[0]
    print "Checking %s" % id
    with open(filename, 'rb') as f:
        text = f.read().decode('utf-8')
        html = markdown.markdown(text)
        for href in re.findall(r'href="([^"]+)"', html):
            if href.startswith('http'):
                continue
            if href not in files:
                print '    ', href;