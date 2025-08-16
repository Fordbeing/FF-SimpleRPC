package com.ff.fault.tolerant;

import com.ff.model.RpcResponse;

import java.util.Map;

// 服务容错
public interface TolerantStrategy {

    RpcResponse tolerant(Map<String, Object> context, Exception e);
}
