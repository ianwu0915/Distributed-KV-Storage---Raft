package com.github.raftimpl.raft.example.server.service;

/**
 * This is a example service. It is used to test raft.
 */
public interface ExampleService {

    ExampleProto.SetResponse set(ExampleProto.SetRequest request);

    ExampleProto.GetResponse get(ExampleProto.GetRequest request);
}
