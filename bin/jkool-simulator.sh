#! /bin/bash
origdir=`pwd`
cd `dirname $0`
jardir=`pwd`
cd ${origdir}
java -jar ${jardir}/jkool-jesl.jar $*