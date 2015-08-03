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

# Run this script from your project root
# or from the top level directory
# into which it was unpacked.
# Use the . ./cloud/bin/setEnvironment.sh

if [ -z "$INSTANCE_NAME" ]; then
  echo "export INSTANCE_NAME as the ssh target of the machine to install on"
  exit
fi

if [ -z "$IDENTITY" ]; then
  echo "export IDENTITY as the ssh private key to use when connecting to $INSTANCE_NAME"
  exit;
fi

SCP_OPTS="-oStrictHostKeyChecking=no -i $IDENTITY"
SSH_OPTS="-oStrictHostKeyChecking=no -i $IDENTITY ubuntu@$INSTANCE_NAME"

ssh -i $IDENTITY ubuntu@$INSTANCE_NAME uname -a

# configure java, zip, and other useful utilities.
scp $SCP_OPTS cloud/bin/baselineUbuntu.sh ubuntu@$INSTANCE_NAME:baselineUbuntu.sh
ssh $SSH_OPTS chmod +x baselineUbuntu.sh
ssh $SSH_OPTS ./baselineUbuntu.sh

# for single instance deployments with a local mysql database.
scp $SCP_OPTS cloud/bin/mysqlUbuntu.sh ubuntu@$INSTANCE_NAME:mysqlUbuntu.sh
ssh $SSH_OPTS chmod +x mysqlUbuntu.sh
ssh $SSH_OPTS ./mysqlUbuntu.sh
