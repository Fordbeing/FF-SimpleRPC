package com.ff.serializer;

import java.io.IOException;

public interface Serializer {

    <T> byte[] serialize(T object) throws IOException;  // 序列化

    <T> T deserialize(byte[] bytes,Class<T> classType) throws IOException; // 反序列化
}
