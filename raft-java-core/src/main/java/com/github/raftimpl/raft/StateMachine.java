package com.github.raftimpl.raft;

/**
 * Raft状态机接口类
 */
public interface StateMachine {

    /**
     * 對狀態中數據进行snapshot，每个節點本地定时取用
     * @param snapshotDir snapshot數據輸出目錄
     */
    void writeSnapshot(String snapshotDir);

    /**
     * 讀取snapshot到狀態機，節點啟動時调用
     * @param snapshotDir snapshot數據目錄
     */
    void readSnapshot(String snapshotDir);

    /**
     * 將數據應用到狀態機
     * @param dataBytes 二進制
     */
    void apply(byte[] dataBytes);
}
