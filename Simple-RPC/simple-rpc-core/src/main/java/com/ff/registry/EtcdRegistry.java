package com.ff.registry;

import cn.hutool.json.JSONUtil;
import com.ff.config.RegistryConfig;
import com.ff.model.ServiceMetaInfo;
import io.etcd.jetcd.*;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * 基于 etcd 实现的服务注册中心
 */
@Slf4j
public class EtcdRegistry implements Registry {

    // etcd 客户端
    private Client client;

    // etcd KV 操作客户端
    private KV kvClient;

    /**
     * etcd 中所有服务注册的根路径
     */
    private static final String ETCD_ROOT_PATH = "/rpc/";

    /**
     * 初始化注册中心，创建 etcd 客户端
     *
     * @param registryConfig 注册中心配置，包括地址、超时时间等
     */
    @Override
    public void init(RegistryConfig registryConfig) {
        client = Client.builder()
                .endpoints(registryConfig.getAddress()) // 设置 etcd 地址
                .connectTimeout(Duration.ofMillis(registryConfig.getTimeout())) // 设置连接超时
                .build();
        kvClient = client.getKVClient(); // 获取 KV 操作客户端
    }

    /**
     * 注册服务到 etcd
     *
     * @param serviceMetaInfo 服务的元信息
     */
    @Override
    public void registry(ServiceMetaInfo serviceMetaInfo) throws ExecutionException, InterruptedException {
        Lease leaseClient = client.getLeaseClient();
        // 创建一个30秒的租约，用于实现服务临时性，防止宕机后节点长期存在
        long leaseId = leaseClient.grant(30).get().getID();

        // 拼接 etcd 中的服务键名 - key
        String registryKey = ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
        ByteSequence key = ByteSequence.from(registryKey, StandardCharsets.UTF_8);

        // 将服务信息序列化为 JSON 格式 - value
        ByteSequence value = ByteSequence.from(JSONUtil.toJsonStr(serviceMetaInfo), StandardCharsets.UTF_8);

        // 使用租约写入 etcd，使服务注册信息具有过期时间
        PutOption putOption = PutOption.builder().withLeaseId(leaseId).build();
        kvClient.put(key, value, putOption).get();
    }

    /**
     * 根据服务 key 获取该服务下的所有服务实例信息
     *
     * @param serviceKey 服务名称
     * @return 服务实例列表
     */
    @Override
    public List<ServiceMetaInfo> getServices(String serviceKey) {
        // 拼接前缀路径，注意以 '/' 结尾用于前缀匹配
        String searchPrefix = ETCD_ROOT_PATH + serviceKey;

        try {
            // 设置前缀查询选项
            GetOption getOption = GetOption.builder().isPrefix(true).build();
            List<KeyValue> keyValues = kvClient.get(
                            ByteSequence.from(searchPrefix, StandardCharsets.UTF_8),
                            getOption)
                    .get()
                    .getKvs();

            // 将查询结果反序列化为服务信息对象
            return keyValues.stream()
                    .map(keyValue -> {
                        String value = keyValue.getValue().toString(StandardCharsets.UTF_8);
                        return JSONUtil.toBean(value, ServiceMetaInfo.class);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("获取服务列表失败", e);
        }
    }

    /**
     * 服务下线，从 etcd 中删除服务实例信息
     *
     * @param serviceMetaInfo 服务元信息
     */
    @Override
    public void unRegistry(ServiceMetaInfo serviceMetaInfo) {
        kvClient.delete(ByteSequence.from(
                ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey(),
                StandardCharsets.UTF_8));
    }

    /**
     * 销毁注册中心，关闭连接
     */
    @Override
    public void destroy() {
        log.info("注册中心销毁！");
        if (kvClient != null) {
            kvClient.close(); // 关闭 KV 客户端
        }
        if (client != null) {
            client.close(); // 关闭 etcd 客户端
        }
    }
}
