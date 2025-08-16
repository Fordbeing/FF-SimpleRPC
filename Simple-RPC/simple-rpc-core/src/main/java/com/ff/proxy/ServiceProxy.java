package com.ff.proxy;

import cn.hutool.core.collection.CollUtil;
import com.ff.RpcApplication;
import com.ff.config.RpcConfig;
import com.ff.constant.RpcConstant;
import com.ff.fault.retry.RetryStrategy;
import com.ff.fault.retry.RetryStrategyFactory;
import com.ff.fault.tolerant.TolerantStrategy;
import com.ff.fault.tolerant.TolerantStrategyFactory;
import com.ff.loadbalancer.LoadBalancer;
import com.ff.loadbalancer.LoadBalancerFactory;
import com.ff.model.RpcRequest;
import com.ff.model.RpcResponse;
import com.ff.model.ServiceMetaInfo;
import com.ff.registry.Registry;
import com.ff.registry.RegistryFactory;
import com.ff.server.tcp.VertxTcpClient;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ServiceProxy implements InvocationHandler {

    // 通过 JDK代理 实现方法拦截，JDK 代理只能够拦截实现了接口的类，也就是实现了接口的类就要经过JDK代理
    // invoke 就是拦截之后具体的做法， 拦截之后就会把调用的接口、方法、类型参数、参数值给传过来，刚好符合我们的需求
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {


        // 构建请求
        RpcRequest request = RpcRequest.builder()
                .serviceName(method.getDeclaringClass().getName()) // 接口名称
                .methodName(method.getName()) // 方法名
                .parameterTypes(method.getParameterTypes())  // 方法参数类型
                .parameters(args) // 参数
                .build();

        try {
            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());

            ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
            serviceMetaInfo.setServiceName(method.getDeclaringClass().getName());
            serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
            List<ServiceMetaInfo> services = registry.getServices(serviceMetaInfo.getServiceKey());
            if (CollUtil.isEmpty(services)) {
                log.info("注册中心：{}，无法找到key：{} 的服务地址", method.getDeclaringClass().getName(), serviceMetaInfo.getServiceKey());
                throw new RuntimeException("暂无服务地址");
            }
            // 负载均衡算法
            LoadBalancer instance = LoadBalancerFactory.getInstance(rpcConfig.getLoadBalancer());
            Map<String, Object> requestParams = new HashMap<>();
            requestParams.put("methodName", request.getMethodName());
            requestParams.put("services", services); // 用于服务容错
            requestParams.put("request", request); // 用于服务容错使用
            ServiceMetaInfo serviceMetaInfoLoadBalancer = instance.select(requestParams, services);

            // 发送请求 serviceMetaInfoFirst.getServiceAddress() -> URL
            // 取消其他的服务器发送功能-暂时只能通过Vert.x进行发送
//            byte[] result = rpcServer.sendPost(serviceMetaInfoFirst.getServiceAddress(), bytes);

            // 重试机制
            try {
                RetryStrategy retryStrategy = RetryStrategyFactory.getInstance(RpcApplication.getRpcConfig().getRetryStrategy());
                // 进行重试
                RpcResponse rpcResponse = retryStrategy.retry(() ->
                        VertxTcpClient.sendPost(request, serviceMetaInfoLoadBalancer, 5)
                );
                return rpcResponse.getResult();
            } catch (Exception e) {
                // 容错机制
                TolerantStrategy tolerantStrategy = TolerantStrategyFactory.getInstance(RpcApplication.getRpcConfig().getTolerantStrategy());
                try {
                    return tolerantStrategy.tolerant(requestParams, e);
                } catch (Exception exception) {
                    log.error(e.getMessage(), e);
                    throw new RuntimeException("调用失败！");
                }
            }
            // 反序列化
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
