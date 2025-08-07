package com.lightSpring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MainTests {

    @Test
    public void mainTests() {
        // 原始Bean:
        OriginBean origin = new OriginBean();
        origin.name = "Bob";
// 调用原始Bean的hello():
        assertEquals("Hello, Bob.", origin.hello());

// 创建Proxy:
        OriginBean proxy = new ProxyResolver().createProxy(origin, new PoliteInvocationHandler());

// Proxy类名,类似OriginBean$ByteBuddy$9hQwRy3T:
        System.out.println(proxy.getClass().getName());

// Proxy类与OriginBean.class不同:
        assertNotSame(OriginBean.class, proxy.getClass());
// proxy实例的name字段应为null:
        assertNull(proxy.name);

// 调用带@Polite的方法:
        assertEquals("Hello, Bob!", proxy.hello());
// 调用不带@Polite的方法:
        assertEquals("Morning, Bob.", proxy.morning());

    }
}
