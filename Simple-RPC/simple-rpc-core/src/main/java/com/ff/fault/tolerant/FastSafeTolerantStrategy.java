package com.ff.fault.tolerant;

import com.ff.model.RpcResponse;

import java.util.Map;

// 静默处理，不处理
public class FastSafeTolerantStrategy implements TolerantStrategy{
    @Override
    public RpcResponse tolerant(Map<String, Object> context, Exception e) {
        return new RpcResponse();
    }
}
