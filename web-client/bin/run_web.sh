#!/bin/bash

cd ../target
RUNJAVA="$JAVA_HOME/bin/java"
JAR=raft.client-0.0.1-SNAPSHOT.jar
$RUNJAVA -jar $JAR
cd -
