#!/bin/bash -x

BASE=`git rev-parse --show-toplevel`

pushd $BASE/imap2signal-gateway-*/

  # TODO aanno: problematic - but why?
  # java -p ".:lib" -jar imap2signal-gateway-*.jar

  # Working with explizit module name
  java -p .:lib -m imap2signal/com.github.aanno.imap2signal.MailFetch $*

popd
