package com.ff.server;

import com.ff.RpcApplication;
import com.ff.model.RpcRequest;
import com.ff.model.RpcResponse;
import com.ff.registry.LocalRegistry;
import com.ff.serializer.Serializer;
import com.ff.serializer.SerializerFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 基于 Netty 的 HTTP 协议 RPC 服务端实现
 */
public class NettyHttpServer implements RpcServer {

    // 获取序列化器（根据配置支持不同格式，如Kryo、Hessian等）
    private final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());

    /**
     * 启动 Netty HTTP 服务器
     * @param port 监听端口
     * @throws IOException 启动异常
     */
    @Override
    public void start(int port) throws IOException {
        // bossGroup 接收连接，workerGroup 处理连接的读写
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // 使用 NIO 的 ServerSocketChannel
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            // 编解码 HTTP 请求和响应
                            p.addLast(new HttpServerCodec());
                            // 聚合 HTTP 消息成为 FullHttpRequest（完整请求体）
                            p.addLast(new HttpObjectAggregator(65536));
                            // 自定义处理逻辑
                            p.addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
                                    System.out.println("Received request: " + request.method() + " " + request.uri());

                                    try {
                                        // 读取请求体为字节数组
                                        byte[] requestData = new byte[request.content().readableBytes()];
                                        request.content().readBytes(requestData);

                                        // 反序列化为 RpcRequest 对象
                                        RpcRequest rpcRequest = serializer.deserialize(requestData, RpcRequest.class);

                                        // 从本地注册中心获取实现类，并通过反射调用方法
                                        Class<?> implClass = LocalRegistry.get(rpcRequest.getServiceName());
                                        Method method = implClass.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
                                        Object result = method.invoke(implClass.getDeclaredConstructor().newInstance(), rpcRequest.getParameters());

                                        // 构建响应对象
                                        RpcResponse rpcResponse = new RpcResponse();
                                        rpcResponse.setResult(result);

                                        // 序列化响应数据
                                        byte[] responseData = serializer.serialize(rpcResponse);

                                        // 构造 HTTP 响应
                                        FullHttpResponse response = new DefaultFullHttpResponse(
                                                HttpVersion.HTTP_1_1,
                                                HttpResponseStatus.OK,
                                                ctx.alloc().buffer().writeBytes(responseData)
                                        );
                                        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
                                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

                                        // 返回响应
                                        ctx.writeAndFlush(response);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        ctx.close(); // 异常时关闭连接
                                    }
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    cause.printStackTrace();
                                    ctx.close(); // 捕获未处理异常
                                }
                            });
                        }
                    });

            // 启动服务器绑定端口
            ChannelFuture future = bootstrap.bind(port).sync();
            System.out.println("Server is now listening on port " + port);
            // 阻塞等待服务关闭
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new IOException("Server interrupted", e);
        } finally {
            // 优雅关闭资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * 向目标 URL 发送 HTTP POST 请求（用于 RPC 客户端）
     * @param url 请求地址
     * @param contentBytes 请求体（已序列化的 RpcRequest）
     * @return 服务端返回的响应字节数据
     * @throws Exception 网络或服务端异常
     */
    @Override
    public byte[] sendPost(String url, byte[] contentBytes) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL targetUrl = new URL(url);
            connection = (HttpURLConnection) targetUrl.openConnection();

            // 设置 POST 请求属性
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("Content-Length", String.valueOf(contentBytes.length));

            // 写入请求体
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(contentBytes);
                outputStream.flush();
            }

            // 检查响应状态
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("HTTP POST failed with error code: " + responseCode);
            }

            // 读取响应体
            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }

                return baos.toByteArray(); // 返回原始响应字节数据
            }

        } finally {
            if (connection != null) {
                connection.disconnect(); // 关闭连接
            }
        }
    }

}
