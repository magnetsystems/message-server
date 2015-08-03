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

echo "arg: $1"

if [[ "jenkins" == $1 ]]; then
    echo "***A Jenkins execution"
    echo
    . ./setEnvByJenkins.sh
else
    echo "***A local execution"
    echo
    . ./setEnv.sh
fi

echo "***Stopping openfire"
/etc/init.d/openfire stop
echo

# run setup.sh
if [[ "true" == "$REFRESH_DB" ]]; then
    if [[ $MYSQL_HOST != "" ]] && [[ $MYSQL_ROOT_PWD != "" ]]; then
        ./setup.sh -h $MYSQL_HOST -p $MYSQL_ROOT_PWD $DB
    else
        if [[ $MYSQL_HOST != "" ]] && [[ $MYSQL_ROOT_PWD == "" ]]; then
            ./setup.sh -h $MYSQL_HOST $DB
        else
            if [[ $MYSQL_HOST == "" ]] && [[ $MYSQL_ROOT_PWD != "" ]]; then
                ./setup.sh -p $MYSQL_ROOT_PWD $DB
            else
                ./setup.sh $DB
            fi
        fi
    fi
echo
fi

if [[ "$SEED_TEST_DATA" == "true" ]]; then
    echo "***Seed test data"
    if [ "$MYSQL_HOST" == "" ]; then
        mysql -u $OPENFIRE_MYSQL_USR -p$OPENFIRE_MYSQL_PWD $DB < "seed_citest01.sql"
    else
        mysql -h $MYSQL_HOST -u $OPENFIRE_MYSQL_USR -p$OPENFIRE_MYSQL_PWD $DB < "seed_citest01.sql"
    fi
    echo
fi

echo "***Mandatory db schema update"
if [ "$MYSQL_HOST" == "" ]; then
    mysql -u $OPENFIRE_MYSQL_USR -p$OPENFIRE_MYSQL_PWD $DB < "update.sql"
else
    mysql -h $MYSQL_HOST -u $OPENFIRE_MYSQL_USR -p$OPENFIRE_MYSQL_PWD $DB < "update.sql"
fi
echo

#keep
echo "***Swapping openfire's openfire.jar with ours."
sudo cp ./openfire.jar $OPENFIRE_HOME/lib/
sudo chown openfire $OPENFIRE_HOME/lib/openfire.jar
echo

echo "***Remove the old plugin and jar"
sudo rm $OPENFIRE_HOME/plugins/mmxmgmt.jar
sudo rm -r $OPENFIRE_HOME/plugins/mmxmgmt
echo

echo "***Applying the new plugin" # mandatory
sudo cp mmxmgmt.jar $OPENFIRE_HOME/plugins
sudo chown openfire:openfire $OPENFIRE_HOME/plugins/mmxmgmt.jar

rm -r mmxmgmt
mkdir mmxmgmt
cd mmxmgmt
jar -xvf ../mmxmgmt.jar
cd ..
sudo mv mmxmgmt $OPENFIRE_HOME/plugins
sudo chown -R openfire:openfire $OPENFIRE_HOME/plugins/mmxmgmt
echo

echo "***Applying the new MMXLockoutProvider"
sudo cp mmx-server-providers-lockout.jar $OPENFIRE_HOME/lib
sudo chown openfire:openfire $OPENFIRE_HOME/lib/mmx-server-providers-lockout.jar
echo

echo "***Applying the new log4j.xml"
sudo mv log4j.xml /etc/openfire/
sudo chown openfire:openfire /etc/openfire/log4j.xml
echo

echo "***Starting openfire"
sudo /etc/init.d/openfire force-reload
echo

echo "*****Update done"
