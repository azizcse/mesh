#!/bin/sh
#
# Start-up script for our keep rule generator -- companion tool for DexGuard
# Use this tool to generate all keep rules for a specific aar library.

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

java -cp "$PROGUARD_HOME/lib/dexguard-tools.jar" com.guardsquare.tools.AarKeepRuleGenerator "$@"
