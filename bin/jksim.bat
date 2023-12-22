@echo off
setlocal
.\jks.bat run -T:stream.meshiq.com -C:HTTPS -p:10 -ui -uc %*
endlocal