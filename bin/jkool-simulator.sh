#! /bin/bash
origdir=`pwd`
cd `dirname $0`
jardir=`pwd`
cd ${origdir}
java -Dtnt4j.config=../tnt4j-simulator.properties -jar ${jardir}/jkool-jesl.jar $*