#!/bin/bash -x

BASE=`git rev-parse --show-toplevel`
CP=""

pushd $BASE/imap2signal-gateway-*

  APP=`ls imap2signal-gateway-*.jar`
  for i in lib/*.jar; do
    CP="$CP:$i"
  done

  java -cp "${APP}${CP}" com.github.aanno.imap2signal.MailFetch $*

popd
