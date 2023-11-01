@ECHO OFF

REM Start-up script for our certificate hash tool -- companion tool for DexGuard
REM Use this tool to get a hash for a public key, the resulting code fragment
REM can be used to initialize a SSL pinned trust manager.

IF EXIST "%PROGUARD_HOME%" GOTO home
SET PROGUARD_HOME=%~dp0\..
:home

java -cp "%PROGUARD_HOME%\lib\dexguard-tools.jar" com.guardsquare.tools.PublicKeyPinningTool %*
