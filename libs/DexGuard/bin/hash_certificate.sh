#!/bin/sh
#
# Start-up script for our certificate hash tool -- companion tool for DexGuard
# Use this tool to get a hash for a public key, the resulting code fragment
# can be used to initialize a SSL pinned trust manager.

# Account for possibly missing/basic readlink.
# POSIX conformant (dash/ksh/zsh/bash).
PROGUARD=`readlink -f "$0" 2>/dev/null`
if test "$PROGUARD" = ''
then
  PROGUARD=`readlink "$0" 2>/dev/null`
  if test "$PROGUARD" = ''
  then
    PROGUARD="$0"
  fi
fi

PROGUARD_HOME=`dirname "$PROGUARD"`/..

java -cp "$PROGUARD_HOME/lib/dexguard-tools.jar" com.guardsquare.tools.PublicKeyPinningTool "$@"
