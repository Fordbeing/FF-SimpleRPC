package com.ff.fault.retry;

import com.ff.model.RpcResponse;
import com.github.rholder.retry.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

// 固定时间间隔重试
@Slf4j
public class FixedIntervalRetryStrategy implements RetryStrategy {

    // 重试，采用guava的retryer实现
    @Override
    public RpcResponse retry(Callable<RpcResponse> callable) throws Exception {
        Retryer<RpcResponse> retryer = RetryerBuilder.<RpcResponse>newBuilder()
                .retryIfExceptionOfType(Exception.class) // 重试类型
                .withWaitStrategy(WaitStrategies.fixedWait(3L, TimeUnit.SECONDS)) // 间隔几秒进行重试
                .withStopStrategy(StopStrategies.stopAfterAttempt(3)) // 最大重试次数
                .withRetryListener(new RetryListener() { // 监听重试
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        log.error("当前重试次数为：{}", attempt.getAttemptNumber());
                    }
                }).build();
        return retryer.call(callable);
    }
}
