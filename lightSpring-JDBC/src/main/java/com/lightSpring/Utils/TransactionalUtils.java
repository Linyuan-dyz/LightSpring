package com.lightSpring.Utils;

import com.lightSpring.TransactionInvocationHandler;
import com.lightSpring.TransactionStatus;

import java.sql.Connection;

public class TransactionalUtils {

    public static Connection getCurrentConnection() {
        TransactionStatus transactionStatus = TransactionInvocationHandler.transactionStatus.get();
        if (transactionStatus == null) {
            return null;
        }
        return transactionStatus.getConnection() == null ? null : transactionStatus.getConnection();
    }
}
