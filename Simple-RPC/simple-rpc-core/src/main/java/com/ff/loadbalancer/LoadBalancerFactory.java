package com.ff.loadbalancer;

import com.ff.spi.SpiLoader;

// 负载均衡容器工厂
public class LoadBalancerFactory {
    // 通过spi机制加载类信息
    static {
        SpiLoader.load(LoadBalancer.class);
    }

    // 默认的负载均衡器
    private static final LoadBalancer DEFAULT_LOAD_BALANCER = new RandomLoadBalancer();

    // 获取实例
    public static LoadBalancer getInstance(String key){
        return SpiLoader.getInstance(LoadBalancer.class, key);
    }
}
