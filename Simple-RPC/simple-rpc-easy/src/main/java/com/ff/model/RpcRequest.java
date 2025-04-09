package com.ff.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RpcRequest implements Serializable {
    // RPC 远程调用请求
    private String serviceName;             // 接口名称
    private String methodName;              // 方法名
    private Class<?>[] parameterTypes;      // 方法参数类型
    private Object[] parameters;            // 方法参数值
}
