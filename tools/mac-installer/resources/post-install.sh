#!/bin/bash
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
check_port() {
    if lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null ; then
        echo "port $1 unavailable" >> $log
        return 1
    else
        return 0
    fi
}

home="mmx-standalone-dist"
homev="mmx-standalone-dist-1.3.0-SNAPSHOT"
homev101="mmx-standalone-dist-1.0.1"
log="/Users/$USER/${home}_installlog.txt"

echo post_install >> $log
echo $testing >> $log

if [ -d "/Users/$USER/$homev" ]; then
    cd /Users/$USER
    TIME=`date +%F-%H%M%S`
    mv /Users/$USER/$homev /Users/$USER/$homev.$TIME
    echo "previous 1.0.2 install backed up" >> $log
fi

pwd >> $log

if [ -d "/Users/$USER/$homev101" ]; then
    cd /Users/$USER
    TIME=`date +%F-%H%M%S`
    mv /Users/$USER/$homev101 /Users/$USER/$homev101.$TIME
    echo "previous 1.0.1 install backed up" >> $log
fi

mv /Users/Shared/$homev/ /Users/$USER/
chown -R $USER:everyone /Users/$USER/$homev
echo "installed to /Users/$USER" >> $log

check_port 3000;check_nodejs=$?
echo "check_nodejs: $check_nodejs" >> $log

check_port 9090;check_bootstrap_http=$?
echo "check_bootstrap_http: $check_bootstrap_http" >> $log

check_port 9091;check_bootstrap_https=$?
echo "check_bootstrap_https: $check_bootstrap_https" >> $log

check_port 6060;check_admin_http=$?
echo "check_admin_http: $check_admin_http" >> $log

check_port 6061;check_admin_https=$?
echo "check_admin_https: $check_admin_https" >> $log

check_port 5220;check_public_http=$?
echo "check_public_http: $check_public_http" >> $log

check_port 5221;check_public_https=$?
echo "check_public_https: $check_public_https" >> $log

check_port 5222;check_messaging_tcp=$?
echo "check_messaging_tcp: $check_messaging_tcp" >> $log

if [ "$check_nodejs" -eq 1  ] || [ "$check_bootstrap_http" -eq 1 ] || [ "$check_bootstrap_https" -eq 1 ] || [ "$check_admin_http" -eq 1 ] || [ "$check_admin_https" -eq 1 ] || [ "$check_public_http" -eq 1 ] || [ "$check_public_https" -eq 1 ] || [ "$check_messaging_tcp" -eq 1 ]; then
# at least 1 of the required ports is taken
    su - $USER -c "open /Users/$USER/$homev/troubleshooting.html"
    echo "troubleshooting opened as at least one of the ports is taken" >> $log
else
    su - $USER -c "bash -c 'cd /Users/$USER/$homev;./mmx.sh start'"

    if [ $? -neq 0]; then
        echo "failed to start mmx.sh" >> $log
    fi
    echo "wizard openend" >> $log
    sleep 5
    su - $USER -c "open http://localhost:3000"
fi

TIME=`date +%F-%H%M%S`
mv $log "/Users/$USER/${home}_installlog.$TIME.txt"
