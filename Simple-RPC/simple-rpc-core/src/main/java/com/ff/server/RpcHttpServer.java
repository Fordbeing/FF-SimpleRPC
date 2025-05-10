package com.ff.server;

import com.ff.RpcApplication;
import com.ff.model.RpcRequest;
import com.ff.model.RpcResponse;
import com.ff.registry.LocalRegistry;
import com.ff.serializer.Serializer;
import com.ff.serializer.SerializerFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;

/**
 * 使用 HTTP 协议实现的 RPC 服务器，支持请求接收、反序列化、方法调用、响应返回等核心功能。
 */
public class RpcHttpServer implements RpcServer {

    /**
     * 启动 RPC 服务器，监听指定端口
     * @param port 监听端口
     * @throws IOException 启动失败时抛出异常
     */
    @Override
    public void start(int port) throws IOException {
        // 创建 HTTP 服务器绑定指定端口
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        // 设置请求处理器，所有请求交由 RpcHandler 处理
        server.createContext("/", new RpcHandler());
        server.setExecutor(null); // 使用默认单线程处理
        server.start(); // 启动服务器
        System.out.println("RPC Server started on port " + port);
    }

    /**
     * 向指定 URL 发送 POST 请求，携带二进制数据
     * @param url 请求地址
     * @param contentBytes 请求体内容（序列化后的字节数组）
     * @return 响应的字节数组
     * @throws Exception 网络异常或服务端错误
     */
    @Override
    public byte[] sendPost(String url, byte[] contentBytes) throws Exception {
        // 创建 URL 对象并打开连接
        java.net.URL targetUrl = new java.net.URL(url);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) targetUrl.openConnection();

        // 设置 POST 方法和请求头
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setRequestProperty("Content-Length", String.valueOf(contentBytes.length));

        // 发送请求体数据
        try (OutputStream os = connection.getOutputStream()) {
            os.write(contentBytes);
            os.flush();
        }

        // 读取正常响应
        try (InputStream is = connection.getInputStream()) {
            return is.readAllBytes();
        } catch (IOException e) {
            // 捕获错误流以便调试
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                byte[] errorBytes = errorStream.readAllBytes();
                System.err.println("Error response from server: " + new String(errorBytes));
            }
            throw e;
        } finally {
            connection.disconnect(); // 释放连接
        }
    }

    /**
     * 内部类：处理 RPC 请求的核心逻辑
     */
    static class RpcHandler implements HttpHandler {

        // 获取序列化器实例（如：Kryo、Hessian 等）
        private final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());

        /**
         * 处理 HTTP 请求：反序列化 -> 方法调用 -> 序列化返回
         * @param exchange 请求-响应交换对象
         * @throws IOException IO 异常
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("Received request: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());

            // 读取请求体（字节数组）
            InputStream inputStream = exchange.getRequestBody();
            byte[] requestBytes = inputStream.readAllBytes();

            RpcResponse rpcResponse = new RpcResponse(); // 初始化响应对象

            try {
                // 反序列化请求体为 RpcRequest 对象
                RpcRequest rpcRequest = serializer.deserialize(requestBytes, RpcRequest.class);

                // 根据服务名从注册中心获取实现类
                Class<?> implClass = LocalRegistry.get(rpcRequest.getServiceName());
                // 获取指定方法
                Method method = implClass.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
                // 创建实例并执行方法
                Object result = method.invoke(implClass.getDeclaredConstructor().newInstance(), rpcRequest.getParameters());

                // 设置返回结果
                rpcResponse.setResult(result);
            } catch (Exception e) {
                e.printStackTrace();
                // 异常设置进响应中
                rpcResponse.setException(e);
            }

            // 将响应对象序列化并发送回客户端
            byte[] responseBytes = serializer.serialize(rpcResponse);
            exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(200, responseBytes.length);

            // 写入响应体
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }
}
