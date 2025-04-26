package com.ff.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/*
 * 基于 Kryo 的序列化实现类
 * Kryo 是一种高性能的 Java 序列化框架，但它本身线程不安全，因此通过 ThreadLocal 保证每个线程一个实例。
 * 实际上就是将数据进行序列化存放到 ThreadLocal 中，反序列化的时候再通过 ThreadLocal中进行读取并且反序列化
 */
public class KryoSerializer implements Serializer {

    /**
     * 使用 ThreadLocal 保存 Kryo 实例，确保每个线程拥有独立的 Kryo 对象，避免线程安全问题
     */
    private static final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        // 允许动态注册类，不需要提前注册所有待序列化的类
        kryo.setRegistrationRequired(false);
        // 设置类加载器，避免某些环境下（如应用服务器）类加载冲突
        kryo.setClassLoader(Thread.currentThread().getContextClassLoader());
        return kryo;
    });

    /**
     * 将对象序列化为字节数组
     *
     * @param object  需要序列化的对象
     * @param <T>     对象的泛型类型
     * @return        序列化后的字节数组
     * @throws IOException IO异常
     */
    @Override
    public <T> byte[] serialize(T object) throws IOException {
        if (object == null) {
            throw new IllegalArgumentException("Object to serialize cannot be null.");
        }

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             Output output = new Output(byteArrayOutputStream)) {

            // 从 ThreadLocal 中获取 Kryo 实例，进行对象写入
            kryoThreadLocal.get().writeObject(output, object);
            output.flush(); // 确保数据完全写入输出流
            return byteArrayOutputStream.toByteArray();
        }
    }

    /**
     * 将字节数组反序列化为对象
     *
     * @param bytes     字节数组
     * @param classType 目标对象的 Class 类型
     * @param <T>       对象的泛型类型
     * @return          反序列化后的对象
     * @throws IOException IO异常
     */
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> classType) throws IOException {
        // 防御性编程，防止传入空参数
        if (bytes == null || classType == null) {
            throw new IllegalArgumentException("Bytes and classType cannot be null.");
        }

        // 使用 try-with-resources 自动关闭流资源
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             Input input = new Input(byteArrayInputStream)) {

            // 从 ThreadLocal 中获取 Kryo 实例，进行对象读取
            return kryoThreadLocal.get().readObject(input, classType);
        }
    }
}
