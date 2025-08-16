package com.ff.fault.tolerant;

import cn.hutool.core.collection.CollUtil;
import com.ff.RpcApplication;
import com.ff.fault.retry.RetryStrategy;
import com.ff.fault.retry.RetryStrategyFactory;
import com.ff.loadbalancer.LoadBalancer;
import com.ff.loadbalancer.LoadBalancerFactory;
import com.ff.model.RpcRequest;
import com.ff.model.RpcResponse;
import com.ff.model.ServiceMetaInfo;
import com.ff.server.tcp.VertxTcpClient;

import java.util.List;
import java.util.Map;

public class FileOverTolerantStrategy implements TolerantStrategy {
    @Override
    public RpcResponse tolerant(Map<String, Object> context, Exception e) {
        LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(RpcApplication.getRpcConfig().getLoadBalancer());
        List<ServiceMetaInfo> services = (List<ServiceMetaInfo>) context.get("services");

        RpcRequest request = (RpcRequest) context.get("request");
        ServiceMetaInfo serviceMetaInfoLoadBalancer = loadBalancer.select(context, services);
        RetryStrategy retryStrategy = RetryStrategyFactory.getInstance(RpcApplication.getRpcConfig().getRetryStrategy());
        // 进行重试
        try {
            return retryStrategy.retry(() ->
                    VertxTcpClient.sendPost(request, serviceMetaInfoLoadBalancer, 5)
            );
        } catch (Exception exception) {
            throw new RuntimeException("服务调用失败！");
        }
    }
}
