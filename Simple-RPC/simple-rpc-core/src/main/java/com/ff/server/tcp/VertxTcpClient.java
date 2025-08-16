package com.ff.server.tcp;

import cn.hutool.core.util.IdUtil;
import com.ff.RpcApplication;
import com.ff.model.RpcRequest;
import com.ff.model.RpcResponse;
import com.ff.model.ServiceMetaInfo;
import com.ff.protocol.*;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Vertx TCP 请求客户端（支持多请求并发安全、requestId 校验、超时处理）
 */
@Slf4j
public class VertxTcpClient {

    private static final Vertx vertx = Vertx.vertx();
    private static final NetClient netClient = vertx.createNetClient();

    // 全局 map 保存 requestId -> CompletableFuture
    private static final Map<Long, CompletableFuture<RpcResponse>> pendingRequests = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * 发送请求
     *
     * @param rpcRequest
     * @param serviceMetaInfo
     * @param timeoutSeconds 超时时间（秒）
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public static RpcResponse sendPost(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo, long timeoutSeconds)
            throws InterruptedException, ExecutionException, TimeoutException {

        CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();

        netClient.connect(serviceMetaInfo.getServicePort(), serviceMetaInfo.getServiceHost(),
                result -> {
                    if (!result.succeeded()) {
                        responseFuture.completeExceptionally(new RuntimeException("Failed to connect to TCP server"));
                        return;
                    }
                    NetSocket socket = result.result();

                    // 构造请求消息
                    ProtocolMessage<RpcRequest> protocolMessage = new ProtocolMessage<>();
                    ProtocolMessage.Header header = new ProtocolMessage.Header();
                    header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
                    header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
                    header.setSerializer((byte) ProtocolMessageSerializerEnum
                            .getEnumByValue(RpcApplication.getRpcConfig().getSerializer()).getKey());
                    header.setType((byte) ProtocolMessageTypeEnum.REQUEST.getType());

                    // 生成全局 requestId
                    long requestId = IdUtil.getSnowflakeNextId();
                    header.setRequestId(requestId);
                    protocolMessage.setHeader(header);
                    protocolMessage.setBody(rpcRequest);

                    // 将 requestId 与 future 关联
                    pendingRequests.put(requestId, responseFuture);

                    // 超时处理
                    scheduler.schedule(() -> {
                        CompletableFuture<RpcResponse> removed = pendingRequests.remove(requestId);
                        if (removed != null) {
                            removed.completeExceptionally(new TimeoutException("请求超时: " + requestId));
                        }
                    }, timeoutSeconds, TimeUnit.SECONDS);

                    // 编码并发送请求
                    try {
                        Buffer encodeBuffer = ProtocolMessageEncoder.encode(protocolMessage);
                        socket.write(encodeBuffer);
                    } catch (Exception e) {
                        responseFuture.completeExceptionally(new RuntimeException("协议消息编码错误", e));
                        pendingRequests.remove(requestId);
                    }

                    // 处理响应
                    socket.handler(buffer -> {
                        try {
                            ProtocolMessage<RpcResponse> rpcResponseProtocolMessage =
                                    (ProtocolMessage<RpcResponse>) ProtocolMessageDecoder.decode(buffer);
                            long respId = rpcResponseProtocolMessage.getHeader().getRequestId();
                            CompletableFuture<RpcResponse> future = pendingRequests.remove(respId);
                            if (future != null) {
                                future.complete(rpcResponseProtocolMessage.getBody());
                            } else {
                                log.warn("收到未知或超时的响应 requestId={}", respId);
                            }
                        } catch (IOException e) {
                            log.error("协议消息解码错误", e);
                        }
                    });
                });

        return responseFuture.get(timeoutSeconds + 1, TimeUnit.SECONDS); // 阻塞等待
    }
}
