package com.lightSpring;

import com.lightSpring.Annotations.*;
import com.lightSpring.Exception.ErrorResponseException;
import com.lightSpring.Exception.NestedRuntimeException;
import com.lightSpring.Utils.JsonUtils;
import com.lightSpring.Utils.PathUtils;
import com.lightspring.Context.BeanDefinition;
import com.lightspring.Context.ConfigurableApplicationContext;
import com.lightspring.Context.ResourceResolver;
import com.lightspring.Tools.ClassUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import com.lightSpring.Dispatcher.Result;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DispatcherServlet extends HttpServlet {

    ViewResolver viewResolver;

    private final String resourcePath;
    private final String faviconPath;

    private final ConfigurableApplicationContext applicationContext;

    private final List<Dispatcher> getDispatchers = new ArrayList<>();

    private final List<Dispatcher> postDispatchers = new ArrayList<>();

    public DispatcherServlet(ConfigurableApplicationContext applicationContext, Properties propertyResolver) {
        this.applicationContext = applicationContext;
        this.resourcePath = propertyResolver.getProperty("/static");
        this.faviconPath = propertyResolver.getProperty("/favicon.ico");
    }

    public void init() {
        for (BeanDefinition beanDefinition : applicationContext.findBeanDefinitions(Object.class)) {
            Object instance = beanDefinition.getInstance();
            Class<?> clazz = instance.getClass();
            Controller controllerAnno = clazz.getAnnotation(Controller.class);
            RestController restAnno = clazz.getAnnotation(RestController.class);
            if (controllerAnno != null && restAnno != null) {
                throw new RuntimeException("不能同时定义@Controller和@RestController");
            }
            if (controllerAnno != null) {
                addController(false, clazz, instance);
            }
            if (restAnno != null) {
                addController(true, clazz, instance);
            }
        }
    }

    private void addController(boolean isRest, Class<?> clazz, Object instance) {
        for (Method method : clazz.getDeclaredMethods()) {
            try {
                addMethod(isRest, instance, method);
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void addMethod(boolean isRest, Object instance, Method method) throws ServletException {
        boolean isResponseBody = false;
        boolean isVoid = false;
        method.setAccessible(true);
        checkMethod(method);
        Param[] params = parseMethodParams(method);
        ResponseBody responseBody = method.getAnnotation(ResponseBody.class);
        if (responseBody != null) {
            isResponseBody = true;
        }
        if (method.getReturnType().equals(void.class)) {
            isVoid = true;
        }
        //  TODO:暂时还不支持@RequestMapping注解，在类级别添加URI
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            getDispatchers.add(new Dispatcher(isRest, isResponseBody, isVoid,
                    PathUtils.compile(getMapping.value()), instance, method, params));
            return;
        }
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping != null) {
            postDispatchers.add(new Dispatcher(isRest, isResponseBody, isVoid,
                    PathUtils.compile(postMapping.value()), instance, method, params));
        }
        //  递归回到父类进行方法添加
        Class<?> superclass = instance.getClass().getSuperclass();
        if (superclass != null) {
            addController(isRest, superclass, instance);
        }
    }

    private Param[] parseMethodParams(Method method) {
        Parameter[] parameters = method.getParameters();
        Param[] params = new Param[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Class<?> type = parameters[i].getType();
            PathVariable pathVariable = parameters[i].getAnnotation(PathVariable.class);
            RequestParam requestParam = parameters[i].getAnnotation(RequestParam.class);
            RequestBody requestBody = parameters[i].getAnnotation(RequestBody.class);
            int count = (pathVariable != null ? 1 : 0) + (requestParam != null ? 1 : 0) + (requestBody != null ? 1 : 0);
            if (count > 1) {
                throw new RuntimeException("同一个参数不能拥有多个参数注解");
            }
            if (pathVariable != null) {
                String name = pathVariable.value();
                String defaultValue = !pathVariable.defaultValue().isEmpty() ? pathVariable.defaultValue() : null;
                params[i] = new Param(name, Param.ParamType.PATH_VARIABLE, type, defaultValue);
            } else if (requestParam != null) {
                String name = requestParam.value();
                String defaultValue = !requestParam.defaultValue().isEmpty() ? requestParam.defaultValue() : null;
                params[i] = new Param(name, Param.ParamType.REQUEST_PARAM, type, defaultValue);
            } else if (requestBody != null) {
                params[i] = new Param(null, Param.ParamType.REQUEST_BODY, type, null);
            } else {
                params[i] = new Param(null, Param.ParamType.SERVLET_VARIABLE, type, null);
                // check servlet variable type:
                if (type != HttpServletRequest.class && type != HttpServletResponse.class && type != HttpSession.class
                        && type != ServletContext.class) {
                    throw new RuntimeException("(Missing annotation?) Unsupported argument type: " + type + " at method: " + method);
                }
            }
        }
        return params;
    }

    private void checkMethod(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new RuntimeException("静态方法不可以成为控制器方法");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String url = req.getRequestURI();
        if (url.equals(this.faviconPath) || url.startsWith(this.resourcePath)) {
            doResource(url, req, resp);
        } else {
            doService(req, resp, this.getDispatchers);
        }
    }

    public void doService(HttpServletRequest req, HttpServletResponse resp, List<Dispatcher> dispatchers) throws ServletException, IOException {
        String url = req.getRequestURI();
        try {
            doService(url, req, resp, dispatchers);
        } catch (ErrorResponseException e) {
            if (!resp.isCommitted()) {
                resp.resetBuffer();
                resp.sendError(e.statusCode);
            }
        } catch (RuntimeException | ServletException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new NestedRuntimeException(e);
        }
    }

    void doService(String url, HttpServletRequest req, HttpServletResponse resp, List<Dispatcher> dispatchers) throws Exception {
        for (Dispatcher dispatcher : dispatchers) {
            Result result = dispatcher.process(url, req, resp);
            if (result.processed()) {
                Object r = result.returnObject();
                if (dispatcher.isRest) {
                    // send rest response:
                    if (!resp.isCommitted()) {
                        resp.setContentType("application/json");
                    }
                    if (dispatcher.isResponseBody) {
                        if (r instanceof String s) {
                            // send as response body:
                            PrintWriter pw = resp.getWriter();
                            pw.write(s);
                            pw.flush();
                        } else if (r instanceof byte[] data) {
                            // send as response body:
                            ServletOutputStream output = resp.getOutputStream();
                            output.write(data);
                            output.flush();
                        } else {
                            // error:
                            throw new ServletException("Unable to process REST result when handle url: " + url);
                        }
                    } else if (!dispatcher.isVoid) {
                        PrintWriter pw = resp.getWriter();
                        JsonUtils.writeJson(pw, r);
                        pw.flush();
                    }
                } else {
                    // process MVC:
                    if (!resp.isCommitted()) {
                        resp.setContentType("text/html");
                    }
                    if (r instanceof String s) {
                        if (dispatcher.isResponseBody) {
                            // send as response body:
                            PrintWriter pw = resp.getWriter();
                            pw.write(s);
                            pw.flush();
                        } else if (s.startsWith("redirect:")) {
                            // send redirect:
                            resp.sendRedirect(s.substring(9));
                        } else {
                            // error:
                            throw new ServletException("Unable to process String result when handle url: " + url);
                        }
                    } else if (r instanceof byte[] data) {
                        if (dispatcher.isResponseBody) {
                            // send as response body:
                            ServletOutputStream output = resp.getOutputStream();
                            output.write(data);
                            output.flush();
                        } else {
                            // error:
                            throw new ServletException("Unable to process byte[] result when handle url: " + url);
                        }
                    } else if (r instanceof ModelAndView mv) {
                        String view = mv.getViewName();
                        if (view.startsWith("redirect:")) {
                            // send redirect:
                            resp.sendRedirect(view.substring(9));
                        } else {
                            this.viewResolver.render(view, mv.getModel(), req, resp);
                        }
                    } else if (!dispatcher.isVoid && r != null) {
                        // error:
                        throw new ServletException("Unable to process " + r.getClass().getName() + " result when handle url: " + url);
                    }
                }
                return;
            }
        }
        // not found:
        resp.sendError(404, "Not Found");
    }

    public void doResource(String url, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletContext ctx = req.getServletContext();
        try (InputStream input = ctx.getResourceAsStream(url)) {
            if (input == null) {
                resp.sendError(404, "Not Found");
            } else {
                // guess content type:
                String file = url;
                int n = url.lastIndexOf('/');
                if (n >= 0) {
                    file = url.substring(n + 1);
                }
                String mime = ctx.getMimeType(file);
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                resp.setContentType(mime);
                ServletOutputStream output = resp.getOutputStream();
                input.transferTo(output);
                output.flush();
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doService(req, resp, this.getDispatchers);
    }

    @Override
    public void destroy() {
        applicationContext.close();
    }
}
