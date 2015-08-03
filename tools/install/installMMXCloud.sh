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


. ./setEnv.sh

#./updateAmi.sh

# install openfire.
echo "*****Openfire installation starts"
echo
scp $SCP_OPTS installOpenfireUbuntu.sh ubuntu@$INSTANCE_NAME:
scp $SCP_OPTS ./setupDB.sql ubuntu@$INSTANCE_NAME:

ssh $SSH_OPTS chmod +x installOpenfireUbuntu.sh
ssh $SSH_OPTS ./installOpenfireUbuntu.sh
echo

echo "The Openfire server is now set up. Please run updateMMXCloud.sh to update it."
echo


