#!/bin/bash
SCRIPT_DIR=$(dirname $0)
LIB_DIR=$SCRIPT_DIR/../lib
CONF_DIR=$SCRIPT_DIR/../conf

# System detection
UNAME_STR=$(uname -a)
CURRENT_SYS=${UNAME_STR:0:5}
if [ "$CURRENT_SYS" == "MINGW" ]; then
    echo "--current system is mingw--"
elif [ "$CURRENT_SYS" == "CYGWI" ]; then
    echo "--current system is cygwin--"
fi

# Check arguments
if [ $# -eq 2 ]; then
    # New format: SERVER_ID GROUP_CONFIG
    SERVER_ID=$1
    CONFIG_FILE=$2
    DATA_PATH="data/$SERVER_ID"  # Default data path based on server ID
elif [ $# -eq 3 ]; then
    # Old format: DATA_PATH CLUSTER CURRENT_NODE
    echo "Warning: Using deprecated argument format. Please update to: $0 SERVER_ID GROUP_CONFIG"
    DATA_PATH=$1
    CONFIG_FILE=$2
    SERVER_ID=$3
else
    echo "Usage (recommended): $0 SERVER_ID GROUP_CONFIG"
    echo "  SERVER_ID: The ID of this server"
    echo "  GROUP_CONFIG: Path to the group configuration file (absolute path or relative to conf dir)"
    echo ""
    echo "Usage (deprecated): $0 DATA_PATH CLUSTER CURRENT_NODE"
    exit 1
fi

# Ensure data directory exists
mkdir -p "$DATA_PATH"

# Validate and resolve config file path
if [ -f "$CONFIG_FILE" ]; then
    RESOLVED_CONFIG="$CONFIG_FILE"
elif [ -f "$CONF_DIR/$CONFIG_FILE" ]; then
    RESOLVED_CONFIG="$CONF_DIR/$CONFIG_FILE"
else
    echo "Error: Configuration file not found at '$CONFIG_FILE' or '$CONF_DIR/$CONFIG_FILE'"
    exit 1
fi

# JVM Configuration
JMX_PORT=18101
GC_LOG="$SCRIPT_DIR/../logs/gc.log"

JAVA_BASE_OPTS="-Djava.awt.headless=true -Dfile.encoding=UTF-8"

JAVA_JMX_OPTS="-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.port=$JMX_PORT \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.authenticate=false"

JAVA_MEM_OPTS="-server -Xms2g -Xmx2g -Xmn600m \
-XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled \
-XX:+UseCMSCompactAtFullCollection -XX:LargePageSizeInBytes=128m \
-XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly \
-XX:CMSInitiatingOccupancyFraction=70"

JAVA_GC_OPTS="-verbose:gc -Xloggc:$GC_LOG \
-XX:+PrintGCDetails -XX:+PrintGCDateStamps"

# Set classpath based on system type
if [ "$CURRENT_SYS" == "MINGW" ] || [ "$CURRENT_SYS" == "CYGWI" ]; then
    CLASSPATH="$CONF_DIR;$LIB_DIR/*"
else
    CLASSPATH="$CONF_DIR:$LIB_DIR/*"
fi

# Run the server with all JVM options
java $JAVA_BASE_OPTS $JAVA_MEM_OPTS $JAVA_JMX_OPTS $JAVA_GC_OPTS \
    -cp "$CLASSPATH" \
    -Draft.data.dir="$DATA_PATH" \
    com.github.raftimpl.raft.example.server.ServerMain $SERVER_ID "$RESOLVED_CONFIG"
