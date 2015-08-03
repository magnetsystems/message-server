#!/bin/bash
#
# A standalone script similar to Google's "repo" script.
# User should do "mmx-repo init" once to clone three repos.
# Then user should invoke "mmx-repo git-command" which will
# apply a git-command (e.g. merge) to each repo.
#

if [ $# -eq 0 ]; then
  echo "Usage: $0 { init | git-command }" >&2
  exit 1
fi

REPOS="mmx messaging-console-server messaging-console"

if [ "$1" = "init" ]; then
  for dir in ${REPOS}; do
    git clone git@bitbucket.org:${dir}.git
  done
else
  for dir in ${REPOS}; do
    ( cd $dir && git $@ )
  done
fi
