@echo off
setlocal
.\jks.bat run -T:data.jkoolcloud.com -C:HTTPS -p:10 -ui -uc %*
endlocal