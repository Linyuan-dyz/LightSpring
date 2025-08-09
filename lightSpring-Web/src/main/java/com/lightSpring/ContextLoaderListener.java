package com.lightSpring;

import com.lightSpring.Utils.WebUtils;
import com.lightspring.Context.AnnotationConfigApplicationContext;
import com.lightspring.Context.ApplicationContext;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.Properties;

public class ContextLoaderListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        Properties propertyResolver = WebUtils.createPropertyResolver();
        String encoding = propertyResolver.getProperty("${spring.web.character-encoding:UTF-8}");
        servletContext.setRequestCharacterEncoding(encoding);
        servletContext.setResponseCharacterEncoding(encoding);
        //  启动ioc容器
        Object applicationContext = createApplicationContext(servletContext.getInitParameter("configuration"), propertyResolver);
        //  注册Dispatcher Servlet
        WebUtils.registerDispatcherServlet(servletContext, propertyResolver);

        servletContext.setAttribute("applicationContext", applicationContext);
    }

    private Object createApplicationContext(String configuration, Properties propertyResolver) {
        if (configuration == null || configuration.isEmpty()) {
            throw new IllegalArgumentException("配置为空或配置文件为空");
        }
        try {
            Class<?> clazz = Class.forName(configuration);
            return new AnnotationConfigApplicationContext(clazz, propertyResolver);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        //  TODO:感觉会有问题？
        if (sce.getServletContext().getAttribute("applicationContext") instanceof ApplicationContext applicationContext) {
            applicationContext.close();
        }
    }


}
