#!/usr/bin/env bash

# Step 1: Build the core project
# Navigate to the core directory, clean, and install the project without running tests.
echo "Building raft-java-core..."
cd ../raft-java-core && mvn clean install -DskipTests
cd -
echo "Core build completed."

# Step 2: Build and package the example project
echo "Packaging raft-java-example..."
mvn clean package
echo "Example project packaged."

# Step 3: Prepare the environment
# Create a root directory for deployment if it doesn't exist.
EXAMPLE_TAR=raft-java-example-1.9.0-deploy.tar.gz
ROOT_DIR=./env
mkdir -p $ROOT_DIR
cd $ROOT_DIR

# Step 4: Set up and start example servers
# Define an array of server configurations
declare -a SERVERS=("example1" "example2" "example3")
declare -a ADDRESSES=("127.0.0.1:8051:1" "127.0.0.1:8052:2" "127.0.0.1:8053:3")

for i in "${!SERVERS[@]}"; do
  SERVER_DIR=${SERVERS[$i]}
  SERVER_ADDRESS=${ADDRESSES[$i]}

  echo "Setting up $SERVER_DIR..."
  mkdir $SERVER_DIR
  cd $SERVER_DIR

  # Copy and extract the deployment tarball
  cp -f ../../target/$EXAMPLE_TAR .
  tar -zxvf $EXAMPLE_TAR

  # Grant execution permissions to scripts
  chmod +x ./bin/*.sh

  # Start the server in the background
  echo "Starting server $SERVER_DIR with address $SERVER_ADDRESS..."
  nohup ./bin/run_server.sh ./data \
    "127.0.0.1:8051:1,127.0.0.1:8052:2,127.0.0.1:8053:3" \
    "$SERVER_ADDRESS" &
  echo "$SERVER_DIR started."
  cd -
done

# Step 5: Set up the client
echo "Setting up client..."
mkdir client
cd client

# Copy and extract the deployment tarball
cp -f ../../target/$EXAMPLE_TAR .
tar -zxvf $EXAMPLE_TAR

# Grant execution permissions to scripts
chmod +x ./bin/*.sh
echo "Client setup completed."
cd -

echo "All servers and client are set up successfully."
