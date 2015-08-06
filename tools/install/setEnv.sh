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

if [ "$JENKINS" == "" ]; then # deploying from a dev machine
    echo ***"Set the environment preparing for non-Jenkins deployment"
    echo
    # refresh the openfire database
    # -----------------------------
    export REFRESH_DB=true

    # seed test data into the database
    # --------------------------------
    export SEED_TEST_DATA=true

    # the ec2 instance hosting the openfire server
    # --------------------------------------------
    export INSTANCE_NAME=54.1.3.97.28

    # openfire home
    # -------------
    export OPENFIRE_HOME=/usr/share/openfire

    # openfire user
    # -------------
    # The username of the openfire mysql user.
    export OPENFIRE_MYSQL_USR=openfire_user

    # openfire user password
    # ----------------------
    export OPENFIRE_MYSQL_PWD=openfire

    # openfire user password
    # ----------------------
#    export MYSQL_HOST=localhost

    # password for the mysql root user if it is not null
    # --------------------------------------------------
#    export MYSQL_ROOT_PWD=test

    # the mysql database for openfire
    # -------------------------------
    export DB=openfireutf8
fi

# if undefined, define it.  Otherwise go with what you have.
if [ -z "$IDENTITY" ]; then
    export IDENTITY=$HOME/.ssh/MagnetEngOffice.pem
else
    echo Using AWS key $IDENTITY
fi


