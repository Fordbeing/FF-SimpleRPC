package com.ff;

import com.ff.common.service.UserService;
import com.ff.provider.UserServiceImpl;
import com.ff.registry.LocalRegistry;
import com.ff.server.RpcHttpServer;

import java.io.IOException;

public class ProviderMain {
    public static void main(String[] args) throws IOException {
        // 注册服务
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);

        // 启动 web 服务
        RpcHttpServer.start(8080); // 启动 RPC 服务端

    }
}