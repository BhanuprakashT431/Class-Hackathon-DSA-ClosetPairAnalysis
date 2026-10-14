@echo off
title Closest Pair of Points Analysis
echo ========================================================
echo   Closest Pair of Points: Comparative Analysis & GUI
echo ========================================================
echo.

set JAVA_BIN=C:\Program Files\Java\jdk-25\bin\java.exe

if not exist "%JAVA_BIN%" (
    set JAVA_BIN=java
)

echo Starting application...
"%JAVA_BIN%" ClosestPairAnalysis 200

pause
