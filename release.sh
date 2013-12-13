#!/bin/sh

RELEASE=hdp_`date +"%Y%m%d"`
echo "Creating release in $RELEASE"
if [ -e "$RELEASE" ]; then
    echo "Release '$RELEASE' already exists, exiting."; exit;
fi
mkdir "$RELEASE"

# package with appassembler
mvn clean package jar:jar appassembler:assemble
rc=$?
if [[ $rc != 0 ]] ; then
  echo "could not package release"; exit $rc
fi

mv target/appassembler/* "$RELEASE"/.
echo "Done creating release in $RELEASE"
