@ECHO OFF

REM Start-up script for our keep rule generator -- companion tool for DexGuard
REM Use this tool to generate all keep rules for a specific aar library.

IF EXIST "%PROGUARD_HOME%" GOTO home
SET PROGUARD_HOME=%~dp0\..
:home

java -cp "%PROGUARD_HOME%\lib\dexguard-tools.jar" com.guardsquare.tools.AarKeepRuleGenerator %*
