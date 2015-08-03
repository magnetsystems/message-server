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


# preset the password to test
install_mysql () {
    sudo debconf-set-selections <<< 'mysql-server mysql-server/root_password password test'
    sudo debconf-set-selections <<< 'mysql-server mysql-server/root_password_again password test'
    sudo apt-get -y install mysql-server
}

type mysql >/dev/null 2>&1 && echo 'MySQL present.' || install_mysql