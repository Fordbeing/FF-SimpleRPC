package com.ff.proxy;

import com.ff.RpcApplication;
import com.ff.model.RpcRequest;
import com.ff.model.RpcResponse;
import com.ff.serializer.Serializer;
import com.ff.serializer.SerializerFactory;
import com.ff.server.RpcServer;
import com.ff.server.RpcServerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;


public class ServiceProxy implements InvocationHandler {

    final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());
    final RpcServer rpcServer = RpcServerFactory.getInstance(RpcApplication.getRpcConfig().getRpcServer());

    // 通过 JDK代理 实现方法拦截，JDK 代理只能够拦截实现了接口的类，也就是实现了接口的类就要经过JDK代理
    // invoke 就是拦截之后具体的做法
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {


        // 构建请求
        RpcRequest request = RpcRequest.builder()
                .serviceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameters(args)
                .parameterTypes(method.getParameterTypes())
                .build();

        try {
            byte[] bytes = serializer.serialize(request); // 将请求序列化

            // 发送请求
            // 拼接请求地址
            String serverHost = RpcApplication.getRpcConfig().getServerHost();
            int serverPort = RpcApplication.getRpcConfig().getServerPort();
            String url = String.format("http://%s:%d", serverHost, serverPort);

            byte[] result = rpcServer.sendPost(url, bytes);
            // 反序列化
            RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
            return rpcResponse.getResult();

        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }
}
