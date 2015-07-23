#! /bin/bash
RUNDIR=`pwd`

CLPATH="$RUNDIR/../jkool-jesl.jar:$RUNDIR/../lib/*"
MAINCL=org.productivity.java.syslog4j.SyslogMain
java -classpath $CLPATH $MAINCL $*
