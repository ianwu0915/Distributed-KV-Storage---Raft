package com.example.raft.client;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConsistentHash {
    private final int numberOfReplicas;
    private final SortedMap<Integer, String> circle = new TreeMap<>();
    private final ConcurrentMap<String, RpcClient> clientMap = new ConcurrentHashMap<>();

    public ConsistentHash(int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    public void addNode(String node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(getHash(node + i), node);
        }
        if (!clientMap.containsKey(node)) {
            clientMap.put(node, new RpcClient("list://" + node));
        }
    }

    public void removeNode(String node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.remove(getHash(node + i));
        }
        RpcClient client = clientMap.remove(node);
        if (client != null) {
            client.stop();
        }
    }

    public String getNode(String key) {
        if (circle.isEmpty()) {
            return null;
        }
        int hash = getHash(key);
        if (!circle.containsKey(hash)) {
            SortedMap<Integer, String> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    public RpcClient getClient(String key) {
        String node = getNode(key);
        return node != null ? clientMap.get(node) : null;
    }

    private int getHash(String key) {
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < key.length(); i++) {
            hash = (hash ^ key.charAt(i)) * p;
        }
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        return hash;
    }
} 