package com.lightSpring;

import com.lightSpring.Annotation.Transactional;
import com.lightSpring.Exception.TransactionException;
import com.lightSpring.Interface.TransactionalInterface;
import com.lightSpring.Utils.TransactionalUtils;
import com.lightspring.Annotations.Component;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class TransactionInvocationHandler implements InvocationHandler, TransactionalInterface {

    //  将事务状态数据储存在ThreadLocal中，方便获取和查验事务
    public static final ThreadLocal<TransactionStatus> transactionStatus = new ThreadLocal<>();

    private final DataSource dataSource;

    public TransactionInvocationHandler(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> clazz = proxy.getClass();
        if (clazz.getAnnotation(Transactional.class) != null) {
            Transactional transAnno = clazz.getAnnotation(Transactional.class);
            TransactionStatus transaction = transactionStatus.get();
            if (transaction == null) {
                try (Connection connection = dataSource.getConnection()) {
                    //  事务操作不可以影响数据库的autoCommit参数，只能先暂时调整
                    final boolean autoCommit = connection.getAutoCommit();
                    //  事务隔离级别也是一样的
                    final int actualLevel = connection.getTransactionIsolation();
                    int exceptLever = transAnno.level().getLevel();
                    if (autoCommit) {
                        connection.setAutoCommit(false);
                    }
                    //  如果期望隔离等级不为空且实际等级和期望等级不同，则设置为期望等级
                    if (actualLevel != exceptLever && exceptLever != Connection.TRANSACTION_NONE) {
                        connection.setTransactionIsolation(exceptLever);
                    }
                    try {
                        transactionStatus.set(new TransactionStatus(connection, exceptLever));
                        Object result = method.invoke(proxy, args);
                        connection.commit();
                        return result;
                    } catch (InvocationTargetException e) {
                        // 回滚事务:
                        TransactionException te = new TransactionException(e.getCause());
                        try {
                            connection.rollback();
                        } catch (SQLException sqle) {
                            te.addSuppressed(sqle);
                        }
                        throw te;
                    } finally {
                        transactionStatus.remove();
                        if (autoCommit) {
                            connection.setAutoCommit(true);
                        }
                        if (actualLevel != exceptLever) {
                            connection.setTransactionIsolation(actualLevel);
                        }
                    }
                }
            } else {
                return method.invoke(proxy, args);
            }
        }
        return method.invoke(proxy, args);
    }
}
