package com.ff.fault.tolerant;

import com.ff.spi.SpiLoader;

// 获取容错机制工厂类
public class TolerantStrategyFactory {
    static {
        SpiLoader.load(TolerantStrategy.class);
    }

    private static final TolerantStrategy tolerantStrategy = new FastFailTolerantStrategy();

    public static TolerantStrategy getInstance(String key) {
        return SpiLoader.getInstance(TolerantStrategy.class, key);
    }
}
