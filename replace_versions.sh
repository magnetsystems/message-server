#!/bin/bash

usage() { printf "Usage: $0 \n\t[-h help] \n\t[-s <string to search>] \n\t[-r <string to replace>]  \n\t[-l <expected line count>]  \n\t[-f <expected file count>] \nExample: \n\t ./replace_versions.sh -s "\"1\\.2\\.3\"" -r "\"1\\.2\\.4\"" -l 4 -f 3\n" 1>&2; exit 1; }


function is_int() { return $(test "$@" -eq "$@" > /dev/null 2>&1); } 

while getopts ":s:r:l:f:" o; do
	case "${o}" in
		l)
			l=${OPTARG}
			if $(is_int $l);
			then
				echo "expected_line_count=$l"
			else
				echo "expected_line_count needs to be a positive integer...exiting"
				exit 0
			fi
			;;
		f)
			f=${OPTARG}
			if $(is_int $f);
			then
				echo "expected_file_count=$f"
			else
				echo "expected_file_count needs to be a positive integer...exiting"
				exit 0
			fi
			;;
		s)
			s=${OPTARG}
			if [ -z "$s" ]; 
			then
				echo "-s <string to search> option is mandatory...exiting"
				exit 0
			else
				echo "string_to_search=$s"
			fi
			;;	
		r)
			r=${OPTARG}
			if [ -z "$r" ];
			then
				echo "-r <string to replace> option is mandatory...exiting"
				exit 0
			else
				echo "string_to_replace=$r"
			fi
			;;
		*)
			usage
			;;
	esac
done
shift $((OPTIND-1))

if [ -z "${s}" ] || [ -z "${s}" ] || [ -z "${l}" ] || [ -x "${f}" ]; then
	usage
fi

if [ $f -gt $l ];
then
	echo "expected_file_count=$f cannot be greater than expected_line_count=$l...exiting"
	exit 0
fi

file_ct=$(git grep -lz $s -- `git ls-files | grep -v pom.xml` | xargs -0 | wc -w | xargs echo)
line_ct=$(git grep -lz $s -- `git ls-files | grep -v pom.xml` | xargs -0 grep $s | wc -l | xargs echo)

if [ $line_ct -ne $l ];
then
	echo "Actual lines that will be affected = $line_ct is not the same as expected line count = $l...exiting"
	exit 0
fi

if [ $file_ct -ne $f ];
then
	echo "Actual files that will be affected = $file_ct is not the same as expected fule count = $f...exiting"
	exit 0
fi

echo "Replacing '$s' with '$r'"

git grep -lz $s -- `git ls-files | grep -v pom.xml` | xargs -0 perl -i'' -pE "s/$s/$r/g"
