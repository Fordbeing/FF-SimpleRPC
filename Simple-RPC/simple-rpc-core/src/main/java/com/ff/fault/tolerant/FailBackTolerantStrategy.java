package com.ff.fault.tolerant;

import com.ff.model.RpcResponse;

import java.util.Map;

// 服务降级处理
public class FailBackTolerantStrategy implements TolerantStrategy{
    @Override
    public RpcResponse tolerant(Map<String, Object> context, Exception e) {
        return null;
    }
}
