#! /bin/bash
ORIGDIR=`pwd`
cd `dirname $0`
RUNDIR=`pwd`
cd $ORIGDIR


CLPATH="$RUNDIR/..:$RUNDIR/../*:$RUNDIR/../lib/*"
MAINCL=com.jkoolcloud.jesl.simulator.TNT4JSimulator
TNT4JOPTS="-Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dtnt4j.config=$RUNDIR/../config/tnt4j-simulator.properties"

java $TNT4JOPTS -classpath $CLPATH $MAINCL $@
