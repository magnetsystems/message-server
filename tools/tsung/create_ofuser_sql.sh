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
# This script is used to generate sql statement to insert users into openfire ofUser table
#

if [ "$1" == "help" -o $# -eq 0 ]; then
    echo "Use this script to generate sql statements for inserting users into openfire ofUser table"
    echo "username: loadtestNN, where NN is user number"
    echo "Usage:  create_ofuser_sql.sh <num-of-users>"
    echo "Example: sh ./create_ofuser_sql.sh 20"
    exit 1
fi

if [ $1 -ge 0 ]
then

count=1
ofUser="loadtest"

while [ $count -le "$1" ]; do
    user="'loadtest$count'"
    emailaddr="'email'"
    zero="'0'"
    let count=count+1
    echo "INSERT INTO ofUser ( username , plainPassword, encryptedPassword, name, email, creationDate, modificationDate) VALUES ($user, $user, NULL, $user, $emailaddr, $zero, $zero);"
done
exit 0

else
    echo "Use a number > 0 as argument"
    exit 1
fi
