#!/usr/bin/env bash

PUSH_CHANGES=$1
RELEASE_VERSION=$2

git commit -a -m "[Jenkins release job] Preparing next development iteration"

if [ "$PUSH_CHANGES" = true ] ; then
    echo "Pushing changes to the upstream repository."
    git push origin master
    git push origin $RELEASE_VERSION
fi
if [ "$PUSH_CHANGES" != true ] ; then
    echo "WARNING: Not pushing changes to the upstream repository."
fi
