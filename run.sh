#!/bin/bash
cd "$(dirname "$0")"
javac -d out -sourcepath src/main/java src/main/java/dungeonexplorer/Main.java
java -cp out dungeonexplorer.Main
