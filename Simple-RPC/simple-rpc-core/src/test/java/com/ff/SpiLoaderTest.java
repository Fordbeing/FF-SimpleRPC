package com.ff;

import com.ff.serializer.Serializer;
import com.ff.serializer.SerializerKeys;
import com.ff.spi.SpiLoader;
import org.junit.Test;

import java.io.IOException;

import static com.ff.spi.SpiLoader.*;

public class SpiLoaderTest {
    @Test
    public void testLoad() {
        // 加载所有 SPI 配置
        loadAll();
        // 获取 Serializer 接口的实例，key 为 "jdk"
        Serializer serializer = getInstance(Serializer.class, SerializerKeys.JDK);
        System.out.println(serializer); // 打印实例
    }
}
