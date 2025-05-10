package com.ff.spi;

import cn.hutool.core.io.resource.ResourceUtil;
import com.ff.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * SPI 加载器
 * 自定义实现，支持键值对映射
 * 该类用于加载并实例化 SPI（Service Provider Interface）实现类
 */
@Slf4j
public class SpiLoader {

    /*
     * 存储已加载的类：接口名 =>（key => 实现类）
     * 用于记录接口类型和它对应的多个实现类的映射
     */
    private static final Map<String, Map<String, Class<?>>> loaderMap = new ConcurrentHashMap<>();

    /*
     * 对象实例缓存（避免重复 new），类路径 => 对象实例，单例模式
     * 存储实例化的对象，避免多次实例化同一个类
     */
    private static final Map<String, Object> instanceCache = new ConcurrentHashMap<>();

    /*
     * 系统 SPI 目录
     * 存放系统默认的 SPI 配置
     */
    private static final String RPC_SYSTEM_SPI_DIR = "META-INF/rpc/system/";

    /*
     * 用户自定义 SPI 目录
     * 存放用户自定义的 SPI 配置
     */
    private static final String RPC_CUSTOM_SPI_DIR = "META-INF/rpc/custom/";

    /*
     * 扫描路径
     * 用于指定扫描 SPI 配置文件的路径
     */
    private static final String[] SCAN_DIRS = new String[]{RPC_SYSTEM_SPI_DIR, RPC_CUSTOM_SPI_DIR};

    /*
     * 动态加载的类列表
     * 需要加载 SPI 配置的接口列表
     */
    private static final List<Class<?>> LOAD_CLASS_LIST = Arrays.asList(Serializer.class);

    /*
     * 加载所有类型
     * 遍历所有类并加载对应的 SPI 配置
     */
    public static void loadAll() {
        log.info("加载所有 SPI");
        // 加载 接口类下所有的实现类
        for (Class<?> aClass : LOAD_CLASS_LIST) {
            load(aClass); // 加载每一个类的 SPI 配置
        }
    }

    /**
     * 获取某个接口的实例
     *
     * @param tClass 接口类
     * @param key    对应的实现类的 key
     * @param <T>    泛型
     * @return 对应接口的实现类实例
     */
    public static <T> T getInstance(Class<?> tClass, String key) {
        // 获取接口的类名
        String tClassName = tClass.getName();
        Map<String, Class<?>> keyClassMap = loaderMap.get(tClassName);
        if (keyClassMap == null) {
            throw new RuntimeException(String.format("SpiLoader 未加载 %s 类型", tClassName));
        }
        // 检查是否有对应 key 的实现类
        if (!keyClassMap.containsKey(key)) {
            throw new RuntimeException(String.format("SpiLoader 的 %s 不存在 key=%s 的类型", tClassName, key));
        }
        // 获取到要加载的实现类型
        Class<?> implClass = keyClassMap.get(key);
        String implClassName = implClass.getName();
        // 从实例缓存中加载指定类型的实例
        if (!instanceCache.containsKey(implClassName)) {
            try {
                // 使用构造器实例化
                Constructor<?> constructor = implClass.getDeclaredConstructor();
                constructor.setAccessible(true);  // 设置为可访问
                Object instance = constructor.newInstance(); // 实例化对象
                instanceCache.put(implClassName, instance); // 缓存实例
            } catch (Exception e) {
                String errorMsg = String.format("%s 类实例化失败", implClassName);
                throw new RuntimeException(errorMsg, e); // 如果实例化失败，抛出异常
            }
        }
        log.info("实例化类型：{}", implClassName);
        return (T) instanceCache.get(implClassName); // 返回实例
    }

    /**
     * 加载某个类型的 SPI 配置
     * 扫描并加载指定接口类对应的 SPI 配置
     *
     * @param loadClass 要加载的接口类
     * @return 配置文件中的 key-实现类 映射
     * @throws IOException IO 异常
     */
    public static Map<String, Class<?>> load(Class<?> loadClass) {
        log.info("加载类型为 {} 的 SPI", loadClass.getName());
        // 用于存放 loadClass 名称文件下所有的 Class
        Map<String, Class<?>> keyClassMap = new HashMap<>();
        // 遍历扫描路径，加载 SPI 配置
        for (String scanDir : SCAN_DIRS) {
            List<URL> resources = ResourceUtil.getResources(scanDir + loadClass.getName());
            if(resources.isEmpty()){
                log.error("加载资源配置失败，请检查 SPI 配置，路径：{}，文件名：{}",scanDir,loadClass.getName());
                throw new RuntimeException("无法获取配置文件信息");
            }
            // 读取每个资源文件
            for (URL resource : resources) {
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resource.openStream()))) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        // 每行格式为 key=className
                        String[] strArray = line.split("=");
                        if (strArray.length == 2) {
                            String key = strArray[0].trim(); // 获取 key
                            String className = strArray[1].trim(); // 获取类名
                            try {
                                // 加载类并放入映射中
                                Class<?> clazz = Class.forName(className);
                                keyClassMap.put(key, clazz); // 存储 key-类映射
                            } catch (ClassNotFoundException e) {
                                log.error("SPI 配置文件中指定的类未找到: {}", className, e); // 类加载失败时记录错误
                            }
                        } else {
                            log.warn("SPI 配置文件格式不正确: {}", line); // 配置格式不正确时警告
                        }
                    }
                } catch (IOException e) {
                    log.error("读取 SPI 配置文件时发生错误: {}", resource, e); // 文件读取出错时记录错误
                }
            }
        }
        // 将加载的配置存入 loaderMap
        loaderMap.put(loadClass.getName(), keyClassMap);
        return keyClassMap; // 返回映射
    }

}
