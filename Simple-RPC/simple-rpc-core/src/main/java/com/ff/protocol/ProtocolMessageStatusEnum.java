package com.ff.protocol;

import lombok.Getter;

// 状态码枚举
@Getter
public enum ProtocolMessageStatusEnum {
    OK("ok", 200),
    BAD_REQUEST("badRequest", 400),
    BAD_RESPONSE("badRequest", 500);

    private final String text;

    private final int status;

    ProtocolMessageStatusEnum(String text, int status) {
        this.text = text;
        this.status = status;
    }


    // 根据状态码获取枚举
    public static ProtocolMessageStatusEnum getEnumByStatus(int status) {
        for (ProtocolMessageStatusEnum statusEnum : ProtocolMessageStatusEnum.values()) {
            if (statusEnum.status == status) {
                return statusEnum;
            }
        }
        return null;
    }
}
