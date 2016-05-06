@echo off
setlocal
set RUNDIR=%~p0
set RUNCMD=run
set CLPATH=%RUNDIR%..;%RUNDIR%..\jkool-jesl.jar;%RUNDIR%../lib/*
set MAINCL=com.jkool.jesl.simulator.TNT4JSimulator
set TNT4JOPTS=-Dlog4j.configuration=file:%RUNDIR%..\config\log4j-simulator.properties -Dtnt4j.config=%RUNDIR%..\config\tnt4j-simulator.properties -Dtnt4j.token.repository=%RUNDIR%..\config\tnt4j-tokens.properties
set RUNFILE=%RUNDIR%\..\sims\order-process.xml
java %TNT4JOPTS% -classpath %CLPATH% %MAINCL% %RUNCMD% -A:token -f:%RUNFILE% -T:test.jkoolcloud.com -P:6580 -C:HTTP -u -p:98 -i:1
endlocal
