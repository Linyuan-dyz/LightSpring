package com.lightspring.Context;

import com.lightspring.Annotations.*;
import com.lightspring.Tools.ApplicationContextUtils;
import jakarta.annotation.Nullable;
import org.yaml.snakeyaml.introspector.Property;

import java.io.IOException;
import java.lang.reflect.*;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static com.lightspring.Tools.ApplicationContextUtils.closeApplicationContext;
import static com.lightspring.Tools.ClassUtils.*;

public class AnnotationConfigApplicationContext implements ConfigurableApplicationContext {

    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

    private final Set<String> dependNames = new HashSet<>();

    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    private ResourceResolver resourceResolver = new ResourceResolver("E:\\LightSpring\\lightSpring-Context\\src\\main\\resources\\test.properties");

    public AnnotationConfigApplicationContext(Class<?> type, @Nullable ResourceResolver.PropertyExpr propertyExpr) throws Exception {
        //  将当前容器储存到工具类中，方便取用
        ApplicationContextUtils.setApplicationContext(this);
        if (resourceResolver != null) {
            if (propertyExpr != null) {
                this.resourceResolver.addProperty(propertyExpr);
            }
        }
        //  扫描所有包
        List<String> list = scanClassNames(type);
        //  进行beanDefinition创建,已经校验了@Component注解
        createBeanDefinitions(list);
        //  进行@Configuration注解修饰的工厂类bean创建
        createBeanFactories();
        //  进行BeanPostProcessor创建
        createBeanPostProcessors();
        //  进行普通bean创建并使用BeanPostProcessor处理
        createCommonBeans();
        //  进行所有bean的字段注入
        beanDefinitionMap.values().forEach(this::injectBeans);
        //  进行init方法和@PostConstruct注解修饰方法的初始化
        beanDefinitionMap.values().forEach(this::initBeans);
    }

    @Override
    public boolean containsBean(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("名字不可以为空");
        }
        return beanDefinitionMap.containsKey(name);
    }

    @Override
    public <T> T getBean(String beanName) {
        return (T) beanDefinitionMap.get(beanName).getInstance();
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        return (T) findBeanDefinition(name, requiredType).getInstance();
    }

    @Override
    public void close() {
        List<BeanDefinition> list = beanDefinitionMap.values().stream()
                .filter(beanDefinition -> beanDefinition.getDestroyMethod() != null)
                .toList();
        for (BeanDefinition beanDefinition : list) {
            Method destroyMethod = beanDefinition.getDestroyMethod();
            try {
                destroyMethod.invoke(getOriginBean(beanDefinition));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        beanDefinitionMap.clear();
        dependNames.clear();
        beanPostProcessors.clear();
        closeApplicationContext();
    }

    public <T> T getBean(Class<T> clazz) {
        BeanDefinition beanDefinition = findBeanDefinition(clazz);
        return (T) beanDefinition.getInstance();
    }

    public <T> List<T> getBeans(Class<T> clazz) {
        return beanDefinitionMap.values().stream()
                .filter(beanDefinition -> clazz.isAssignableFrom(beanDefinition.getBeanClass()))
                .map(BeanDefinition::getInstance)
                .map(instance -> (T) instance)
                .toList();
    }

    @Override
    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return beanDefinitionMap.values().stream()
                .filter(beanDefinition -> type.isAssignableFrom(beanDefinition.getBeanClass()))
                .toList();
    }

    @Nullable
    @Override
    public BeanDefinition findBeanDefinition(Class<?> type) {
        List<BeanDefinition> list = findBeanDefinitions(type);
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        List<BeanDefinition> primaryList = list.stream()
                .filter(BeanDefinition::isPrimary)
                .toList();
        if (primaryList.size() == 1) {
            return primaryList.get(0);
        }
        if (primaryList.isEmpty()) {
            throw new RuntimeException("包含多个符合条件的bean但不存在primary");
        } else {
            throw new RuntimeException("存在多个primary");
        }
    }

    @Nullable
    @Override
    public BeanDefinition findBeanDefinition(String name) {
        return beanDefinitionMap.get(name);
    }

    @Nullable
    @Override
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        List<BeanDefinition> list = findBeanDefinitions(requiredType);
        for (BeanDefinition beanDefinition : list) {
            if (beanDefinition.getName().equals(name)) {
                return beanDefinition;
            }
        }
        return null;
    }

    private void createBeanPostProcessors() {
        List<BeanDefinition> list = beanDefinitionMap.values().stream()
                .filter(beanDefinition -> BeanPostProcessor.class.isAssignableFrom(beanDefinition.getBeanClass()))
                .sorted(Comparator.comparing(BeanDefinition::getOrder))
                .toList();
        list.forEach(beanDefinition -> {
            try {
                BeanPostProcessor beanPostProcessor = (BeanPostProcessor) createBean(beanDefinition);
                beanPostProcessors.add(beanPostProcessor);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void createCommonBeans() {
        List<BeanDefinition> commonList = beanDefinitionMap.values().stream()
                .filter(beanDefinition -> beanDefinition.getInstance() == null)
                .toList();
        commonList.forEach(common -> {
            try {
                createBean(common);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void createBeanFactories() {
        List<BeanDefinition> configurationList = beanDefinitionMap.values().stream()
                .filter(beanDefinition -> isConfiguration(beanDefinition.getBeanClass()))
                .toList();
        configurationList.forEach(configuration -> {
            try {
                createBean(configuration);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void initBeans(BeanDefinition beanDefinition) {
        if (beanDefinition.getInstance() == null || beanDefinition.getInitMethod() == null) {
            return;
        }
        injectInitMethod(beanDefinition.getInitMethod(), beanDefinition.getInstance());
    }

    private void injectInitMethod(Method initMethod, Object instance) {
        Parameter[] parameters = initMethod.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getType().getName();
            BeanDefinition paramBeanDefinition = beanDefinitionMap.get(paramName);
            if (paramBeanDefinition == null || paramBeanDefinition.getInstance() == null) {
                throw new RuntimeException("不存在对应的初始化方法参数");
            }
            args[i] = paramBeanDefinition.getInstance();
        }
        try {
            initMethod.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void injectBeans(BeanDefinition beanDefinition) {
        if (beanDefinition.getInstance() == null) {
            return;
        }
        Object bean = getOriginBean(beanDefinition);
        injectFields(beanDefinition, beanDefinition.getInstance().getClass(), bean);
    }

    private Object getOriginBean(BeanDefinition beanDefinition) {
        Object bean = beanDefinition.getInstance();
        List<BeanPostProcessor> list = this.beanPostProcessors;
        List<BeanPostProcessor> reversedList = list.reversed();
        for (BeanPostProcessor beanPostProcessor : reversedList) {
            bean = beanPostProcessor.getOriginBean(bean, beanDefinition.getName());
        }
        return bean;
    }

    private <T> void injectFields(BeanDefinition beanDefinition, Class<?> clazz, Object bean) {
        //  先进行setter方法的注入
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.isAnnotationPresent(Autowired.class)) {
                method.setAccessible(true);
                Parameter[] parameters = method.getParameters();
                Object[] args = new Object[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    String paramName = parameters[i].getType().getName();
                    BeanDefinition paramBeanDefinition = beanDefinitionMap.get(paramName);
                    if (paramBeanDefinition == null || paramBeanDefinition.getInstance() == null) {
                        throw new RuntimeException("自动注入setter方法失败");
                    }
                    args[i] = paramBeanDefinition.getInstance();
                }
                try {
                    method.invoke(bean, args);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        //  后进行@Autowired和@Value字段注入
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Value.class)) {
                field.setAccessible(true);
                String value = field.getAnnotation(Value.class).value();
                T property = (T) resourceResolver.getProperty(value, field.getType());
                try {
                    field.set(bean, property);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (field.isAnnotationPresent(Autowired.class)) {
                field.setAccessible(true);
                String fieldName = field.getType().getName();
                BeanDefinition fieldBeanDefinition = beanDefinitionMap.get(fieldName);
                if (fieldBeanDefinition == null || fieldBeanDefinition.getInstance() == null) {
                    throw new RuntimeException("@Autowired字段注入失败");
                }
                try {
                    field.set(bean, fieldBeanDefinition.getInstance());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        //  最后进行父类字段注入，使用递归实现
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            injectFields(beanDefinition, superclass, bean);
        }
    }

    //  TODO:实现对于注入方法和注入字段的统一
//    public void tryInject(BeanDefinition beanDefinition, AccessibleObject acc, Class<?> clazz) {
//        acc.setAccessible(true);
//        Object instance = beanDefinition.getInstance();
//
//    }

    public Object createBean(BeanDefinition beanDefinition) {
        //  递归调用时先检查是否存在bean
        if (beanDefinition.getInstance() != null) {
            return beanDefinition.getInstance();
        }
        if (!dependNames.add(beanDefinition.getName())) {
            throw new RuntimeException("出现循环依赖：" + beanDefinition.getName());
        }
        if (beanDefinition.getBeanClass().isAnnotation()) {
            return null;
        }
        String factoryMethodName = beanDefinition.getFactoryMethodName();
        Constructor<?> constructor = beanDefinition.getConstructor();
        Method factoryMethod = beanDefinition.getFactoryMethod();
        Executable constructMethod = factoryMethodName == null ? constructor : factoryMethod;
        Parameter[] parameters = constructMethod.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < args.length; i++) {
            Value valueAnno = parameters[i].getAnnotation(Value.class);
            Autowired autowiredAnno = parameters[i].getAnnotation(Autowired.class);
            if (valueAnno != null && autowiredAnno != null) {
                throw new RuntimeException("不能在同一个参数上加上@Value和@Autowired");
            }
            if (valueAnno != null) {
                args[i] = resourceResolver.getProperty(valueAnno.value(), parameters[i].getType());
            } else if (autowiredAnno != null) {
                // TODO:目前对于参数名的处理暂时先试用类型名，直接使用参数名会导致解析出错变成arg i
                BeanDefinition paramBeanDefinition = beanDefinitionMap.get(parameters[i].getType().getName());
                Object paramInstance = paramBeanDefinition.getInstance();
                if (paramInstance == null) {
                    paramInstance = createBean(paramBeanDefinition);
                }
                args[i] = paramInstance;
            } else {
                //  TODO 非依赖注入参数？
                throw new RuntimeException("无法在没有依赖注入的情况下填充参数");
            }
        }
        Object instance = null;
        //  使用构造方法
        if (factoryMethodName == null) {
            try {
                instance = constructor.newInstance(args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            beanDefinition.setInstance(instance);
        } else {
            //  使用工厂方法
            String factoryName = factoryMethod.getDeclaringClass().getName();
            Object factoryBean = beanDefinitionMap.get(factoryName).getInstance();
            try {
                instance = factoryMethod.invoke(factoryBean, args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            beanDefinition.setInstance(instance);
        }
        for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
            Object newInstance = beanPostProcessor.postProcessBeforeInitialization(instance, beanDefinition.getName());
            if (newInstance != instance) {
                beanDefinition.setInstance(newInstance);
            }
        }
        return instance;
    }

    private void createBeanDefinitions(List<String> list) {
        list.forEach(name -> {
            Class<?> clazz = null;
            try {
                clazz = Class.forName(name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (findAnnotation(clazz, Component.class) != null) {
                Constructor<?> constructor = null;
                if (!clazz.isAnnotation()) {
                    //  对于注解类不需要构造器
                    try {
                        constructor = getSituatableConstructor(clazz);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                boolean primary = getPrimary(clazz);
                int order = getOrder(clazz);
                Method[] declaredMethods = clazz.getDeclaredMethods();
                Method initMethod = getAnnotationMethod(declaredMethods, PostConstruct.class);
                Method destroyMethod = getAnnotationMethod(declaredMethods, PreDestroy.class);
                String initMethodName = initMethod == null ? null : initMethod.getName();
                String destroyMethodName = destroyMethod == null ? null : destroyMethod.getName();
                beanDefinitionMap.put(name, new BeanDefinition(name, clazz, null, constructor, null, null,
                        order, primary, initMethodName, destroyMethodName, initMethod, destroyMethod));
                if (clazz.getAnnotation(Configuration.class) != null) {
                    try {
                        handleBeanMethods(clazz);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    private void handleBeanMethods(Class<?> clazz) throws Exception{
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            Bean beanAnnotation = method.getAnnotation(Bean.class);
            if (beanAnnotation != null) {
                Class<?> resultType = method.getReturnType();
                String name = getClassNameByBeanAnnotation(method);
                BeanDefinition beanDefinition = new BeanDefinition(name, resultType, null, null,
                        method.getName(), method, getOrder(resultType), getPrimary(resultType),
                        null, null, null, null);
                beanDefinitionMap.put(name, beanDefinition);
            }
        }
    }

    public List<String> scanClassNames(Class<?> type) throws Exception {
        List<String> list = new ArrayList<>();
        componentScan(type, list);
        importScan(type, list);
        return list;
    }

    private void componentScan(Class<?> type, List<String> list) throws Exception {
        //  @ComponentScan(basePackages = "com.example.project")
        ComponentScan componentScan = type.getAnnotation(ComponentScan.class);
        if (componentScan == null) {
            return;
        }
        List<String> packageList = getPackageName(type, componentScan);
        for (String packageName : packageList) {
            list.addAll(scanPackage(packageName));
        }
    }

    private void importScan(Class<?> type, List<String> list) throws Exception {
        // @Import(Xyz.class)
        Import importAnnotation = type.getAnnotation(Import.class);
        if (importAnnotation == null) {
            return;
        }
        String[] values = importAnnotation.value();
        for (String value : values) {
            String name = value.substring(0, value.length() - ".class".length());
            name = name.replace('/', '.');
            list.add(name);
        }
    }

    private static List<String> getPackageName(Class<?> type, ComponentScan componentScan) {
        //  扫描配置类的ComponentScan注解包名，如果不存在则扫描当前包
        String[] basePackages = componentScan.basePackage();
        List<String> packageList = new ArrayList<>();
        if (basePackages == null || basePackages.length == 0) {
            packageList.add(type.getPackageName());
            return packageList;
        }
        packageList.addAll(Arrays.asList(basePackages));
        return packageList;
    }

    private List<String> scanPackage(String packageName) throws Exception {
        //  扫描包下的所有类，获取所有类名字
        List<String> list = new ArrayList<>();
        URL resource = this.getClass().getClassLoader().getResource(packageName.replace('.', '\\'));
        Path path = Path.of(resource.toURI());
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path absolutePath = file.toAbsolutePath();
                String pathString = absolutePath.toString();
                if (pathString.endsWith(".class")) {
                    pathString = pathString.replace('\\', '.');
                    int index = pathString.indexOf(packageName);
                    String className = pathString.substring(index, pathString.length() - ".class".length());
                    list.add(className);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return list;
    }

    public void printMap() {
        beanDefinitionMap.forEach((beanName, beanDefinition) -> {
            System.out.println("beanName: " + beanName + ", instance: " + beanDefinition.getInstance() +
                    ", constructor: " + beanDefinition.getConstructor());
        });
    }
}
