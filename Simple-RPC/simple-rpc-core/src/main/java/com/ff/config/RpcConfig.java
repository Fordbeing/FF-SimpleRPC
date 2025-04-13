package com.ff.config;

import lombok.Data;

/*
 * RPC 框架全局配置
 * RPC 框架功能配置包括有以下几个部分
 * 一个常见的 RPC 框架需要有：
 * 服务注册中心地址、负载均衡、服务接口、网络通信协议、超时设置、服务端线程模型

 */
@Data
public class RpcConfig {

    // 默认的配置

    // 名称
    private String name = "ff-rpc";

    // 版本号
    private String version = "1.0";

    // 服务器主机名
    private String serverHost = "localhost";

    // 服务器端口号
    private Integer serverPort = 8080;


}
