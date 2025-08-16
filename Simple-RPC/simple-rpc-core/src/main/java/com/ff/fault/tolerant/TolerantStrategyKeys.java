package com.ff.fault.tolerant;

// 服务容错常量
public interface TolerantStrategyKeys {
    String FAST_FAIL = "fastfail";

    String FAST_SAFE = "fastsafe";

    String FAST_OVER = "fastover";

    String FAST_BACK = "fastback";
}
