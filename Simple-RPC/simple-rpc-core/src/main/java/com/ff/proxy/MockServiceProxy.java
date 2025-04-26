package com.ff.proxy;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

@Slf4j
public class MockServiceProxy implements InvocationHandler {

    /*
     * 重写 invoke 方法，用于返回一个默认值的方法
     * Mock 服务代理 （JDK 动态代理）
     * */

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 获取方法返回的类型
        Class<?> returnType = method.getReturnType();
        log.info("mock invoke {}", method.getName());

        return getDefaultObject(returnType);
    }

    private Object getDefaultObject(Class<?> returnType) {
        if (returnType.isPrimitive()) { // 如果是基础数据类型
            if (returnType == boolean.class) {
                return false;
            } else if (returnType == int.class) {
                return 0;
            } else if (returnType == short.class) {
                return (short) 0;
            }else if(returnType == long.class){
                return 0L;
            }
        }
        // 如果是对象类型
        return null;
    }
}
