package com.lightSpring;

import com.lightSpring.Exception.ServerErrorException;
import com.lightSpring.Exception.ServerWebInputException;
import com.lightSpring.Utils.JsonUtils;
import com.lightSpring.Utils.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Dispatcher {
    // 是否返回REST:
    boolean isRest;
    // 是否有@ResponseBody:
    boolean isResponseBody;
    // 是否返回void:
    boolean isVoid;
    // URL正则匹配:
    Pattern urlPattern;
    // Bean实例:
    Object controller;
    // 处理方法:
    Method handlerMethod;
    // 方法参数:
    Param[] methodParameters;

    final static Result NOT_PROCESSED = new Result(false, null);

    Result process(String url, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Matcher matcher = urlPattern.matcher(url);
        if (matcher.matches()) {
            Object[] arguments = new Object[this.methodParameters.length];
            for (int i = 0; i < arguments.length; i++) {
                Param param = methodParameters[i];
                arguments[i] = switch (param.paramType) {
                    case PATH_VARIABLE -> {
                        try {
                            String s = matcher.group(param.name);
                            yield convertToType(param.classType, s);
                        } catch (IllegalArgumentException e) {
                            throw new ServerWebInputException("Path variable '" + param.name + "' not found.");
                        }
                    }
                    case REQUEST_BODY -> {
                        BufferedReader reader = request.getReader();
                        yield JsonUtils.readJson(reader, param.classType);
                    }
                    case REQUEST_PARAM -> {
                        String s = getOrDefault(request, param.name, param.defaultValue);
                        yield convertToType(param.classType, s);
                    }
                    case SERVLET_VARIABLE -> {
                        Class<?> classType = param.classType;
                        if (classType == HttpServletRequest.class) {
                            yield request;
                        } else if (classType == HttpServletResponse.class) {
                            yield response;
                        } else if (classType == HttpSession.class) {
                            yield request.getSession();
                        } else if (classType == ServletContext.class) {
                            yield request.getServletContext();
                        } else {
                            throw new ServerErrorException("Could not determine argument type: " + classType);
                        }
                    }
                };
            }
            Object result = null;
            try {
                result = this.handlerMethod.invoke(this.controller, arguments);
            } catch (InvocationTargetException e) {
                Throwable t = e.getCause();
                if (t instanceof Exception ex) {
                    throw ex;
                }
                throw e;
            } catch (ReflectiveOperationException e) {
                throw new ServerErrorException(e);
            }
            return new Result(true, result);
        }
        return NOT_PROCESSED;
    }

    Object convertToType(Class<?> classType, String s) {
        if (classType == String.class) {
            return s;
        } else if (classType == boolean.class || classType == Boolean.class) {
            return Boolean.valueOf(s);
        } else if (classType == int.class || classType == Integer.class) {
            return Integer.valueOf(s);
        } else if (classType == long.class || classType == Long.class) {
            return Long.valueOf(s);
        } else if (classType == byte.class || classType == Byte.class) {
            return Byte.valueOf(s);
        } else if (classType == short.class || classType == Short.class) {
            return Short.valueOf(s);
        } else if (classType == float.class || classType == Float.class) {
            return Float.valueOf(s);
        } else if (classType == double.class || classType == Double.class) {
            return Double.valueOf(s);
        } else {
            throw new ServerErrorException("Could not determine argument type: " + classType);
        }
    }

    String getOrDefault(HttpServletRequest request, String name, String defaultValue) {
        String s = request.getParameter(name);
        if (s == null) {
            if (WebUtils.DEFAULT_PARAM_VALUE.equals(defaultValue)) {
                throw new ServerWebInputException("Request parameter '" + name + "' not found.");
            }
            return defaultValue;
        }
        return s;
    }

    public Dispatcher(boolean isRest, boolean isResponseBody, boolean isVoid, Pattern urlPattern, Object controller, Method handlerMethod, Param[] methodParameters) {
        this.isRest = isRest;
        this.isResponseBody = isResponseBody;
        this.isVoid = isVoid;
        this.urlPattern = urlPattern;
        this.controller = controller;
        this.handlerMethod = handlerMethod;
        this.methodParameters = methodParameters;
    }

    static record Result(boolean processed, Object returnObject) {
    }
}

