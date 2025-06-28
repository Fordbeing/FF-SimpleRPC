package com.ff.registry;

import com.ff.model.ServiceMetaInfo;

import java.util.List;

// 注册中心本地缓存
public class RegistryServiceCache {
    List<ServiceMetaInfo> serviceMetaInfoList;

    // 写缓存
    public void writeCache(List<ServiceMetaInfo> serviceMetaInfoList) {
        this.serviceMetaInfoList = serviceMetaInfoList;
    }

    // 读缓存
    public List<ServiceMetaInfo> readCache() {
        return serviceMetaInfoList;
    }

    // 清空缓存
    public void clearCache(){
        this.serviceMetaInfoList = null;
    }
}
