package com.ff.serializer;

import com.ff.spi.SpiLoader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化器工厂类
 * 负责根据不同的key返回对应的序列化器实例
 */
public class SerializerFactory {

    static {
        SpiLoader.load(Serializer.class);
    }

    // 默认使用的序列化器（当key找不到对应实例时返回）
    private static final Serializer DEFAULT_SERIALIZER = new JdkSerializer();

    /**
     * 根据指定key返回对应的序列化器实例
     *
     * @param key 序列化器标识（如 "jdk"、"hessian"、"kryo"、"json"）
     * @return 对应的Serialize实例，如果找不到则返回默认的JDK序列化器
     */
    public static Serializer getInstance(String key) {
        return SpiLoader.getInstance(Serializer.class, key);
    }
}
