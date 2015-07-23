@echo off
setlocal
set RUNDIR=%~p0

set CLPATH=%RUNDIR%../jkool-jesl.jar;%RUNDIR%../lib/*
set MAINCL=org.productivity.java.syslog4j.SyslogMain
java -classpath %CLPATH% %MAINCL% %*
endlocal