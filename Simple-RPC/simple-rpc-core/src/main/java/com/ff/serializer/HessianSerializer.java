package com.ff.serializer;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 基于 Hessian 的序列化实现类
 * Hessian 是一种轻量级、跨语言的二进制序列化协议，适合用于远程通信（RPC）场景。
 */
public class HessianSerializer implements Serializer {

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
        // 创建一个字节输出流，用于存储序列化后的数据
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // 创建 HessianOutput，用于将对象写入输出流
        HessianOutput hos = new HessianOutput(bos);
        // 将对象序列化到输出流中
        hos.writeObject(object);
        // 返回字节数组
        return bos.toByteArray();
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
        // 创建一个字节输入流，用于读取序列化数据
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        // 创建 HessianInput，用于从输入流中读取对象
        HessianInput his = new HessianInput(bis);
        // 读取对象并强制转换为指定类型
        return (T) his.readObject();
    }
}
