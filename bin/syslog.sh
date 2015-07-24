#! /bin/bash
RUNDIR=`pwd`

CLPATH="$RUNDIR/../jkool-jesl.jar:$RUNDIR/../lib/*"
MAINCL=com.jkool.jesl.net.syslogd.SyslogSend
java -classpath $CLPATH $MAINCL $*
