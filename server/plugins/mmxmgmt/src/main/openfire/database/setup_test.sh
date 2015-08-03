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
# The default database is "openfire_db_test".
#
# Usage: setup.sh [-p password] [database]
#

ARGS=`getopt p: -- "$@"`
eval set -- "$ARGS"
while true; do
  case "$1" in
  -p) PASSWORD="--password=$2"; shift 2;;
  --) shift; break;;
  esac
done
DATABASE=$1
if [ "$DATABASE" = "" ]; then
  DATABASE=openfire_db_test
fi

PWD=`pwd`

mysql -u root $PASSWORD <<EOF
drop database if exists $DATABASE;
create database $DATABASE;
#create user 'mmxunittest'@'localhost' identified by 'test';
grant all on $DATABASE.* to 'mmxunittest'@'localhost' identified by 'test';
use $DATABASE;
source $PWD/openfire_mysql.sql
source $PWD/mmxappmgmt_mysql.sql
source $PWD/update.sql
#drop user 'mmxunittest'@'localhost';
#drop database $DATABASE;
EOF
