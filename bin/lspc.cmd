@echo off

rem Windows and OS/2 batch script for running lspc

rem SET LAGOON_HOME=D:\lagoon

java -classpath %CLASSPATH%;%LAGOON_HOME%\lsp.jar;%LAGOON_HOME%\xmlutil.jar nu.staldal.lsp.LSPCompilerCLI %1 %2 %3 %4 %5 %6 %7 %8 %9