#! /bin/bash
origdir=`pwd`
cd `dirname $0`
jardir=`pwd`
cd ${origdir}
options=-cp ${jardir}/../lib -Dlog4j.configuration=file:${jardir}/../config/log4j-simulator.properties -Dtnt4j.config=${jardir}/../config/tnt4j-simulator.properties -Dtnt4j.token.repository=${jardir}/../tnt4j-tokens.properties
java ${options} -jar ${jardir}/../jkool-jesl.jar $*