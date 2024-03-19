package com.github.raftimpl.raft;

import lombok.Getter;
import lombok.Setter;

/**
 * raft Confguration
 */
@Setter
@Getter
public class RaftOptions {

    // A follower would become a candidate if it doesn't receive any message
    // from the leader in electionTimeoutMs milliseconds
    private int electionTimeoutMilliseconds = 5000;

    // A leader sends RPCs at least this often, even if there is no data to send
    private int heartbeatPeriodMilliseconds = 500;

    // snapshot定時器執行間隔
    private int snapshotPeriodSeconds = 3600;
    // log entry大小達到snapshotMinLogSize，才做snapshot
    private int snapshotMinLogSize = 100 * 1024 * 1024;
    // maximum size of snapshot data in a single RPC
    private int maxSnapshotBytesPerRequest = 500 * 1024; // 500k

    private int maxLogEntriesPerRequest = 5000;

    // 一個segment文件大小，默認100m
    private int maxSegmentFileSize = 100 * 1000 * 1000;

    // follower與leader差距在catchupMargin，才可以參與選舉跟提供服務
    // unit: ms
    private long catchupMargin = 500;

    // replicate最大等待時間，單位ms
    private long maxAwaitTimeout = 1000;

    // 与其他节点进行同步、选主等操作的线程池大小
    // 與其他節點進行同步，選主等操作的線程池大小
    private int raftConsensusThreadNum = 20;

    // 是否異步寫數據；true表示主節點保存后就返回，然后異步同步给從節點；
    // false表示主節点同步给大多數从節點后才返回。
    // This is a trade-off between latency and consistency.
    // True: strong consistency, but high latency
    // False: eventual consistency, but low latency
    private boolean asyncWrite = false;

    // raft的log和snapshot父目錄，絕對路徑
    private String dataDir = System.getProperty("com.github.raftimpl.raft.data.dir");
}
