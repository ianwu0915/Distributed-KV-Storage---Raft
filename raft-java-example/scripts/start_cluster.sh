#!/bin/bash

JAR_PATH="../target/raft-server-jar-with-dependencies.jar"

# Start Group 1 servers
java -jar $JAR_PATH 1 ../src/main/resources/raft-groups.properties &
java -jar $JAR_PATH 2 ../src/main/resources/raft-groups.properties &
java -jar $JAR_PATH 3 ../src/main/resources/raft-groups.properties &

# Start Group 2 servers
java -jar $JAR_PATH 4 ../src/main/resources/raft-groups.properties &
java -jar $JAR_PATH 5 ../src/main/resources/raft-groups.properties &
java -jar $JAR_PATH 6 ../src/main/resources/raft-groups.properties &

# Start Group 3 servers
java -jar $JAR_PATH 7 ../src/main/resources/raft-groups.properties &
java -jar $JAR_PATH 8 ../src/main/resources/raft-groups.properties &
java -jar $JAR_PATH 9 ../src/main/resources/raft-groups.properties & 