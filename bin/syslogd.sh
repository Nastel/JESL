#! /bin/bash
RUNDIR=`pwd`

CLPATH=$RUNDIR../jkool-jesl.jar:$RUNDIR../lib/*
MAINCL=com.jkool.jesl.net.syslogd.Syslogd
TNT4JOPTS=-Dtnt4j.dump.on.vm.shutdown=true -Dtnt4j.dump.provider.default=true -Dtnt4j.config=$RUNDIR../config/tnt4j.properties
java $TNT4JOPTS -classpath $CLPATH $MAINCL -h 0.0.0.0 tcp
