#!/usr/bin/env bash

DIST_PARENT_DIR=$1
RELEASE_VERSION=$2

(echo mkdir $DIST_PARENT_DIR/$RELEASE_VERSION; echo quit) | sftp -b - frs.sourceforge.net
scp readme.md frs.sourceforge.net:$DIST_PARENT_DIR/$RELEASE_VERSION
scp changelog.txt frs.sourceforge.net:$DIST_PARENT_DIR/$RELEASE_VERSION
scp distribution/target/hibernate-ogm-$RELEASE_VERSION-dist.zip frs.sourceforge.net:$DIST_PARENT_DIR/$RELEASE_VERSION
scp distribution/target/hibernate-ogm-$RELEASE_VERSION-dist.tar.gz frs.sourceforge.net:$DIST_PARENT_DIR/$RELEASE_VERSION
scp modules/wildfly/target/hibernate-ogm-modules-wildfly8-$RELEASE_VERSION.zip frs.sourceforge.net:$DIST_PARENT_DIR/$RELEASE_VERSION
scp modules/eap/target/hibernate-ogm-modules-eap6-$RELEASE_VERSION-experimental.zip frs.sourceforge.net:$DIST_PARENT_DIR/$RELEASE_VERSION
