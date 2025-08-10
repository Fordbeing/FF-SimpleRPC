package com.ff.protocol;

import com.ff.model.RpcRequest;
import com.ff.model.RpcResponse;
import com.ff.serializer.Serializer;
import com.ff.serializer.SerializerFactory;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;

// 解码器
public class ProtocolMessageDecoder {
    // 解码
    public static ProtocolMessage<?> decode(Buffer buffer) throws IOException {
        // 从指定位置读取出 buffer
        ProtocolMessage.Header header = new ProtocolMessage.Header();
        // 取出魔数
        byte magic = buffer.getByte(0);
        // 校验魔数
        if (magic != ProtocolConstant.PROTOCOL_MAGIC) {
            throw new RuntimeException("message attribute 'magic' is error, please check it and try again! ");
        }
        header.setMagic(magic);
        header.setVersion(buffer.getByte(1));
        header.setSerializer(buffer.getByte(2));
        header.setType(buffer.getByte(3));
        header.setStatus(buffer.getByte(4));
        header.setRequestId(buffer.getByte(5));
        header.setBodyLength(buffer.getByte(13));

        // 解决粘包、半包问题
        byte[] bodyBytes = buffer.getBytes(ProtocolConstant.MESSAGE_HEADER_LENGTH, ProtocolConstant.MESSAGE_HEADER_LENGTH + header.getBodyLength());
        // 解析系列化之后的消息体
        ProtocolMessageSerializerEnum serializerEnum = ProtocolMessageSerializerEnum.getEnumByKey(header.getSerializer());
        if (serializerEnum == null) {
            throw new RuntimeException("获取序列化失败，不支持该序列化机制");
        }
        Serializer serializer = SerializerFactory.getInstance(serializerEnum.getValue());
        // 查看消息返回类型
        byte type = header.getType();
        ProtocolMessageTypeEnum typeEnum = ProtocolMessageTypeEnum.getEnumByType(type);

        if (typeEnum == null) {
            throw new RuntimeException("序列化消息的类型不存在");
        }

        // switch 新版本使用方法，第一次使用嘿嘿
        return switch (typeEnum) {
            case REQUEST -> {
                RpcRequest request = serializer.deserialize(bodyBytes, RpcRequest.class);
                yield new ProtocolMessage<>(header, request);
            }
            case RESPONSE -> {
                RpcResponse rpcResponse = serializer.deserialize(bodyBytes, RpcResponse.class);
                yield new ProtocolMessage<>(header, rpcResponse);
            }
            default -> throw new RuntimeException("暂时不支持该消息类型");
        };

    }
}
