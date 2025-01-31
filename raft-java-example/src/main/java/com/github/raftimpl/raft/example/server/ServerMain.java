package com.github.raftimpl.raft.example.server;

import com.baidu.brpc.server.RpcServer;
import com.github.raftimpl.raft.RaftOptions;
import com.github.raftimpl.raft.example.server.service.ExampleService;
import com.github.raftimpl.raft.RaftNode;
import com.github.raftimpl.raft.example.server.service.impl.ExampleServiceImpl;
import com.github.raftimpl.raft.proto.RaftProto;
import com.github.raftimpl.raft.service.RaftClientService;
import com.github.raftimpl.raft.service.RaftConsensusService;
import com.github.raftimpl.raft.service.impl.RaftClientServiceImpl;
import com.github.raftimpl.raft.service.impl.RaftConsensusServiceImpl;
import com.github.raftimpl.raft.RaftGroupManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * This class provides a command line interface for starting a Raft node.
 */
public class ServerMain {
    private static RaftGroupManager groupManager;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.printf("Usage: ./run_server.sh SERVER_ID GROUP_CONFIG\n");
            // e.g. ./run_server.sh 1 /path/to/raft-groups.properties
            System.exit(-1);
        }

        int serverId = Integer.parseInt(args[0]);
        String configPath = args[1];
        
        // Load group configurations
        Properties groupConfig = loadGroupConfig(configPath);
        String groupId = findGroupForServer(serverId, groupConfig);
        
        // Initialize the server for its group
        initializeServer(serverId, groupId, groupConfig);
    }

    private static void initializeServer(int serverId, String groupId, Properties config) {
        String servers = config.getProperty(groupId + ".servers");
        String dataDir = config.getProperty(groupId + ".data.dir");

        List<RaftProto.Server> serverList = parseServers(servers);
        RaftProto.Server localServer = findLocalServer(serverId, serverList);

        // Initialize RaftOptions
        RaftOptions raftOptions = new RaftOptions();
        raftOptions.setDataDir(dataDir);
        
        // Initialize StateMachine
        ExampleStateMachine stateMachine = new ExampleStateMachine(dataDir);
        
        // Initialize RaftNode
        RaftNode raftNode = new RaftNode(raftOptions, serverList, localServer, stateMachine, groupId);

        // Initialize or get GroupManager
        if (groupManager == null) {
            List<String> allGroupIds = getAllGroupIds(config);
            groupManager = new RaftGroupManager(allGroupIds);
        }
        groupManager.addRaftGroup(groupId, raftNode);

        // Start RPC server
        RpcServer server = new RpcServer(localServer.getEndpoint().getPort());
        registerServices(server, raftNode, stateMachine);
        server.start();
        raftNode.init();
    }

    private static List<String> getAllGroupIds(Properties config) {
        List<String> groupIds = new ArrayList<>();
        for (String key : config.stringPropertyNames()) {
            if (key.endsWith(".servers")) {
                groupIds.add(key.substring(0, key.length() - 8));
            }
        }
        return groupIds;
    }

    private static Properties loadGroupConfig(String configPath) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(configPath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load group configuration from " + configPath, e);
        }
        return properties;
    }

    private static String findGroupForServer(int serverId, Properties config) {
        for (String key : config.stringPropertyNames()) {
            if (key.endsWith(".servers")) {
                String groupId = key.substring(0, key.length() - 8);
                String servers = config.getProperty(key);
                if (servers.contains(":" + serverId + ":") || servers.contains(":" + serverId + ",")) {
                    return groupId;
                }
            }
        }
        throw new IllegalArgumentException("Server ID " + serverId + " not found in any group configuration");
    }

    private static List<RaftProto.Server> parseServers(String servers) {
        List<RaftProto.Server> serverList = new ArrayList<>();
        String[] serverStrings = servers.split(",");
        
        for (String serverString : serverStrings) {
            String[] parts = serverString.split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid server format. Expected host:port:serverId but got: " + serverString);
            }
            
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            int serverId = Integer.parseInt(parts[2]);
            
            RaftProto.Endpoint endpoint = RaftProto.Endpoint.newBuilder()
                .setHost(host)
                .setPort(port)
                .build();
                
            RaftProto.Server server = RaftProto.Server.newBuilder()
                .setServerId(serverId)
                .setEndpoint(endpoint)
                .build();
                
            serverList.add(server);
        }
        return serverList;
    }

    private static RaftProto.Server findLocalServer(int serverId, List<RaftProto.Server> serverList) {
        return serverList.stream()
            .filter(server -> server.getServerId() == serverId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Local server ID " + serverId + " not found in server list"));
    }

    private static void registerServices(RpcServer server, RaftNode raftNode, ExampleStateMachine stateMachine) {
        // Register Raft consensus service for inter-node communication
        RaftConsensusService raftConsensusService = new RaftConsensusServiceImpl(raftNode);
        server.registerService(raftConsensusService);
        
        // Register client service for client requests
        RaftClientService raftClientService = new RaftClientServiceImpl(raftNode);
        server.registerService(raftClientService);
        
        // Register application-specific service with groupManager
        ExampleService exampleService = new ExampleServiceImpl(raftNode, stateMachine, groupManager);
        server.registerService(exampleService);
    }
}
