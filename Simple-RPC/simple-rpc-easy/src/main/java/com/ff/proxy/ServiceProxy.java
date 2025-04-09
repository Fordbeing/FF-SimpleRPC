package com.ff.proxy;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.ff.model.RpcRequest;
import com.ff.model.RpcResponse;
import com.ff.serializer.JdkSerializer;
import com.ff.serializer.Serialize;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;


public class ServiceProxy implements InvocationHandler {

    // 通过 JDK代理 实现方法拦截，JDK 代理只能够拦截实现了接口的类，也就是实现了接口的类就要经过JDK代理
    // invoke 就是拦截之后具体的做法
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Serialize serialize = new JdkSerializer();

        // 构建请求
        RpcRequest request = RpcRequest.builder()
                .serviceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameters(args)
                .parameterTypes(method.getParameterTypes())
                .build();

        try {
            byte[] bytes = serialize.serialize(request); // 将请求序列化

            // 发送请求
            // 此处采用硬编码，后续可通过配置文件实现动态请求
            try(HttpResponse httpResponse = HttpRequest.post("http://localhost:8080").body(bytes).execute()) {
                byte[] result = httpResponse.bodyBytes();

                // 反序列化
                RpcResponse rpcResponse = serialize.deserialize(result, RpcResponse.class);
                return rpcResponse.getResult();
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }
}
