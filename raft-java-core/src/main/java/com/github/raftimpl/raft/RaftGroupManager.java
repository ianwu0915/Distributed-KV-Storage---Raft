package com.github.raftimpl.raft;

import com.github.raftimpl.raft.ConsistentHash;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RaftGroupManager {
    private final Map<String, RaftNode> raftGroups = new ConcurrentHashMap<>();
    private final ConsistentHash<String> consistentHash;
    
    public RaftGroupManager(List<String> groupIds) {
        this.consistentHash = new ConsistentHash<>(100, groupIds);
    }

    public void addRaftGroup(String groupId, RaftNode node) {
        raftGroups.put(groupId, node);
    }

    public RaftNode getRaftGroup(String key) {
        String groupId = consistentHash.get(key);
        return raftGroups.get(groupId);
    }

    public List<RaftNode> getAllGroups() {
        return new ArrayList<>(raftGroups.values());
    }
} 