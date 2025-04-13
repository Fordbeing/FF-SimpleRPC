package com.ff;

import com.ff.config.RpcConfig;
import com.ff.constant.RpcConstant;
import com.ff.utils.ConfigUtils;
import lombok.extern.slf4j.Slf4j;

/*
    集中存储和管理全局配置，
    确保 RPC 框架的配置只被初始化一次，
    并且在整个项目生命周期内都能被安全访问。
 */

@Slf4j
public class RpcApplication {
    // 静态变量，保存全局配置
    private static volatile RpcConfig rpcConfig;

    // 方法一
    // 手动初始化框架，传入自定义配置
    public static void init(RpcConfig newRpcConfig) {
        rpcConfig = newRpcConfig;
        log.info("rpc init, config = {}", newRpcConfig.toString());
    }

    // 方法二
    // 自动初始化配置，从配置文件中加载
    public static void init() {
        RpcConfig newRpcConfig;
        try {
            // 读取配置文件
            newRpcConfig = ConfigUtils.loadConfig(RpcConfig.class, RpcConstant.DEFAULT_CONFIG_PREFIX);
        } catch (Exception e) {
            // 默认配置属性
            newRpcConfig = new RpcConfig();
        }
        init(newRpcConfig);
    }

    // 懒加载 + 双检锁获取配置
    public static RpcConfig getRpcConfig() {
        if (rpcConfig == null) {
            synchronized (RpcApplication.class) {
                if (rpcConfig == null) {
                    init();
                }
            }
        }

        return rpcConfig;
    }
}
