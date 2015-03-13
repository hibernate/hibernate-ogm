#!/usr/bin/env bash

RELEASE_VERSION=$1
VERSION_FAMILY=$2

unzip distribution/target/hibernate-ogm-$RELEASE_VERSION-dist.zip -d distribution/target/unpacked
rsync -rzh --progress --delete --protocol=28 distribution/target/unpacked/dist/docs/ filemgmt.jboss.org:/docs_htdocs/hibernate/ogm/$VERSION_FAMILY


