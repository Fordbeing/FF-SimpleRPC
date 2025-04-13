package com.ff.serializer;

import java.io.*;

public class JdkSerializer implements Serialize {
    @Override
    public <T> byte[] serialize(T object) throws IOException {
        // 序列化
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = new ObjectOutputStream(outputStream);
        objectOutput.writeObject(object);
        objectOutput.close();
        return outputStream.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> classType) throws IOException {
        // 反序列化
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

        try {
            return (T) objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            objectInputStream.close();
        }

    }
}
