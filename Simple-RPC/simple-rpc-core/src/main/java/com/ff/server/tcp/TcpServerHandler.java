package com.ff.server.tcp;

import com.ff.model.RpcRequest;
import com.ff.model.RpcResponse;
import com.ff.protocol.ProtocolMessage;
import com.ff.protocol.ProtocolMessageDecoder;
import com.ff.protocol.ProtocolMessageEncoder;
import com.ff.protocol.ProtocolMessageTypeEnum;
import com.ff.registry.LocalRegistry;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Method;

@Slf4j
public class TcpServerHandler implements Handler<NetSocket> {
    @Override
    public void handle(NetSocket netSocket) {
        // 处理连接
        netSocket.handler(buffer -> {
            // 接受请求，并且解码
            ProtocolMessage<RpcRequest> protocolMessage;
            try {
                protocolMessage = (ProtocolMessage<RpcRequest>) ProtocolMessageDecoder.decode(buffer);
            } catch (IOException e) {
                throw new RuntimeException("协议解析失败");
            }
            RpcRequest request = protocolMessage.getBody();
            // 构造响应请求
            RpcResponse response = new RpcResponse();
            try {
                // 获取要调用的类
                Class<?> implClass = LocalRegistry.get(request.getServiceName());
                Method method = implClass.getMethod(request.getMethodName(), request.getParameterTypes());
                Object result = method.invoke(implClass, request.getParameters());
                response.setResult(result);
                response.setMessage("success");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                response.setMessage(e.getMessage());
                response.setException(e);
            }

            // 进行编码，发送
            ProtocolMessage.Header header = protocolMessage.getHeader();
            header.setType((byte) ProtocolMessageTypeEnum.RESPONSE.getType());
            ProtocolMessage<RpcResponse> rpcResponseProtocolMessage = new ProtocolMessage<>(header, response);
            try {
                // 对响应体进行编码
                Buffer encode = ProtocolMessageEncoder.encode(rpcResponseProtocolMessage);
                netSocket.write(encode);
            } catch (Exception e) {
                throw new RuntimeException("协议编码解析出错");
            }

        });
    }
}
