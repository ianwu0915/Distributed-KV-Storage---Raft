package com.github.raftimpl.raft.example.server.service.impl;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.instance.Endpoint;
import com.github.raftimpl.raft.Peer;
import com.github.raftimpl.raft.example.server.ExampleStateMachine;
import com.github.raftimpl.raft.example.server.service.ExampleProto;
import com.github.raftimpl.raft.example.server.service.ExampleService;
import com.github.raftimpl.raft.RaftNode;
import com.github.raftimpl.raft.proto.RaftProto;
import com.googlecode.protobuf.format.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.raftimpl.raft.RaftGroupManager;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is the implementation of the ExampleService interface.
 */
public class ExampleServiceImpl implements ExampleService {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleServiceImpl.class);
    private static JsonFormat jsonFormat = new JsonFormat();

    private final RaftNode raftNode;
    private final ExampleStateMachine stateMachine;
    private int leaderId = -1;
    private RpcClient leaderRpcClient = null;
    private Lock leaderLock = new ReentrantLock();
    private final RaftGroupManager groupManager;

    public ExampleServiceImpl(RaftNode raftNode, ExampleStateMachine stateMachine, RaftGroupManager groupManager) {
        this.raftNode = raftNode;
        this.stateMachine = stateMachine;
        this.groupManager = groupManager;
    }

    private void onLeaderChangeEvent() {
        if (raftNode.getLeaderId() != -1
                && raftNode.getLeaderId() != raftNode.getLocalServer().getServerId()
                && leaderId != raftNode.getLeaderId()) {
            leaderLock.lock();
            if (leaderId != -1 && leaderRpcClient != null) {
                leaderRpcClient.stop();
                leaderRpcClient = null;
                leaderId = -1;
            }
            leaderId = raftNode.getLeaderId();
            Peer peer = raftNode.getPeerMap().get(leaderId);
            Endpoint endpoint = new Endpoint(peer.getServer().getEndpoint().getHost(),
                    peer.getServer().getEndpoint().getPort());
            RpcClientOptions rpcClientOptions = new RpcClientOptions();
            rpcClientOptions.setGlobalThreadPoolSharing(true);
            leaderRpcClient = new RpcClient(endpoint, rpcClientOptions);
            leaderLock.unlock();
        }
    }

    /**
     * Handles write requests to the Raft cluster. This method implements sharding across
     * multiple Raft groups by routing requests to the appropriate group based on the key.
     *
     * @param request The SetRequest containing the key-value pair to write
     * @return SetResponse indicating success or failure of the operation
     */
    @Override
    public ExampleProto.SetResponse set(ExampleProto.SetRequest request) {
        ExampleProto.SetResponse.Builder responseBuilder = ExampleProto.SetResponse.newBuilder();
        
        // Get the appropriate raft group for this key
        RaftNode targetNode = groupManager.getRaftGroup(request.getKey());
        
        if (targetNode == null) {
            responseBuilder.setSuccess(false);
            return responseBuilder.build();
        }

        // If this request is for a different group, forward it
        if (!targetNode.getGroupId().equals(raftNode.getGroupId())) {
            // Forward to the correct group's leader
            return forwardRequestToGroup(request, targetNode.getGroupId());
        }
        
        // Handle the request in this group
        if (raftNode.getLeaderId() <= 0) {
            responseBuilder.setSuccess(false);
        } else if (raftNode.getLeaderId() != raftNode.getLocalServer().getServerId()) {
            // Forward to leader within this group
            onLeaderChangeEvent();
            ExampleService exampleService = BrpcProxy.getProxy(leaderRpcClient, ExampleService.class);
            ExampleProto.SetResponse responseFromLeader = exampleService.set(request);
            responseBuilder.mergeFrom(responseFromLeader);
        } else {
            byte[] data = request.toByteArray();
            boolean success = raftNode.replicate(data, RaftProto.EntryType.ENTRY_TYPE_DATA);
            responseBuilder.setSuccess(success);
        }

        return responseBuilder.build();
    }

    /**
     * Forwards a write request to the appropriate Raft group. This method handles the
     * cross-group communication when a request needs to be processed by a different group.
     *
     * @param request The original SetRequest to be forwarded
     * @param groupId The target group's identifier
     * @return SetResponse from the target group
     */
    private ExampleProto.SetResponse forwardRequestToGroup(ExampleProto.SetRequest request, String groupId) {
        ExampleProto.SetResponse.Builder responseBuilder = ExampleProto.SetResponse.newBuilder();
        
        // Get the target group's RaftNode
        RaftNode targetGroup = groupManager.getRaftGroup(groupId);
        if (targetGroup == null) {
            responseBuilder.setSuccess(false);
            return responseBuilder.build();
        }

        // Get the leader of the target group
        int targetLeaderId = targetGroup.getLeaderId();
        if (targetLeaderId <= 0) {
            responseBuilder.setSuccess(false);
            return responseBuilder.build();
        }

        // Get the leader's peer information
        Peer leaderPeer = targetGroup.getPeerMap().get(targetLeaderId);
        if (leaderPeer == null) {
            responseBuilder.setSuccess(false);
            return responseBuilder.build();
        }

        // Create RPC client to the target group's leader
        Endpoint leaderEndpoint = new Endpoint(
            leaderPeer.getServer().getEndpoint().getHost(),
            leaderPeer.getServer().getEndpoint().getPort()
        );
        
        RpcClientOptions rpcClientOptions = new RpcClientOptions();
        rpcClientOptions.setGlobalThreadPoolSharing(true);
        
        // Manual resource management since RpcClient doesn't implement AutoCloseable
        RpcClient rpcClient = null;
        try {
            rpcClient = new RpcClient(leaderEndpoint, rpcClientOptions);
            ExampleService exampleService = BrpcProxy.getProxy(rpcClient, ExampleService.class);
            ExampleProto.SetResponse response = exampleService.set(request);
            LOG.debug("Forward request successful to group {}: request={}, response={}", 
                    groupId, jsonFormat.printToString(request), jsonFormat.printToString(response));
            return response;
        } catch (Exception e) {
            LOG.error("Failed to forward request to group {}: request={}, error={}", 
                    groupId, jsonFormat.printToString(request), e);
            if (e.getCause() != null) {
                LOG.error("Caused by: ", e.getCause());
            }
            responseBuilder.setSuccess(false);
            return responseBuilder.build();
        } finally {
            if (rpcClient != null) {
                rpcClient.stop();  // Ensure RPC client is properly cleaned up
            }
        }
    }

    /**
     * Handles read requests from the Raft cluster with strong consistency guarantees.
     * Routes requests to the appropriate Raft group based on the key.
     *
     * @param request The GetRequest containing the key to read
     * @return GetResponse containing the value if found
     */
    @Override
    public ExampleProto.GetResponse get(ExampleProto.GetRequest request) {
        // Get the appropriate raft group for this key
        RaftNode targetNode = groupManager.getRaftGroup(request.getKey());
        
        if (targetNode == null) {
            return ExampleProto.GetResponse.newBuilder()
                .setValue("")  // Empty value indicates not found/error
                .build();
        }

        // If this request is for a different group, forward it
        if (!targetNode.getGroupId().equals(raftNode.getGroupId())) {
            return forwardGetRequestToGroup(request, targetNode.getGroupId());
        }
        
        // Process the request in this group
        ExampleProto.GetResponse response = stateMachine.get(request);
        LOG.info("get request, request={}, response={}", 
                jsonFormat.printToString(request),
                jsonFormat.printToString(response));
        return response;
    }

    /**
     * Forwards a read request to the appropriate Raft group.
     *
     * @param request The original GetRequest to be forwarded
     * @param groupId The target group's identifier
     * @return GetResponse from the target group
     */
    private ExampleProto.GetResponse forwardGetRequestToGroup(ExampleProto.GetRequest request, String groupId) {
        // Get the target group's RaftNode
        RaftNode targetGroup = groupManager.getRaftGroup(groupId);
        if (targetGroup == null) {
            return ExampleProto.GetResponse.newBuilder()
                .setValue("")
                .build();
        }

        // Get the leader of the target group
        int targetLeaderId = targetGroup.getLeaderId();
        if (targetLeaderId <= 0) {
            return ExampleProto.GetResponse.newBuilder()
                .setValue("")
                .build();
        }

        // Get the leader's peer information
        Peer leaderPeer = targetGroup.getPeerMap().get(targetLeaderId);
        if (leaderPeer == null) {
            return ExampleProto.GetResponse.newBuilder()
                .setValue("")
                .build();
        }

        // Create RPC client to the target group's leader
        Endpoint leaderEndpoint = new Endpoint(
            leaderPeer.getServer().getEndpoint().getHost(),
            leaderPeer.getServer().getEndpoint().getPort()
        );
        
        RpcClientOptions rpcClientOptions = new RpcClientOptions();
        rpcClientOptions.setGlobalThreadPoolSharing(true);
        
        // Manual resource management for RpcClient
        RpcClient rpcClient = null;
        try {
            rpcClient = new RpcClient(leaderEndpoint, rpcClientOptions);
            ExampleService exampleService = BrpcProxy.getProxy(rpcClient, ExampleService.class);
            return exampleService.get(request);
        } catch (Exception e) {
            LOG.error("Failed to forward get request to group {}: {}", groupId, e.getMessage());
            return ExampleProto.GetResponse.newBuilder()
                .setValue("")
                .build();
        } finally {
            if (rpcClient != null) {
                rpcClient.stop();
            }
        }
    }

}
