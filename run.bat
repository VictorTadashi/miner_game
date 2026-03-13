@echo off
cd /d "%~dp0"
javac -d out -sourcepath src/main/java src/main/java/dungeonexplorer/Main.java
xcopy /Y /I /E src\main\resources\tiles out\tiles >nul 2>&1
java -cp out dungeonexplorer.Main
pause
