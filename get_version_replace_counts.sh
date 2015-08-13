#!/bin/bash

usage() {
	printf "Usage: \n\t$0 -s <string to search>\nExample:\n\t $0 -s \"1\\.2\\.3\"\n" 1>&2; exit 1; 
}

while getopts ":s:" o; do
	case "${o}" in
		s)
			s=${OPTARG}
			if [ -z "$s" ];
			then
				echo "-s <string to search> option is mandatory ...exiting"
				exit 0
			fi
			;;
		*)
			usage
			;;	
	esac
done
shift $((OPTIND-1))

if [ -z "${s}" ];
then
	usage
fi

line_ct=$(git grep -lz $s -- `git ls-files | grep -v pom.xml` | xargs -0 grep $s | wc -l | xargs echo)
file_ct=$(git grep -lz $s -- `git ls-files | grep -v pom.xml` | xargs -0 | wc -w | xargs echo)

echo "filecount=$file_ct"
echo "linecount=$line_ct"

