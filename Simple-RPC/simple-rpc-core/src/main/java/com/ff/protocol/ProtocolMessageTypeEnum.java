package com.ff.protocol;

import lombok.Getter;

// 协议消息请求类型
@Getter
public enum ProtocolMessageTypeEnum {
    REQUEST(0), // 请求
    RESPONSE(1), // 响应
    HEART_BEAT(2), // 心跳检测
    OTHERS(3); // 其他

    private final int type;

    ProtocolMessageTypeEnum(int type) {
        this.type = type;
    }

    // 根据类型获取枚举
    public static ProtocolMessageTypeEnum getEnumByType(int type) {
        for (ProtocolMessageTypeEnum typeEnum : ProtocolMessageTypeEnum.values()){
            if(typeEnum.type == type){
                return typeEnum;
            }
        }
        return null;
    }
}
