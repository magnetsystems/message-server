#!/bin/bash
#
# This script performs the manual steps to complete the
# MMX release build not done by Jenkins.  However, it does not
# cover iOS release build.
#
# Before the final push to master branch, the user will be prompted
# to verify the changes.
#
# Usage: mmx-rel-build old-version new-version release-branch-name
#
# For example: mmx-rel-build 1.6.3 1.0.2 release-1.0.2-RC8
#

mmx-repo() {
  if [ $# -eq 0 ]; then
    echo "Usage: $0 { init | git-command }" >&2
    exit 1
  fi

  REPOS="mmx messaging-console-server messaging-console"

  if [ "$1" = "init" ]; then
    for dir in ${REPOS}; do
      git clone git@bitbucket.org:magneteng/${dir}.git
    done
  else
    for dir in ${REPOS}; do
      ( cd $dir && git $@ )
    done
  fi
}

if [ $# -ne 3 ]; then
  echo "Usage: $0 old-version new-version release-branch" >&2
  exit 1
else
  OLD_VERSION=$1
  NEW_VERSION=$2
  RELEASE_BRANCH=$3
fi

echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
echo "@ Create a local repo: mmx-release-repo..."
echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
rm -rf mmx-release-repo
mkdir mmx-release-repo
cd mmx-release-repo
mmx-repo init

echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
echo "@ Check out ${RELEASE_BRANCH} and merge from master branch..."
echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
mmx-repo checkout -b $RELEASE_BRANCH origin/$RELEASE_BRANCH
mmx-repo merge -m "Merge from remote master to local release branch" -s ours origin/master

echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
echo "@ Check out master and merge from ${RELEASE_BRANCH}..."
echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
mmx-repo checkout -b master -t origin/master
mmx-repo merge -m "Merge from release branch to local master" heads/$RELEASE_BRANCH

#
# roll back the version dependency for the reucon block
#
echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
echo "@ Rollback the version in pom.xml"
echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
sed -e "/reucon/,/version/s/$NEW_VERSION/$OLD_VERSION/" mmx/server/plugins/mmxmgmt/pom.xml > /tmp/pom.xml
if [ -s /tmp/pom.xml ]; then
  mv /tmp/pom.xml mmx/server/plugins/mmxmgmt/pom.xml
fi

echo
echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
echo "@ The merge master<->release may be incorrect."
echo "@ Verify all changes in mmx-release-repo first."
echo "@ Press RETURN for final push or ^C to abort..."
echo "@ run 'mmx-repo.sh push origin HEAD:master' manually"
echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
read VAR
mmx-repo push origin HEAD:master

# Now the "master" branch is ready for the final build.
echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
echo "@ Please start Jenkins final MMX release build..."
echo "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
