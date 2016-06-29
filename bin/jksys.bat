@echo off
setlocal
set RUNDIR=%~p0

set CLPATH=%RUNDIR%../jkool-jesl.jar;%RUNDIR%../lib/*
set MAINCL=com.jkoolcloud.jesl.net.syslogd.SyslogSend
java -classpath %CLPATH% %MAINCL% %*
endlocal