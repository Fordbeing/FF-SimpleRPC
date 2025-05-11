package com.ff.registry;

import com.ff.server.RpcHttpServer;
import com.ff.server.RpcServer;
import com.ff.spi.SpiLoader;

public class RegistryFactory {
    static {
        SpiLoader.load(Registry.class);
    }

    // 默认使用的注册中心（当key找不到对应实例时返回）
    private static final Registry DEFAULT_REGISTRY = new EtcdRegistry();

    public static Registry getInstance(String key) {
        Registry instance = SpiLoader.getInstance(Registry.class, key);
        if (instance == null) {
            return DEFAULT_REGISTRY;
        }
        return instance;
    }
}
