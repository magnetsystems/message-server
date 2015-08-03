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

usage() { echo "Usage: $0 [-p <db password>] [-d <db name>]" 1>&2; exit 1; }

PASSWORD=
DATABASE=
MYSQL_PASSWORD_STR=

while getopts ":p:d:" o; do
	case "${o}" in
		p)
			PASSWORD=${OPTARG}
			;;
		d)
			DATABASE=${OPTARG}
			;;
		*)
			usage
			;;
	esac
done
shift $((OPTIND-1))

if [ "$DATABASE" = "" ]; then
	DATABASE=openfire_db_test
fi

if [ "$PASSWORD" ]; then
	MYSQL_PASSWORD_STR="-p$PASSWORD"			
fi

echo "PASSWORD = ${PASSWORD}"
echo "DATABASE = ${DATABASE}"
echo "MYSQL_PASSWORD_STR = ${MYSQL_PASSWORD_STR}"

PWD=`pwd`

mysql -u root $MYSQL_PASSWORD_STR <<EOF
drop database if exists $DATABASE;
create database $DATABASE;
#create user 'mmxunittest'@'localhost' identified by 'test';
grant all on $DATABASE.* to 'mmxunittest'@'localhost' identified by 'test';
use $DATABASE;
source $PWD/openfire_mysql.sql
source $PWD/mmxappmgmt_mysql.sql
#drop user 'mmxunittest'@'localhost';
#drop database $DATABASE;
EOF
