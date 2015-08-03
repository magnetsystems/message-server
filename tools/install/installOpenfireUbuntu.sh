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


# set up the openfire database
echo '***setupDB.sql'
echo
read
mysql -u root < setupDB.sql

#check with a new instance if /logs exists
#mkdir /logs
#chown openfire /logs

# download and install openfire 3.9.3
echo '***download and install openfire. Press [ENTER] to continue'
echo
read
wget -O openfire_3.9.3_all.deb http://www.igniterealtime.org/downloadServlet?filename=openfire/openfire_3.9.3_all.deb
sudo dpkg --install openfire_3.9.3_all.deb
echo
