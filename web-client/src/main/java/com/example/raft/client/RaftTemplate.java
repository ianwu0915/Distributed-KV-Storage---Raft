package com.example.raft.client;

import com.github.raftimpl.raft.example.server.service.ExampleProto;
import com.github.raftimpl.raft.example.server.service.ExampleService;
import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.googlecode.protobuf.format.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;

@Slf4j
@Component
public class RaftTemplate {
    @Value("${raft-impl.cluster.address}")
    private String address;
    private final JsonFormat format = new JsonFormat();
    private ConsistentHash consistentHash;

    @PostConstruct
    private void init() {
        // Initialize consistent hash with 3 virtual nodes per physical node
        consistentHash = new ConsistentHash(3);
        
        // Add all nodes from the cluster address
        Arrays.stream(address.split(","))
              .map(String::trim)
              .forEach(node -> consistentHash.addNode(node));
    }

    public String read(String key) {
        RpcClient rpcClient = consistentHash.getClient(key);
        if (rpcClient == null) {
            throw new RuntimeException("No available node for key: " + key);
        }
        
        ExampleService exampleService = BrpcProxy.getProxy(rpcClient, ExampleService.class);
        ExampleProto.GetRequest request = ExampleProto.GetRequest.newBuilder()
            .setKey(key).build();
        ExampleProto.GetResponse response = exampleService.get(request);
        String result = format.printToString(response);
        log.info("读请求执行，key={}：{}", key, result);
        return result;
    }

    public String write(String key, String value) {
        RpcClient rpcClient = consistentHash.getClient(key);
        if (rpcClient == null) {
            throw new RuntimeException("No available node for key: " + key);
        }
        
        ExampleService exampleService = BrpcProxy.getProxy(rpcClient, ExampleService.class);
        ExampleProto.SetRequest request = ExampleProto.SetRequest.newBuilder()
            .setKey(key).setValue(value).build();
        ExampleProto.SetResponse response = exampleService.set(request);
        String result = format.printToString(response);
        log.info("写请求执行，key={}，value={}：{}", key, value, result);
        return result;
    }
}