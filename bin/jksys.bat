@echo off
setlocal
set RUNDIR=%~dp0

set CLPATH=%RUNDIR%../*;%RUNDIR%../lib/*
set MAINCL=com.jkoolcloud.jesl.net.syslogd.SyslogSend
java -classpath %CLPATH% %MAINCL% %*
endlocal