#!/bin/sh
#   Copyright (c) 2015 Magnet Systems, Inc.
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
log="/Users/$USER/mmx-standalone-dist_installlog.txt"
java_url=http://www.oracle.com/technetwork/java/javase/downloads/index.html

echo check_java >> $log

jdk_home=$(/usr/libexec/java_home)
if [ $? -neq 0 ]; then
    echo "No java_home (bin) found" >> $log
    open $java_url
    exit 1
fi

if [[ $jdk_home != /Library* ]] || [[ $jdk_home != *"jdk"* ]]; then
    echo "No jdk home found" >> $log
    open $java_url
    exit 1
else
    echo "jdk home found in $jdk_home" >> $log
fi

echo "checking java version" >> $log

if type -p java >/dev/null; then
    _java=java
    echo "java found" >> $log
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    _java="$JAVA_HOME/bin/java"
    echo "JAVA_HOME found" >> $log
else
    echo "No java detected" >> $log
    open $java_url
    exit 1
fi

if [[ "$_java" ]]; then
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    if [[ "$version" < "1.6" ]]; then
        echo "java version lower than 1.6" >> $log
        open $java_url
        exit 1
    fi
fi


