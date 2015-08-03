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


#  Please see readme.txt

if [ "$JENKINS" == "" ]; then
    echo "Are you sure ../../server/plugins/mmxmgmt/target/mmxmgmt*.jar is the one that you want to deploy. [Enter to continue]"
    read
fi

echo "*****Update starts"
echo
. ./setEnv.sh

if [ "$JENKINS" == "true" ]; then # deploying from a dev machine
    echo "***Prepare the script to set the environment on the ec2 instance"
    echo
    if [ -f setEnvByJenkins.sh ]; then
	rm setEnvByJenkins.sh
    fi
    touch setEnvByJenkins.sh
    chmod a+x setEnvByJenkins.sh
    cat << EOF > setEnvByJenkins.sh
#!/bin/sh
export REFRESH_DB=$REFRESH_DB
export SEED_TEST_DATA=$SEED_TEST_DATA
export INSTANCE_NAME=$INSTANCE_NAME
export OPENFIRE_HOME=$OPENFIRE_HOME
export OPENFIRE_MYSQL_USR=$OPENFIRE_MYSQL_USR
export OPENFIRE_MYSQL_PWD=$OPENFIRE_MYSQL_PWD
export MYSQL_HOST=$MYSQL_HOST
export MYSQL_ROOT_PWD=$MYSQL_ROOT_PWD
export DB=$DB
EOF

fi

export SCP_OPTS="-oStrictHostKeyChecking=no -i $IDENTITY"
export SSH_OPTS="-oStrictHostKeyChecking=no -i $IDENTITY ubuntu@$INSTANCE_NAME"

# copy files over
. ./update-CopyFiles.sh

# run the update script on the ec2 instance
if [ "$JENKINS" == "true" ]; then # deploying from a dev machine
    ssh $SSH_OPTS './updateOpenfireUbuntu.sh jenkins'
else
    ssh $SSH_OPTS ./updateOpenfireUbuntu.sh
fi
