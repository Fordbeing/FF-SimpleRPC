package com.ff.fault.retry;

import com.ff.model.RpcResponse;

import java.util.concurrent.Callable;

// 不重试策略
public class NoRetryStrategy implements RetryStrategy{

    // 重试方法
    @Override
    public RpcResponse retry(Callable<RpcResponse> callable) throws Exception {
        return callable.call();
    }
}
