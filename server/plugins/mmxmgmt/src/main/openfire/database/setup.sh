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
#
# Use the script to set up the Openfire database for MMX.
# The default database is "openfire".
#
# Usage: setup.sh [-p password] [database]
#

echo "***Refreshing the db"
while getopts "p:h:" opt; do
    case $opt in
    h)
        HOST="-h ${OPTARG}"
        echo "host: $HOST"
        ;;
    p)
        PASSWORD="-p${OPTARG}"
        echo "password: $PASSWORD"
        ;;
        \?)
        echo "Invalid option: -$OPTARG" >&2
        exit
        ;;
    esac
done

shift $((OPTIND-1))

DATABASE=$1
if [ "$DATABASE" = "" ]; then
    DATABASE=openfire
fi
echo "database: $DATABASE"

echo "To be executed: mysql $HOST -u root $PASSWORD $DATABASE"

mysql $HOST -u root $PASSWORD $DATABASE <<EOF
    create database if not exists $DATABASE;
    use $DATABASE;
    delete from ofVersion where name='mmxappmgmt' and version=0;
    drop table if exists mmxApp;
    drop table if exists mmxDevice;
    drop table if exists mmxMessage;
    drop table if exists mmxWakeupQueue;
    drop table if exists mmxPushMessage;
    source mmxappmgmt_mysql.sql
    source update.sql
EOF

