package com.ff.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class RpcResponse implements Serializable {
    // RPC 远程调用返回

    private Object result; // 返回结果
    private Exception exception; // 返回异常
    private Class<?> parameterType;      // 方法参数类型
    private String message; // 相应信息
}
