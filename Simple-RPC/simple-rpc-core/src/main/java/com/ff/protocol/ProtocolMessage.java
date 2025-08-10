package com.ff.protocol;

/*
   自定义Rpc协议消息结构
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProtocolMessage<T> {
    // 消息头
    private Header header;
    // 消息体
    private T body;

    // 消息头内部类
    @Data
    public static class Header {
        // 魔数 1B
        private byte magic;
        // 版本号 1B
        private byte version;
        // 消息类型(RpcRequest/RpcResponse) 1B
        private byte type;
        // 序列化方式 1B
        private byte serializer;
        // 状态码 1B
        private byte status;
        // 请求ID 8B
        private long requestId;
        // bodyLength 4B
        private int bodyLength;
    }
}
