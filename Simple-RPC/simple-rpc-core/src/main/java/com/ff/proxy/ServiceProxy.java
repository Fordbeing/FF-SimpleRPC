package com.ff.proxy;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.ff.RpcApplication;
import com.ff.model.RpcRequest;
import com.ff.model.RpcResponse;
import com.ff.serializer.Serializer;
import com.ff.serializer.SerializerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;


public class ServiceProxy implements InvocationHandler {

    final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());

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
            try(HttpResponse httpResponse = HttpRequest.post(url).body(bytes).execute()) {
                byte[] result = httpResponse.bodyBytes();

                // 反序列化
                RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
                return rpcResponse.getResult();
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }
}
