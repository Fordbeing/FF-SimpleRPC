package com.ff;


import com.ff.common.service.UserService;
import com.ff.config.RegistryConfig;
import com.ff.config.RpcConfig;
import com.ff.model.ServiceMetaInfo;
import com.ff.provider.UserServiceImpl;
import com.ff.registry.LocalRegistry;
import com.ff.registry.Registry;
import com.ff.registry.RegistryFactory;
import com.ff.server.RpcServer;
import com.ff.server.RpcServerFactory;

import java.io.IOException;

public class ProviderMain {

    public static void main(String[] args) throws IOException {
        // RPC 框架初始化
        RpcApplication.init();

        // 注册服务
        String serviceName = UserService.class.getName();
        // 本地注册
        LocalRegistry.register(serviceName, UserServiceImpl.class);

        // 注册服务到注册中心
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
        serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
        try {
            registry.registry(serviceMetaInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 启动 web 服务
        RpcServer rpcServer = RpcServerFactory.getInstance(RpcApplication.getRpcConfig().getRpcServer());
        rpcServer.start(RpcApplication.getRpcConfig().getServerPort()); // 启动 RPC 服务端
    }
}
