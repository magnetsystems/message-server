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
# Usage: deploy_mmx-dist_Ubuntu.sh SSH_KEY EC2_IP JENKINS_USERNAME JENKINS_PASSWORD
#

echo 'This will install mysql, nodejs, jre 6, MMX Server, MMX Console on a brand new ubuntu 14.04 instance.'

if [ "$#" -ne 4 ]; then
    echo "Usage: deploy_mmx-dist_Ubuntu.sh ~/.ssh/MagnetEngOffice.pem 54.153.39.255 JENKINS_USERNAME JENKINS_PASSWORD" >&2
    exit 1
fi

if ! [ -e "$1" ]; then
    echo "$1 not found" >&2
    exit 1
fi


SCP_OPTS="-oStrictHostKeyChecking=no -i $1"
SSH_OPTS="-oStrictHostKeyChecking=no -i $1 ubuntu@$2"
package="mmx-standalone-dist.zip"


# prepare the package
if [ -e $package ]; then
    echo "Backing up $package in local dir" >&2
    mv $package "$package".old
fi

curl -u $3:$4 -o $package "http://build.magnet.com:8082/view/MMX%20Builds/job/mmx-develop-all-maven/lastSuccessfulBuild/artifact/tools/mmx-standalone-dist/target/$package"


# scp the needed files over
scp $SCP_OPTS bin/baselineUbuntu.sh ubuntu@$2:
scp $SCP_OPTS bin/install_Java_Ubuntu.sh ubuntu@$2:
scp $SCP_OPTS bin/setup_MySQL_Ubuntu.sh ubuntu@$2:
scp $SCP_OPTS ./mmx-standalone-dist.zip ubuntu@$2:
scp $SCP_OPTS ./setup_MMX_DB.sql ubuntu@$2:


# prepare the OS, install the prerequisite
ssh $SSH_OPTS ./baselineUbuntu.sh


# prepare the OS, install the prerequisite
ssh $SSH_OPTS ./install_Java_Ubuntu.sh


# install mysql
ssh $SSH_OPTS ./setup_MySQL_Ubuntu.sh


# create the db instance
ssh $SSH_OPTS "mysql -u root -ptest < ./setup_MMX_DB.sql"


# install and start mmx
ssh $SSH_OPTS "unzip ./mmx-standalone-dist.zip"
ssh $SSH_OPTS "cd mmx-standalone-dist && ./mmx.sh start&"


# cleanup
ssh $SSH_OPTS "rm ./baselineUbuntu.sh ./setup_MySQL_Ubuntu.sh ./mmx-standalone-dist.zip ./setup_MMX_DB.sql install_Java_Ubuntu.sh"


sleep 3


echo
echo "Set up is done."
echo "Please continue with the setup by configuring the Magnet Messaging Admini Console"
echo "If it is not automatically opened, please use a browser to go to http://$2:3000/wizard"
echo "Please take a note of the followings info:"
echo "- MySQL root password is set to test"
echo "- The host name of the MySQL host is localhost (which is the default set in Console)"
echo "- The MMX database username/password: mmx/mmx"
echo "- The MMX schema name: magnetmessaging (which is the default set in Console)"

open http://$2:3000/wizard
