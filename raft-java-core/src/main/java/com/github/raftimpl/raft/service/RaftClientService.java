package com.github.raftimpl.raft.service;

import com.github.raftimpl.raft.proto.RaftProto;

/**
 * This interface defines the contract for client side to call.
 */
public interface RaftClientService {

    /**
     * 獲取raft集群leader節點資訊
     * @param request 請求
     * @return leader節點
     */
    RaftProto.GetLeaderResponse getLeader(RaftProto.GetLeaderRequest request);

    /**
     * 獲取raft集群所有節點信息。
     * @param request 请求
     * @return raft集群各節點地址，以及主從關係。
     */
    RaftProto.GetConfigurationResponse getConfiguration(RaftProto.GetConfigurationRequest request);

    /**
     * 向raft集群添加節點。
     * @param request 要添加的節點資訊。
     * @return 成功與否。
     */
    RaftProto.AddPeersResponse addPeers(RaftProto.AddPeersRequest request);

    /**
     * 从raft集群删除節點
     * @param request 请求
     * @return 成功與否。
     */
    RaftProto.RemovePeersResponse removePeers(RaftProto.RemovePeersRequest request);
}
