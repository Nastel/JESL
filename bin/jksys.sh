#! /bin/bash
RUNDIR=`pwd`

CLPATH="$RUNDIR/../*:$RUNDIR/../lib/*"
MAINCL=com.jkoolcloud.jesl.net.syslogd.SyslogSend
java -classpath $CLPATH $MAINCL $*
