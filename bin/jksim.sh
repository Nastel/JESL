#! /bin/bash
ORIGDIR=`pwd`
cd `dirname $0`
RUNDIR=`pwd`
cd $ORIGDIR


CLPATH="$RUNDIR/..:$RUNDIR/../jkool-jesl.jar:$RUNDIR/../lib/*:$RUNDIR/../lib/slf4j-simple/*"
MAINCL=com.jkool.jesl.simulator.TNT4JSimulator
TNT4JOPTS="-Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dtnt4j.config=$RUNDIR/../config/tnt4j-simulator.properties"

java $TNT4JOPTS -classpath $CLPATH $MAINCL run -T:data.jkoolcloud.com -P:6585 -C:HTTPS -p:10 -uc -ui $@
