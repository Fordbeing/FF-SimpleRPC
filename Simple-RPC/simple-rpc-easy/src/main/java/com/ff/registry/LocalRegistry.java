package com.ff.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalRegistry {

    // 使用 Map 存储，后续扩展为 Etcd
    // 服务注册存储
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
