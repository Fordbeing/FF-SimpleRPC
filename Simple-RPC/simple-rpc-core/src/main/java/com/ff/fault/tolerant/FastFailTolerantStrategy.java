package com.ff.fault.tolerant;

import com.ff.model.RpcResponse;

import java.util.Map;

// 快速失败容错机制
public class FastFailTolerantStrategy implements TolerantStrategy {
    @Override
    public RpcResponse tolerant(Map<String, Object> context, Exception e) {
        throw new RuntimeException("服务出错，请检查！");
    }
}
