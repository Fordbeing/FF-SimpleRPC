package com.ff.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalRegistry {

    // 服务注册存储， 本地存储实现类， consumer 和 provider 都能使用
    public static final Map<String, Class<?>> map = new ConcurrentHashMap<>();

    // 服务注册
    public static void register(String serviceName, Class<?> implClass) {
        map.put(serviceName, implClass);
    }

    // 获取服务
    public static Class<?> get(String serviceName) {
        return map.get(serviceName);
    }

    // 删除服务
    public static void delete(String serviceName){
        map.remove(serviceName);
    }
}
