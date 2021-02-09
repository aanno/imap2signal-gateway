#!/bin/bash -x

BASE=`git rev-parse --show-toplevel`
TMPDIR=`mktemp -d`

pushd "$BASE"

  rm -rf imap2signal-gateway-*/
  unzip build/distributions/imap2signal-gateway-*.zip

  pushd "$TMPDIR"
    unzip $BASE/imap2signal-gateway-*/lib/jffi-*-native.jar
    zip -ur9 $BASE/imap2signal-gateway-*/lib/jffi-*[0-9].jar .
    rm $BASE/imap2signal-gateway-*/lib/jffi-*-native.jar
  popd

popd

rm -rf "$TMPDIR"
