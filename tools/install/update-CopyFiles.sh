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


echo "***Copy files over to the remote server"

scp $SCP_OPTS ./openfire.jar ubuntu@$INSTANCE_NAME:openfire.jar

scp $SCP_OPTS ../../server/plugins/mmxmgmt/src/main/openfire/database/setup.sh ubuntu@$INSTANCE_NAME:
scp $SCP_OPTS ../../server/plugins/mmxmgmt/src/main/openfire/database/mmxappmgmt_mysql.sql ubuntu@$INSTANCE_NAME:
scp $SCP_OPTS ../../server/plugins/mmxmgmt/target/mmxmgmt*.jar ubuntu@$INSTANCE_NAME:mmxmgmt.jar
scp $SCP_OPTS ../../server/providers/mmxlockout/target/mmx-server-providers-lockout*.jar ubuntu@$INSTANCE_NAME:mmx-server-providers-lockout.jar

if [ "$JENKINS" == "" ]; then # deployed from a developer machine
    scp $SCP_OPTS setEnv.sh ubuntu@$INSTANCE_NAME:
    ssh $SSH_OPTS chmod +x setEnv.sh
else
    scp $SCP_OPTS setEnvByJenkins.sh ubuntu@$INSTANCE_NAME:
    ssh $SSH_OPTS chmod +x setEnvByJenkins.sh
fi

scp $SCP_OPTS updateOpenfireUbuntu.sh ubuntu@$INSTANCE_NAME:
ssh $SSH_OPTS chmod +x updateOpenfireUbuntu.sh

scp $SCP_OPTS log4j.xml ubuntu@$INSTANCE_NAME:

if [ "$SEED_TEST_DATA" == "true" ]; then
    scp $SCP_OPTS ../../server/plugins/mmxmgmt/src/main/openfire/database/seed_citest01.sql ubuntu@$INSTANCE_NAME:
fi

scp $SCP_OPTS ../../server/plugins/mmxmgmt/src/main/openfire/database/update.sql ubuntu@$INSTANCE_NAME:

echo
