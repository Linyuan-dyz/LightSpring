package com.lightspring;

import com.lightspring.Annotations.Component;
import com.lightspring.Annotations.Order;
import com.lightspring.Context.BeanPostProcessor;

@Order(100)
@Component
public class FirstProxyBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (OriginBean.class.isAssignableFrom(bean.getClass())) {
            // 检测到OriginBean,创建FirstProxyBean:
            var proxy = new FirstProxyBean((OriginBean) bean);
            // 保存原始Bean:
            originBeanMap.put(beanName, bean);
            // 返回Proxy:
            return proxy;
        }
        return bean;
    }

    @Override
    public Object getOriginBean(Object bean, String beanName) {
        Object origin = originBeanMap.get(beanName);
        if (origin != null) {
            // 存在原始Bean时,返回原始Bean:
            return origin;
        }
        return bean;
    }
}

// 代理Bean:
class FirstProxyBean extends OriginBean {
    final OriginBean target;

    public FirstProxyBean(OriginBean target) {
        this.target = target;
    }
}
