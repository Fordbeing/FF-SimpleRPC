package com.ff.loadbalancer;

import cn.hutool.core.collection.CollUtil;
import com.ff.model.ServiceMetaInfo;

import java.util.List;
import java.util.Map;
import java.util.Random;

// 负载均衡：随机算法
public class RandomLoadBalancer implements LoadBalancer {
    private final Random random = new Random();

    @Override
    public ServiceMetaInfo select(Map<String, Object> requestParams, List<ServiceMetaInfo> serviceMetaInfoList) {
        if (CollUtil.isEmpty(serviceMetaInfoList)) {
            return null;
        }

        // 如果只有一个服务器，那么就直接返回
        if (serviceMetaInfoList.size() == 1) {
            return serviceMetaInfoList.getFirst();
        }

        return serviceMetaInfoList.get(random.nextInt(serviceMetaInfoList.size()));
    }
}
