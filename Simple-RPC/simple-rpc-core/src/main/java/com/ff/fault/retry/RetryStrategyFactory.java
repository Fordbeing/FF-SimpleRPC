package com.ff.fault.retry;

import com.ff.spi.SpiLoader;

public class RetryStrategyFactory {
    static {
        SpiLoader.load(RetryStrategy.class);
    }

    // 默认重试策略
    private static final RetryStrategy DEFAULT_RETRY_STRATEGY = new NoRetryStrategy();

    // 获取重试策略实例
    public static RetryStrategy getInstance(String key){
        return SpiLoader.getInstance(RetryStrategy.class, key);
    }
}
