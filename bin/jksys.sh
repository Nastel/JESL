#! /bin/bash
RUNDIR=`pwd`

CLPATH="$RUNDIR/../jkool-jesl.jar:$RUNDIR/../lib/*"
MAINCL=com.jkoolcloud.jesl.net.syslogd.SyslogSend
java -classpath $CLPATH $MAINCL $*
