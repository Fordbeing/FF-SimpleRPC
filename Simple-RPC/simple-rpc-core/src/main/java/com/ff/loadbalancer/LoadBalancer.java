package com.ff.loadbalancer;

import com.ff.model.ServiceMetaInfo;

import java.util.List;
import java.util.Map;

// 负载均衡接口,用于实现负载均衡器
public interface LoadBalancer {
    ServiceMetaInfo select(Map<String, Object> requestParams, List<ServiceMetaInfo> serviceMetaInfoList);
}
