package com.ff;

import com.ff.common.service.UserService;
import com.ff.provider.UserServiceImpl;
import com.ff.registry.LocalRegistry;
import com.ff.server.RpcHttpServer;
import com.ff.server.RpcServer;
import com.ff.server.RpcServerFactory;

import java.io.IOException;

public class ProviderMain {
    public static void main(String[] args) throws IOException {

        RpcApplication.init();

        // 注册服务 - > 后续改成注册中心
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);

        // 启动 web 服务
        RpcServer rpcServer = RpcServerFactory.getInstance(RpcApplication.getRpcConfig().getRpcServer());

        rpcServer.start(RpcApplication.getRpcConfig().getServerPort()); // 启动 RPC 服务端

    }
}