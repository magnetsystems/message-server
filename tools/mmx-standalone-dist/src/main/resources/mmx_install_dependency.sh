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
baselineUbuntu() {
    if [ -f /var/lib/apt/lists/lock ]; then
        sudo rm /var/lib/apt/lists/lock >>$DEBUG_LOG 2>&1
    fi

    if [ -f /var/cache/apt/archives/lock ]; then
        sudo rm /var/cache/apt/archives/lock >>$DEBUG_LOG 2>&1
    fi

    sudo dpkg --configure -a >>$DEBUG_LOG 2>&1
    # update apt-get
    echo
    echo ' -----------------------------'
    echo ' Performing apt-get update ...'
    sudo apt-get -y update >>$DEBUG_LOG 2>&1
    echo
    echo ' apt-get completed'
    echo ' -----------------------------'
    echo

    #install lsof
    sudo apt-get -y install -qq lsof >>$DEBUG_LOG 2>&1

    #install curl
    sudo apt-get -y install -qq curl >>$DEBUG_LOG 2>&1

    #install nodejs
    echo
    echo ' ---------------------'
    echo ' Installing nodejs ...'
    sudo apt-get install -qq nodejs >>$DEBUG_LOG 2>&1
    sudo apt-get install -qq npm >>$DEBUG_LOG 2>&1
    sudo npm install -qq -g n >>$DEBUG_LOG 2>&1
    sudo n 0.10.3 >>$DEBUG_LOG 2>&1
    echo
    echo ' nodejs prepared'
    echo ' ---------------------'
    echo
}

installJre() {
    if type -p java >>$DEBUG_LOG 2>&1; then
        _java=java
    elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
        _java="$JAVA_HOME/bin/java"
    else
        echo
        echo ' -----------------------------------------------'
        echo ' Java not detected. Installing openjdk-6-jre ...'
        sudo apt-get -y install -qq openjdk-6-jre >>$DEBUG_LOG 2>&1
        echo
        echo ' openjdk-6-jre Installed'
        echo ' -----------------------------------------------'
        echo
    fi

    if [[ "$_java" ]]; then
        version=$("$_java" -version 2>&1 | grep version  | awk '{print $NF}')
        echo "Java version: $version" >>$DEBUG_LOG 2>&1
        if [[ "$version" < "1.6" ]]; then
            echo
            echo ' -----------------------------------------------------------'
            echo ' Java version is less than 1.6. Installing openjdk-6-jre ...'
            sudo apt-get -y install -qq openjdk-6-jre >>$DEBUG_LOG 2>&1
            echo
            echo ' openjdk-6-jre installed'
            echo ' -----------------------------------------------------------'
            echo
        else
            echo
            echo ' -----------------------------------------------------------'
            echo ' Java version is 1.6 or higher'
            echo
            echo ' openjdk-6-jre checked'
            echo ' -----------------------------------------------------------'
            echo
        fi
    fi
}

install_MySQL () {
    if type -p mysql >>$DEBUG_LOG 2>&1; then
        echo
        echo ' -------------'
        echo ' MySQL present'
        echo ' -------------'
        echo
    else
        echo
        echo ' -----------------------------------------------'
        echo ' Installing MySQL ...'

        if uname -v | grep 'Debian' >>$DEBUG_LOG 2>&1; then
            sudo rm /etc/apt/sources.list.d/mysql.list >>$DEBUG_LOG 2>&1
            sudo touch /etc/apt/sources.list.d/mysql.list >>$DEBUG_LOG 2>&1
            echo "deb http://repo.mysql.com/apt/debian/ wheezy mysql-apt-config" | sudo tee -a /etc/apt/sources.list.d/mysql.list >>$DEBUG_LOG 2>&1
            echo "deb http://repo.mysql.com/apt/debian/ wheezy mysql-5.6" | sudo tee -a /etc/apt/sources.list.d/mysql.list >>$DEBUG_LOG 2>&1
            echo "deb-src http://repo.mysql.com/apt/debian/ wheezy mysql-5.6" | sudo tee -a /etc/apt/sources.list.d/mysql.list >>$DEBUG_LOG 2>&1
            sudo apt-get -y update >>$DEBUG_LOG 2>&1
            sudo debconf-set-selections <<< 'mysql-community-server	mysql-community-server/data-dir note' >>$DEBUG_LOG 2>&1
            sudo debconf-set-selections <<< 'mysql-community-server	mysql-community-server/remove-data-dir  boolean	false' >>$DEBUG_LOG 2>&1
            sudo debconf-set-selections <<< 'mysql-community-server	mysql-community-server/re-root-pass	password test' >>$DEBUG_LOG 2>&1
            sudo debconf-set-selections <<< 'mysql-community-server	mysql-community-server/root-pass	password test' >>$DEBUG_LOG 2>&1
        elif uname -v | grep 'Ubuntu' >>$DEBUG_LOG 2>&1; then
            sudo debconf-set-selections <<< 'mysql-server mysql-server/root_password password test' >>$DEBUG_LOG 2>&1
            sudo debconf-set-selections <<< 'mysql-server mysql-server/root_password_again password test' >>$DEBUG_LOG 2>&1
        else
            echo
            echo "!!! Unsupported Linux flavor; user needs to manually install."
            echo
            return 1
        fi

        sudo apt-get -y --force-yes install mysql-server-5.6 >>$DEBUG_LOG 2>&1
        mysql -u root -ptest -e "flush privileges;set password for 'root'@'localhost'=password('');" >>$DEBUG_LOG 2>&1
        echo ' Your MySQL root password has been set to blank.'
        echo
        echo ' MySQL installed'
        echo ' -----------------------------------------------'
        echo
    fi
}

DIR="mmx-standalone-dist"
PACKAGE="$DIR.zip"
TIME=`date +%F-%H%M%S`
DEBUG_LOG=./mmx_install_dependency.log

case "`uname`" in
    CYGWIN*)
        echo
        echo '!!! Please note that Magnet Message does not support cygwin. Please visit www.magnet.com/developer for more information.'
        echo
        exit 1
        ;;
    Darwin*)
        echo
        echo '!!! Please note that this installation does not support Mac. Please download the Mac installer from www.magnet.com/developer.'
        echo
        exit 1
        ;;
    Linux*)
        if ! which apt-get >>$DEBUG_LOG 2>&1; then
            echo
            echo '!!! This installation supports Ubuntu and Debian Linux only. For other Linux flavors, please visit www.magnet.com/developer.'
            echo
            exit 1
        fi
        ;;
    *)
            echo
            echo '!!! This installation does not support your environment. Please visit www.magnet.com/developer for more information.'
            echo
esac

echo
echo ' This will install the dependency software used by Magnet Message (if any of them is not available). The'
echo ' dependencies are: lsof, JRE 1.6, MySQL, and Node.js. It may take a moment depending on the network speed'
echo ' and the number of dependencies that it needs to install. During the installation, it may prompt you for'
echo ' the root password. For EC2 instances, you may see "sudo: unable to resolve host...", which is ignorable.'
echo
read -p ' If you have any old Magnet Message running, please stop it now. [Press any key to continue]'
echo

if [ -f $DEBUG_LOG ]; then
    rm $DEBUG_LOG
fi

touch $DEBUG_LOG

baselineUbuntu
installJre
install_MySQL

echo ' Finished installing the dependency software.'
echo " To start/stop/restart the server, please use ./mmx.sh"
echo ' To uninstall, please stop the server, remove the Magnet Message home directory, and also remove the database schema.'
echo
