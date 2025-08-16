package com.ff.fault.retry;

// 重试键常量
public interface RetryStrategyKeys {
    // 不重试
    String NO = "no";

    // 固定时间间隔重试
    String FIXED_INTERVAL = "fixedinterval";
}
