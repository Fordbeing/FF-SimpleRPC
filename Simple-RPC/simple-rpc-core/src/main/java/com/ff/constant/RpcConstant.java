package com.ff.constant;

/* RPC 框架常量
 * 这样就能读取到 rpc.name 等配置
 */
public interface RpcConstant {
    /*
     * 配置文件加载前缀
     */
    String DEFAULT_CONFIG_PREFIX = "rpc";

    /*
     * 默认服务版本
     */
    String DEFAULT_SERVICE_VERSION = "1.0";
}
