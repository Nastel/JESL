#! /bin/bash
if command -v readlink >/dev/null 2>&1; then
    RUNDIR=$(dirname $(readlink -m $BASH_SOURCE))
else
    RUNDIR=$(cd "$(dirname "$BASH_SOURCE")" ; pwd -P)
fi

TNT4JOPTS="-Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dtnt4j.config=$RUNDIR/../config/tnt4j-simulator.properties"
CLPATH="$RUNDIR/..:$RUNDIR/../*:$RUNDIR/../lib/*"
MAINCL=com.jkoolcloud.jesl.simulator.TNT4JSimulator

java $TNT4JOPTS -classpath "$CLPATH" $MAINCL $@
