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


source startup.properties
check_port=true
find ./messaging/plugins/ -exec touch {} \;
check_port=true

print_usage() {
	echo "Usage: mmx.sh [-p] {start|stop|restart}" >&2
	echo >&2
	echo "Start, stop, or restart the Magnet Messaging server and console." >&2
	echo >&2
	echo "Options:" >&2
	echo "    [-p]    No port check when starting." >&2
	echo >&2
}

if [ "$#" == 0 ] || [ "$#" -gt 2 ] ; then
	print_usage
	exit 1
fi

pwd=$PWD
check_port=true;

check_port() {
	if nc -z 127.0.0.1 $1 > /dev/null; then
		echo "ERROR: TCP port $1 is already in use, cannot start messaging server"
		exit 1
	fi
}

check_port_list() {
	portList=( "$@" );
	for i in "${portList[@]}"; do
		check_port $i
	done
}

set_startup_property() {
	#	if [[ $3 ]]; then
	echo "$2=$3" >> $1
	#	fi
}

set_console_startup_properties() {
	rm console/startup.properties
	touch console/startup.properties
	set_startup_property console/startup.properties consolePort $consolePort
	set_startup_property console/startup.properties httpPort $httpPort
}

set_messaging_startup_properties() {
	rm messaging/conf/startup.properties
	touch messaging/conf/startup.properties
	set_startup_property messaging/conf/startup.properties dbHost $dbHost
	set_startup_property messaging/conf/startup.properties dbPort $dbPort
	set_startup_property messaging/conf/startup.properties dbName $dbName 
	set_startup_property messaging/conf/startup.properties dbUser $dbUser
	set_startup_property messaging/conf/startup.properties dbPassword $dbPassword 
	set_startup_property messaging/conf/startup.properties xmppDomain $xmppDomain
	set_startup_property messaging/conf/startup.properties xmppPort $xmppPort
	set_startup_property messaging/conf/startup.properties xmppPortSecure $xmppPortSecure
	set_startup_property messaging/conf/startup.properties httpPort $httpPort
	set_startup_property messaging/conf/startup.properties httpPortSecure $httpPortSecure
	set_startup_property messaging/conf/startup.properties mmxAdminPort $mmxAdminPort
	set_startup_property messaging/conf/startup.properties mmxAdminPortSecure $mmxAdminPortSecure
	set_startup_property messaging/conf/startup.properties mmxPublicPort $mmxPublicPort
	set_startup_property messaging/conf/startup.properties mmxPublicPortSecure $mmxPublicPortSecure
	set_startup_property messaging/conf/startup.properties standaloneServer $standaloneServer
}

start() {
	if [ $check_port == true ]; then
		echo "Checking port availability"
		check_port_list $xmppPort $xmppPortSecure $httpPort $httpPortSecure $mmxAdminPort $mmxAdminPortSecure $mmxPublicPort $mmxPublicPortSecure $consolePort
	fi

	echo "Starting Message web interface ..."
	set_console_startup_properties
	cd console
	if [ $check_port == true ]; then
		./mmx-console.sh start
	else
		./mmx-console.sh -p start
	fi

	if [ $? -ne 0 ] ; then
		exit 1
	fi
	cd $pwd

	echo "Starting Magnet Message server ..."
	set_messaging_startup_properties
	cd messaging/bin
	if [ $check_port == true ]; then
		./mmx-server.sh start
	else
		./mmx-server.sh -p start
	fi

	if [ $? -ne 0 ] ; then
		cd $pwd/console
		./mmx-console.sh stop
		cd $pwd
		exit 1
	fi
	cd $pwd
	echo "Open the following url in your browser: http://<127.0.0.1_or_remote_ip>:$consolePort"
}

stop() {
	echo "Stopping console ...";
	cd console && ./mmx-console.sh stop
	cd $pwd 
	echo "Stopping mmx server";
	cd messaging/bin && ./mmx-server.sh stop
	cd $pwd	
}

while getopts "p h" opt; do
	case $opt in
		p)
			check_port=false
			;;
		h)
			print_usage
			exit 1
			;;
		\?)
			print_usage
			exit 1
			;;
	esac
done

shift $((OPTIND-1))

case "$1" in
	start)
		start
		exit 0
		;;
	stop)
		stop
		exit 0
		;;
	restart)
		stop
		sleep 3
		start
		exit 0
		;;
	**)
		print_usage
		exit 1
		;;
esac

