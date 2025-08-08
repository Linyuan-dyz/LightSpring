package com.lightSpring;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class TransactionStatus {

    final Connection connection;

    int transactionIsolationLevel;

    public TransactionStatus(Connection connection) {
        this.connection = connection;
        try {
            this.transactionIsolationLevel = connection.getTransactionIsolation();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public TransactionStatus(Connection connection, int transactionIsolationLevel) {
        this.connection = connection;
        this.transactionIsolationLevel = transactionIsolationLevel;
    }

    public int getTransactionIsolationLevel() {
        return transactionIsolationLevel;
    }

    public Connection getConnection() {
        return connection;
    }
}
