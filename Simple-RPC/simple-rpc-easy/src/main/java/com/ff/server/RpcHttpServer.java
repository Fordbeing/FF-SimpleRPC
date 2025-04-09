package com.ff.server;

import com.ff.model.RpcRequest;
import com.ff.model.RpcResponse;
import com.ff.registry.LocalRegistry;
import com.ff.serializer.JdkSerializer;
import com.ff.serializer.Serialize;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;

public class RpcHttpServer {

    // 采用原始的 Http 请求来实现，后续可扩展为 Netty

    public static void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RpcHandler()); // 所有请求都交给 RpcHandler 处理
        server.setExecutor(null); // 单线程
        server.start();
        System.out.println("RPC Server started on port " + port);
    }

    static class RpcHandler implements HttpHandler {

        private final Serialize serializer = new JdkSerializer();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("Received request: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());

            // 读取请求体
            InputStream inputStream = exchange.getRequestBody();
            byte[] requestBytes = inputStream.readAllBytes();

            RpcResponse rpcResponse = new RpcResponse();

            try {
                // 反序列化请求
                RpcRequest rpcRequest = serializer.deserialize(requestBytes, RpcRequest.class);

                // 找到对应的实现类和方法

                Class<?> implClass = LocalRegistry.get(rpcRequest.getServiceName());
                Method method = implClass.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
                Object result = method.invoke(implClass.getDeclaredConstructor().newInstance(), rpcRequest.getParameters());

                // 设置返回值
                rpcResponse.setResult(result);
            } catch (Exception e) {
                e.printStackTrace();
                rpcResponse.setException(e);
            }

            // 响应结果
            byte[] responseBytes = serializer.serialize(rpcResponse);
            exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }
}
