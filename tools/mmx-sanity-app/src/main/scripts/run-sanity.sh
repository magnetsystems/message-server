#!/bin/bash
#
# Perform a sanity test against a MMX server at "host".
# 
# The sanity test will register an application "appname" in the server
# if it does not exist.  Once it is registered, the app settings are
# saved in the "appname.props" file.  Sebsequent sanity tests will use
# "appname.props" for the default settings.  If the application already
# exists but the "appname.props" does not exist, it is an error.  There
# are no easy ways to recover the "appname.props" if the file is lost;
# the file has to be manually recreated.
#
# Usage: SanityApp [-a adminUserId] [-p adminPassword] [-u userId] [-h host]
#                  [-d domain] [-l loglevel] appname
# where   -a adminUserId   (default: admin)
#         -p adminPassword (default: admin)
#         -u userId        (default: john.doe@magnet.com)
#         -h host          (default: localhost)
#         -d domain        (default: mmx)
#         -l s|v|d|i|w|e   (default: d for debug)
#         appname
#
# Upon successful, the exit code is 0.  Non-zero exit code is for failure.
#

for dir in . ../../../target; do
  LIBS=${dir}/mmx-sanity-app-1.7.3-shaded.jar
  if [ -f ${LIBS} ]; then
    break;
  fi
done

#DEBUG=-agentlib:jdwp=transport=dt_socket,address=127.0.0.1:8888,server=y,suspend=n
java $DEBUG -classpath ${LIBS} com.magnet.mmx.client.app.SanityApp $@
