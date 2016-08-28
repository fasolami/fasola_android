#!/usr/bin/env python
 
import sys, os
import webbrowser
import BaseHTTPServer
from SimpleHTTPServer import SimpleHTTPRequestHandler

os.chdir('..') # So that we can access anything in the FaSoLaMinutes directory

HandlerClass = SimpleHTTPRequestHandler
ServerClass  = BaseHTTPServer.HTTPServer
Protocol     = "HTTP/1.0"

port = 8080
server_address = ('127.0.0.1', port)

HandlerClass.protocol_version = Protocol
httpd = ServerClass(server_address, HandlerClass)

sa = httpd.socket.getsockname()
print "Serving HTTP on", sa[0], "port", sa[1], "..."

webbrowser.open("http://localhost:%s/utils/help_preview.html" % port)

httpd.serve_forever()
