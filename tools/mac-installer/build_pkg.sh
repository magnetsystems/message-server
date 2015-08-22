#!/bin/bash
#
# Optionally provide authorization username, password, zipfile
#
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

if [ -v $DEBUG ]; then
    set -x
fi

export MMX_UNZIP_DIR='mmx-standalone-dist-1.5.3'
export MMX_ZIPFILE="${MMX_UNZIP_DIR}.zip"

if [ "$(ls -A payload)" ]; then
    # remove old stuff
    rm -r payload/*
fi

# If the zipfile doesn't exist
if [ ! -e "$MMX_ZIPFILE" ]; then
    echo "$MMX_ZIPFILE missing in the $PWD. Please place/copy it there!"
    exit 1
fi

pushd payload
pwd

unzip ../$MMX_ZIPFILE

echo $MMX_UNZIP_DIR

sed 's/node/..\/node-v0.10.36-darwin-x64\/bin\/node/g' $MMX_UNZIP_DIR/console/mmx-console.sh > $MMX_UNZIP_DIR/console/mmx-console.tmp

mv -f $MMX_UNZIP_DIR/console/mmx-console.tmp $MMX_UNZIP_DIR/console/mmx-console.sh
chmod 775 $MMX_UNZIP_DIR/console/mmx-console.sh

if [ -e $MMX_UNZIP_DIR/mmx_install_dependency.sh ]; then
rm $MMX_UNZIP_DIR/mmx_install_dependency.sh
fi

if [ -e $MMX_UNZIP_DIR/console/mmx-console.bat ]; then
rm $MMX_UNZIP_DIR/console/mmx-console.bat
fi

if [ -e $MMX_UNZIP_DIR/messaging/bin/mmx-server.bat ]; then
rm $MMX_UNZIP_DIR/messaging/bin/mmx-server.bat
fi

pwd
pushd $MMX_UNZIP_DIR
pwd

tar -zxf ../../resources/node-v0.10.36-darwin-x64.tar.gz

popd
popd
pwd

MMS_DIR="MMS"
PKG_PROJNAME="$MMS_DIR/MMS.pkgproj"

#usage: packagesbuild [-v] [-d] [-F <reference folder>] [-t <temporary build location>]  file ...
packagesbuild -F $MMS_DIR $PKG_PROJNAME
