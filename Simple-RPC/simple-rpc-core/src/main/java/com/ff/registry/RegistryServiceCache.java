package com.ff.registry;

import com.ff.model.ServiceMetaInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 注册中心本地缓存
public class RegistryServiceCache {
    Map<String, List<ServiceMetaInfo>> serviceCache = new ConcurrentHashMap<>();

    // 写缓存
    public void writeCache(String key, List<ServiceMetaInfo> serviceMetaInfoList) {
        serviceCache.put(key, serviceMetaInfoList);
    }

    // 读缓存
    public List<ServiceMetaInfo> readCache(String key) {
        return serviceCache.get(key);
    }

    // 清空单个缓存
    public void clearCacheByKey(String key){
        serviceCache.remove(key);
    }

    // 清空所有缓存
    public void clearCache(){
        serviceCache.clear();
    }
}
