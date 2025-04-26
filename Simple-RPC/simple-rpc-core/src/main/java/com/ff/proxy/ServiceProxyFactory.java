package com.ff.proxy;

import com.ff.RpcApplication;

import java.lang.reflect.Proxy;

public class ServiceProxyFactory {
    public static <T> T getProxy(Class<T> serviceClass) {

        // 如果是 mock 类型则直接返回默认值
        if (RpcApplication.getRpcConfig().isMock()) {
            return getMockProxy(serviceClass);
        }


        return (T) Proxy.newProxyInstance( // JDK动态代理机制
                serviceClass.getClassLoader(), // 指定代理类通过哪个类进行加载
                new Class[]{serviceClass}, // 代理类要实现哪些接口(只能代理接口) JDK 特性，不明白就去查一查JDK和CGLIB区别
                new ServiceProxy()); // 代理逻辑的处理器，实现 InvocationHandler 接口
    }

    public static <T> T getMockProxy(Class<T> serviceClass) {
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class[]{serviceClass},
                new MockServiceProxy());
    }
}
