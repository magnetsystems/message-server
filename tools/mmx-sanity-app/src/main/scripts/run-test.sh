#!/bin/bash
#
# Run the Java interactive test app

if [ $# -ne 1 ]; then
  echo "Usage: $0 props"
  exit 1
fi

for dir in . ../../../target; do
  LIBS=${dir}/mmx-sanity-app-1.6.1-shaded.jar
  if [ -f ${LIBS} ]; then
    break;
  fi
done

#DEBUG=-agentlib:jdwp=transport=dt_socket,address=127.0.0.1:8888,server=y,suspend=n
java $DEBUG -classpath ${LIBS} com.magnet.mmx.client.app.TestApp $1
