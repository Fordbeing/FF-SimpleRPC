package com.ff.server;

import com.ff.RpcApplication;
import com.ff.model.RpcRequest;
import com.ff.model.RpcResponse;
import com.ff.registry.LocalRegistry;
import com.ff.serializer.Serializer;
import com.ff.serializer.SerializerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.WebClient;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * 基于 Vert.x 的异步 HTTP RPC 服务器实现
 */
public class VertxHttpServer implements RpcServer {

    // 创建 Vertx 核心实例（事件驱动模型）
    private static final Vertx vertx = Vertx.vertx();

    // 创建 Vert.x 提供的 WebClient，用于发送 HTTP 请求
    private static final WebClient client = WebClient.create(vertx);

    // 获取序列化器（例如：Kryo、Hessian、JSON）
    private final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());

    /**
     * 启动 Vert.x HTTP 服务
     * @param port 监听端口
     * @throws IOException 启动失败时抛出
     */
    @Override
    public void start(int port) throws IOException {
        // 创建 HTTP 服务器
        io.vertx.core.http.HttpServer server = vertx.createHttpServer();

        // 注册请求处理器
        server.requestHandler(request -> {
            // 异步读取请求体
            request.bodyHandler(body -> {
                try {
                    // 将请求体反序列化为 RpcRequest 对象
                    RpcRequest rpcRequest = serializer.deserialize(body.getBytes(), RpcRequest.class);

                    RpcResponse rpcResponse = new RpcResponse();

                    // 通过反射调用本地注册中心中服务实现类的方法 TODO:后面改成从注册中心取
                    Class<?> implClass = LocalRegistry.get(rpcRequest.getServiceName());
                    Method method = implClass.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
                    Object result = method.invoke(implClass.getDeclaredConstructor().newInstance(), rpcRequest.getParameters());

                    // 设置调用结果
                    rpcResponse.setResult(result);
                    byte[] responseBytes = serializer.serialize(rpcResponse);

                    // 返回序列化后的结果给客户端
                    request.response()
                            .putHeader("content-type", "application/octet-stream")
                            .end(Buffer.buffer(responseBytes));

                } catch (Exception e) {
                    // 处理异常并返回 500 状态码
                    request.response().setStatusCode(500).end("Server Error: " + e.getMessage());
                }
            });
        });

        // 启动监听指定端口
        server.listen(port, result -> {
            if (result.succeeded()) {
                System.out.println("Server is now listening on port " + port);
            } else {
                System.err.println("Failed to start server: " + result.cause());
            }
        });
    }

    /**
     * 使用 Vert.x WebClient 发送异步 HTTP POST 请求（用于客户端远程调用）
     * @param url 请求地址（如：http://localhost:8080/）
     * @param contentBytes 请求体内容（序列化后的 RpcRequest）
     * @return 响应体的字节数组
     * @throws Exception 网络或服务端异常
     */
    @Override
    public byte[] sendPost(String url, byte[] contentBytes) throws Exception {
        // 解析 URL
        java.net.URL parsedUrl = new java.net.URL(url);
        String host = parsedUrl.getHost();
        int port = parsedUrl.getPort() == -1 ? 80 : parsedUrl.getPort(); // 默认使用80端口
        String path = parsedUrl.getPath().isEmpty() ? "/" : parsedUrl.getPath();

        // 使用 CompletableFuture 封装异步请求
        CompletableFuture<byte[]> future = new CompletableFuture<>();

        // 发送 POST 请求
        client.post(port, host, path)
                .putHeader("content-type", "application/octet-stream")
                .timeout(5000) // 设置请求超时
                .sendBuffer(Buffer.buffer(contentBytes), ar -> {
                    if (ar.succeeded()) {
                        // 成功接收响应，获取响应体字节数组
                        future.complete(ar.result().body().getBytes());
                    } else {
                        // 异常处理
                        future.completeExceptionally(ar.cause());
                    }
                });

        // 等待响应（最多 5 秒）
        return future.get(5, java.util.concurrent.TimeUnit.SECONDS);
    }
}
