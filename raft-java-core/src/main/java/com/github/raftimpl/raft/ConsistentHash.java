package com.github.raftimpl.raft;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Implements consistent hashing for distributing data across multiple Raft groups.
 * Uses virtual nodes to improve distribution balance.
 */
public class ConsistentHash<T> {
    // Number of virtual nodes per real node to improve distribution
    private final int numberOfReplicas;
    // Sorted map of hash values to nodes, using TreeMap for ordered keys
    private final SortedMap<Integer, T> circle = new TreeMap<>();

    /**
     * Creates a consistent hash ring with the specified number of virtual nodes.
     * @param numberOfReplicas Number of virtual nodes per real node
     * @param nodes Collection of initial nodes to add to the hash ring
     */
    public ConsistentHash(int numberOfReplicas, Collection<T> nodes) {
        this.numberOfReplicas = numberOfReplicas;
        for (T node : nodes) {
            add(node);
        }
    }

    /**
     * Adds a node to the hash ring by creating virtual nodes.
     * @param node The node to add
     */
    public void add(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(hash(node.toString() + i), node);
        }
    }

    /**
     * Removes a node and its virtual nodes from the hash ring.
     * @param node The node to remove
     */
    public void remove(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.remove(hash(node.toString() + i));
        }
    }

    /**
     * Gets the node responsible for the given key.
     * @param key The key to look up
     * @return The node responsible for the key, or null if the ring is empty
     */
    public T get(Object key) {
        if (circle.isEmpty()) {
            return null;
        }
        
        int hash = hash(key.toString());
        
        // If hash not found, get next node in circle
        if (!circle.containsKey(hash)) {
            SortedMap<Integer, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        
        return circle.get(hash);
    }

    /**
     * Implements FNV-1a hash algorithm for consistent distribution.
     * @param key The key to hash
     * @return The hash value
     */
    private int hash(String key) {
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