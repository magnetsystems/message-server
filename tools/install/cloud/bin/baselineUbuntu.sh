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

# make sure we have the lastest OS upgrades
sudo apt-get update
sudo apt-get upgrade

# update the time zone.
sudo dpkg-reconfigure tzdata

# install zip and unzip
sudo apt-get install zip

# install Java 7 from a PPA
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get -y update
sudo apt-get -y install oracle-java7-installer
sudo apt-get -y install oracle-java7-set-default

# setup the magnet user
sudo adduser --disabled-password --home /usr/local/magnet magnet

# if you need to connect directly as the magnet user...
#sudo -u magnet mkdir /usr/local/magnet/.ssh
#set permission to private
#sudo -u magnet chmod 700 /usr/local/magnet/.ssh
#add users keys
#sudo -u magnet vi /usr/local/magnet/.ssh/authorized_keys
#set permission to private
#sudo -u magnet chmod 600 /usr/local/magnet/.ssh/authorized_keys

# Additional Packages Required for the MMS

# required when hosting multiple back end applications.
# move this to a separate setup for haproxy machine.
#sudo apt-get -y install haproxy

# required for running the android tools.
#sudo apt-get -y install ia32-libs

# TODO: install android sdk when changes are complete
# to the android aapt support modules.

# TODO: add install of plutils for linux when changes
# to the modules are complete.

