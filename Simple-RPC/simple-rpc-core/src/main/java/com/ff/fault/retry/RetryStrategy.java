package com.ff.fault.retry;

import com.ff.model.RpcResponse;

import java.util.concurrent.Callable;

// 重试策略接口
public interface RetryStrategy {
    // 重试
    RpcResponse retry(Callable<RpcResponse> callable) throws Exception;
}
