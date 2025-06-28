package com.ff.registry;

import com.ff.config.RegistryConfig;
import com.ff.model.ServiceMetaInfo;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 注册中心接口，用于服务的注册、发现和管理
 */
public interface Registry {

    /**
     * 初始化注册中心
     *
     * @param registryConfig 注册中心配置对象，包含地址、端口、类型等初始化信息
     */
    void init(RegistryConfig registryConfig);

    /**
     * 服务注册
     *
     * @param serviceMetaInfo 服务元数据信息，包括服务名称、地址、端口等
     *                        该方法用于将服务提供者的信息注册到注册中心，供消费者发现
     */
    void registry(ServiceMetaInfo serviceMetaInfo) throws Exception;

    /**
     * 服务发现（客户端）
     *
     * @param serviceKey 服务唯一标识（如服务名+版本号）
     * @return 返回匹配该服务标识的所有服务提供者的元数据列表
     * 该方法供服务消费者调用，用于从注册中心获取可用的服务实例
     */
    List<ServiceMetaInfo> getServices(String serviceKey);

    /**
     * 服务下线
     *
     * @param serviceMetaInfo 要下线的服务元数据
     *                        该方法用于将某个服务实例从注册中心移除（例如服务关闭或异常）
     */
    void unRegistry(ServiceMetaInfo serviceMetaInfo);

    /**
     * 销毁注册中心资源
     * 例如关闭连接、释放线程池等资源，应用关闭时调用
     */
    void destroy();

    /*
     * 注册中心心跳检测
     */
    void heartbeat();


    // 缓存监听
    void watch(String serviceNodeKey);
}
