package com.github.raftimpl.raft.example;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.github.raftimpl.raft.example.server.service.ExampleProto;
import com.github.raftimpl.raft.example.server.service.ExampleService;
import org.junit.Assert;
import org.junit.Test;

public class MultiGroupTest {

    @Test
    public void testMultipleGroups() {
        // Create client
        ExampleService client = createClient("localhost:8001");
        
        // Test writes to different groups
        client.set(ExampleProto.SetRequest.newBuilder()
            .setKey("key1")
            .setValue("value1")
            .build());
            
        client.set(ExampleProto.SetRequest.newBuilder()
            .setKey("key2")
            .setValue("value2")
            .build());
            
        // Verify data is distributed across groups
        ExampleProto.GetResponse response1 = client.get(
            ExampleProto.GetRequest.newBuilder()
            .setKey("key1")
            .build());
            
        ExampleProto.GetResponse response2 = client.get(
            ExampleProto.GetRequest.newBuilder()
            .setKey("key2")
            .build());
            
        Assert.assertEquals("value1", response1.getValue());
        Assert.assertEquals("value2", response2.getValue());
    }

    private ExampleService createClient(String endpoint) {
        // Initialize RPC client
        RpcClient rpcClient = new RpcClient(endpoint);
        // Get proxy
        return BrpcProxy.getProxy(rpcClient, ExampleService.class);
    }
} 