package com.lightSpring.Exception;

public class TransactionException extends Exception {

    // 构造函数：传入错误消息
    public TransactionException(String message) {
        super(message);
    }

    // 构造函数：传入导致异常的原始 Throwable 对象
    public TransactionException(Throwable cause) {
        super(cause);
    }

    // 构造函数：传入错误消息和导致异常的原始 Throwable 对象
    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
