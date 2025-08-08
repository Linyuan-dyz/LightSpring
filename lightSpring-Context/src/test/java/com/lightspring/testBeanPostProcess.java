package com.lightspring;

import com.lightspring.Annotations.ComponentScan;
import com.lightspring.Context.AnnotationConfigApplicationContext;
import com.lightspring.Context.ResourceResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class testBeanPostProcess {

    @Test
    public void test01() throws Exception {
        var ctx = new AnnotationConfigApplicationContext(ScanApplication.class, (ResourceResolver.PropertyExpr) null);

        // 获取OriginBean的实例,此处获取的应该是SendProxyBeanProxy:
        OriginBean proxy = ctx.getBean(OriginBean.class);
        assertSame(SecondProxyBean.class, proxy.getClass());

        // proxy的name和version字段并没有被注入:
        assertNull(proxy.name);
        assertNull(proxy.version);

        // 但是调用proxy的getName()会最终调用原始Bean的getName(),从而返回正确的值:
        assertEquals("Scan App", proxy.getName());

        // 获取InjectProxyOnConstructorBean实例:
        var inject = ctx.getBean(InjectProxyOnConstructorBean.class);
        assertSame(proxy, inject.injected);

    }

    @ComponentScan
    public static class ScanApplication {

    }

}
