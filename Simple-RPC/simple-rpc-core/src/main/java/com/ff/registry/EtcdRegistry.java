package com.ff.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.json.JSONUtil;
import com.ff.config.RegistryConfig;
import com.ff.model.ServiceMetaInfo;
import io.etcd.jetcd.*;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.support.CloseableClient;
import io.etcd.jetcd.watch.WatchEvent;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 基于 etcd 实现的服务注册中心
 */
@Slf4j
public class EtcdRegistry implements Registry {

    /**
     * 本机注册的节点 key 集合（用于维护续期）
     */
    private final Set<String> localRegistryNodeKeySet = new HashSet<>();

    /**
     * 注册中心服务缓存
     */
    private final RegistryServiceCache registryServiceCache = new RegistryServiceCache();

    /**
     * 正在监听的key集合
     */
    private final Set<String> watchingRegistryNodeKeySet = new ConcurrentHashSet<>();


    // etcd 客户端
    private Client client;

    // etcd KV 操作客户端
    private KV kvClient;

    private CloseableClient keepAliveClient;

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
        try {
            client = Client.builder()
                    .endpoints(registryConfig.getAddress()) // 设置 etcd 地址
                    .connectTimeout(Duration.ofMillis(registryConfig.getTimeout())) // 设置连接超时
                    .build();
            kvClient = client.getKVClient(); // 获取 KV 操作客户端
            // 自动续约，心跳检测
            heartbeat();
        } catch (Exception e) {
            log.error("init etcd client error", e);
            throw new RuntimeException("init etcd client error", e);
        }
    }

    @Override
    public void registry(ServiceMetaInfo serviceMetaInfo) throws ExecutionException, InterruptedException, TimeoutException {
        Lease leaseClient = client.getLeaseClient();
        // 创建一个 30 秒的租约
        long leaseId = leaseClient.grant(30).get().getID();

        // 拼接 etcd 中的服务键名
        String registryKey = ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
        ByteSequence key = ByteSequence.from(registryKey, StandardCharsets.UTF_8);

        // 将服务信息序列化为 JSON 格式
        ByteSequence value = ByteSequence.from(JSONUtil.toJsonStr(serviceMetaInfo), StandardCharsets.UTF_8);

        // 使用租约写入 etcd，使服务注册信息具有过期时间
        PutOption putOption = PutOption.builder().withLeaseId(leaseId).build();
        kvClient.put(key, value, putOption).get();

        // 启动自动续租
        leaseClient.keepAlive(leaseId, new StreamObserver<LeaseKeepAliveResponse>() {
            @Override
            public void onNext(LeaseKeepAliveResponse response) {
                log.debug("KeepAlive 成功, leaseId={}, TTL={}",
                        response.getID(), response.getTTL());
            }

            @Override
            public void onError(Throwable t) {
                log.error("KeepAlive 出错, leaseId={}", leaseId, t);
            }

            @Override
            public void onCompleted() {
                log.info("KeepAlive 完成, leaseId={}", leaseId);
            }
        });

        // 注册成功后，把当前节点加入本地集合
        localRegistryNodeKeySet.add(registryKey);
    }


    /**
     * 根据服务 key 获取该服务下的所有服务实例信息
     *
     * @param serviceKey 服务名称
     * @return 服务实例列表
     */
    @Override
    public List<ServiceMetaInfo> getServices(String serviceKey) {
        // 如果缓存里面有，直接返回
        List<ServiceMetaInfo> serviceMetaInfos = registryServiceCache.readCache(serviceKey);
        if (CollUtil.isNotEmpty(serviceMetaInfos)) {
            return serviceMetaInfos;
        }

        // 拼接前缀路径，注意以 '/' 结尾用于前缀匹配 /rpc/com.ff.common.service.UserService:1.0/localhost:8081
        String searchPrefix = ETCD_ROOT_PATH + serviceKey + "/";

        try {
            // 设置前缀查询选项
            GetOption getOption = GetOption.builder().isPrefix(true).build();
            List<KeyValue> keyValues = kvClient.get(
                            ByteSequence.from(searchPrefix, StandardCharsets.UTF_8),
                            getOption)
                    .get()
                    .getKvs();

            // 将查询结果反序列化为服务信息对象
            List<ServiceMetaInfo> serviceMetaInfoList = keyValues.stream()
                    .map(keyValue -> {
                        String key = keyValue.getKey().toString(StandardCharsets.UTF_8);
                        watch(key);
                        String value = keyValue.getValue().toString(StandardCharsets.UTF_8);
                        return JSONUtil.toBean(value, ServiceMetaInfo.class);
                    })
                    .collect(Collectors.toList());
            // 写入注册中心本地缓存
            registryServiceCache.writeCache(serviceKey, serviceMetaInfoList);
            return serviceMetaInfoList;
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
        String registryKey = ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
        kvClient.delete(ByteSequence.from(
                registryKey,
                StandardCharsets.UTF_8));
        localRegistryNodeKeySet.remove(registryKey);
    }

    @Override
    public void destroy() {
        if (keepAliveClient != null) {
            keepAliveClient.close();
        }
        if (kvClient != null) {
            kvClient.close();
        }
        if (client != null) {
            client.close();
        }
    }


    @Override
    public void heartbeat() {
        // 兜底检测
        CronUtil.schedule("*/30 * * * * *", new Task() {
            @Override
            public void execute() {
                for (String registryKey : localRegistryNodeKeySet) {
                    try {
                        List<KeyValue> keyValues = kvClient.get(
                                ByteSequence.from(registryKey, StandardCharsets.UTF_8)).get().getKvs();
                        if (CollUtil.isEmpty(keyValues)) {
                            log.error("检测到 {} 已过期，需要重新注册！", registryKey);
                            // TODO: 可以触发告警 或 自动尝试重新注册
                        }
                    } catch (Exception e) {
                        log.error("心跳检测异常，key:{}", registryKey, e);
                    }
                }
            }
        });

        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }


    @Override
    public void watch(String serviceNodeKey) {
        Watch watchClient = client.getWatchClient();
        // 之前没有被监听，开启监听
        boolean newWatch = watchingRegistryNodeKeySet.add(serviceNodeKey);
        if (newWatch) {
            watchClient.watch(ByteSequence.from(serviceNodeKey, StandardCharsets.UTF_8), watchResponse -> {
                // key 删除时触发
                for (WatchEvent event : watchResponse.getEvents()) {
                    switch (event.getEventType()) {
                        case DELETE:
                            // 清空缓存
                            registryServiceCache.clearCacheByKey(serviceNodeKey);
                            break;
                        case PUT:
                        default:
                            break;
                    }
                }
            });
        }
    }
}
