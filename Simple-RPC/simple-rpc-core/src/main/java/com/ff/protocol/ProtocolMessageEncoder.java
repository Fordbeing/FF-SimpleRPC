package com.ff.protocol;

import cn.hutool.core.util.ObjectUtil;
import com.ff.serializer.Serializer;
import com.ff.serializer.SerializerFactory;
import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;

// 编码器，用于对消息进行编码
@Slf4j
public class ProtocolMessageEncoder {
    // 编码
    public static Buffer encode(ProtocolMessage<?> protocolMessage) {
        if (ObjectUtil.isNull(protocolMessage) || ObjectUtil.isNull(protocolMessage.getHeader())) {
            return Buffer.buffer();
        }

        try {
            ProtocolMessage.Header header = protocolMessage.getHeader();
            // 使用 Vert.x 来管理
            Buffer buffer = Buffer.buffer();
            // 写入缓冲区
            buffer.appendByte(header.getMagic());
            buffer.appendByte(header.getVersion());
            buffer.appendByte(header.getSerializer());
            buffer.appendByte(header.getType());
            buffer.appendByte(header.getStatus());
            buffer.appendLong(header.getRequestId());

            ProtocolMessageSerializerEnum enumByKey = ProtocolMessageSerializerEnum.getEnumByKey(header.getSerializer());
            if (enumByKey == null) {
                throw new RuntimeException("获取序列化失败，不支持该序列化机制");
            }
            Serializer serializer = SerializerFactory.getInstance(enumByKey.getValue());
            byte[] bodyBytes = serializer.serialize(protocolMessage.getBody());
            buffer.appendInt(header.getBodyLength());
            buffer.appendBytes(bodyBytes);
            return buffer;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }
}
