package com.ff.proxy;

import cn.hutool.core.collection.CollUtil;
import com.ff.RpcApplication;
import com.ff.config.RpcConfig;
import com.ff.constant.RpcConstant;
import com.ff.model.RpcRequest;
import com.ff.model.RpcResponse;
import com.ff.model.ServiceMetaInfo;
import com.ff.registry.Registry;
import com.ff.registry.RegistryFactory;
import com.ff.serializer.Serializer;
import com.ff.serializer.SerializerFactory;
import com.ff.server.RpcServer;
import com.ff.server.RpcServerFactory;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

@Slf4j
public class ServiceProxy implements InvocationHandler {

    final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());
    final RpcServer rpcServer = RpcServerFactory.getInstance(RpcApplication.getRpcConfig().getRpcServer());

    // 通过 JDK代理 实现方法拦截，JDK 代理只能够拦截实现了接口的类，也就是实现了接口的类就要经过JDK代理
    // invoke 就是拦截之后具体的做法
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
            byte[] bytes = serializer.serialize(request); // 将请求序列化

            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());

            ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
            serviceMetaInfo.setServiceName(method.getDeclaringClass().getName());
            serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
            List<ServiceMetaInfo> services = registry.getServices(serviceMetaInfo.getServiceKey());
            if(CollUtil.isEmpty(services)){
                log.info("注册中心：{}，无法找到key：{} 的服务地址", method.getDeclaringClass().getName(), serviceMetaInfo.getServiceKey());
                throw new RuntimeException("暂无服务地址");
            }
            // TODO:后续再添加负载均衡
            // 暂时取第一个
            ServiceMetaInfo serviceMetaInfoFirst = services.getFirst();

            // 发送请求 serviceMetaInfoFirst.getServiceAddress() -> URL
            byte[] result = rpcServer.sendPost(serviceMetaInfoFirst.getServiceAddress(), bytes);
            // 反序列化
            RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
            return rpcResponse.getResult();

        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }
}
