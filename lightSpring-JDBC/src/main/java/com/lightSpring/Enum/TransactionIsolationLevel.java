package com.lightSpring.Enum;

import java.sql.Connection;

public enum TransactionIsolationLevel {
    TRANSACTION_NONE(Connection.TRANSACTION_NONE),

    TRANSACTION_READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),

    TRANSACTION_READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),

    TRANSACTION_REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),

    TRANSACTION_SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    private final int level; // 用来存储隔离级别的 int 值

    TransactionIsolationLevel(int level) {
        this.level = level;
    }

    // 获取对应的 int 值
    public int getLevel() {
        return level;
    }
}
