package com.lightSpring;

import com.lightspring.Context.AnnotationConfigApplicationContext;
import com.lightspring.Context.ResourceResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AroundProxyTest {

    @Test
    public void testAroundProxy() throws Exception {
        try (var ctx = new AnnotationConfigApplicationContext(AroundApplication.class, createPropertyResolver())) {
            OriginBean proxy = ctx.getBean(OriginBean.class);
            // OriginBean$ByteBuddy$8NoD1FcQ
            System.out.println(proxy.getClass().getName());

            // proxy class, not origin class:
            assertNotSame(OriginBean.class, proxy.getClass());
            // proxy.name not injected:
            assertNull(proxy.name);

            assertEquals("Hello, Bob!", proxy.hello());
            assertEquals("Morning, Bob.", proxy.morning());

            // test injected proxy:
            OtherBean other = ctx.getBean(OtherBean.class);
            assertSame(proxy, other.origin);
            assertEquals("Hello, Bob!", other.origin.hello());
        }
    }

    ResourceResolver.PropertyExpr createPropertyResolver() {
        var ps = new ResourceResolver.PropertyExpr("customer.name", "Bob");
        return ps;
    }
}