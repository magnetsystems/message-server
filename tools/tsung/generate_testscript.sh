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
# This script is used to generate sql statements and tsung client scripts
#

if [ "$1" == "help" -o $# -le 4 ]; then
    echo "Use this script to generate sql statements for creating users in OpenFire and Tsung client scripts"
    echo "Files will be generated to the 'output' directory"
    echo
    echo "Usage:  generate_testscript.sh <num-of-users> <user-name> <app-id> <api-key> <hostname> <xmpp-server-name>"
    echo "Example: sh ./generate_testscript.sh 20 helen i1xq3w6q5kw 72261c25-ab20-4eb8-8648-aaa62c429ad8 localhost helen.local"
    exit 1
fi


CURR_DIR=`pwd`
OUTPUT_DIR="$CURR_DIR/output"

if [ -d $OUTPUT_DIR ]; then
    rm -rf $OUTPUT_DIR
fi

mkdir -p $OUTPUT_DIR

APPID=$3
APIKEY=$4

# NETWORK HOST NAME or IP address of the XMPP server
HOSTNAME=$5

# Server name of the XMPP server, see OF Console->Server Information
SERVERNAME=$6

if [ $1 -ge 0 ]
then

count=1
password="'test'"
userbase=$2
OUTPUT_SQL_FILE=$OUTPUT_DIR/create_user_$1.sql

while [ $count -le "$1" ]; do
    user="'${userbase}$count%${APPID}'"
    emailaddr="'email'"
    zero="'0'"
    let count=count+1
    echo "DELETE FROM ofUser where username=$user;" >> $OUTPUT_SQL_FILE
    echo "INSERT INTO ofUser ( username , plainPassword, encryptedPassword, name, email, creationDate, modificationDate) VALUES ($user, $password, NULL, $user, $emailaddr, $zero, $zero);" >> $OUTPUT_SQL_FILE
done


else
    echo "Use a number > 0 as argument"
    exit 1
fi

# now generate the load test script

GEN_SCRIPT_JAR="tooljar/mmx-tools-tsung-1.3.13-shaded.jar"
if [ ! -e ${GEN_SCRIPT_JAR} ]; then
    buildCommand="mvn clean install"
    eval "$buildCommand"
fi

FTL_DIR="$CURR_DIR/ftl"

GEN_COMMAND="java -cp $GEN_SCRIPT_JAR com.magnet.mmx.tsung.GenTestScriptCli  -c $1 -d $FTL_DIR -p 5222 -s $SERVERNAME -h $HOSTNAME -u $userbase -a $APPID -k $APIKEY"
eval "$GEN_COMMAND"
echo
echo
echo "### All scripts generated in directory 'output' ###"