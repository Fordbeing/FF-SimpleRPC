# SimpleRPC - 轻量级 RPC 框架  

SimpleRPC 是一个**轻量级、高可扩展的 RPC 框架**，旨在帮助开发者深入理解远程过程调用（RPC）的核心原理，同时提供一个简洁易用的 RPC 解决方案。  

# SimpleRPC 教程：[在线地址](https://www.yuque.com/u39213715/mx5a9f/ybsfoptlo0ecwd7n?singleDoc# 《SimpleRPC 轻量级 RPC 框架教程》)

# RPC 框架文档

## 技术选型

### 后端

本 RPC 框架主要基于 **Java** 开发，所有的设计思想可以迁移到其他语言，代码实现有所不同。

- ⭐ **Netty** 高性能网络通信框架  
- ⭐ **Etcd** 云原生注册中心（jetcd 客户端）  
- ⭐ **SPI 机制**（可扩展组件）  
- ⭐ **Protobuf** 高效序列化协议  
- ⭐ **多种设计模式**  
  - 双检锁单例模式  
  - 工厂模式  
  - 代理模式  
  - 装饰者模式  
- ⭐ **Spring Boot Starter 开发**  
- ⭐ **反射和注解驱动**  
- ⭐ **Guava Retrying** 重试库  
- ⭐ **JUnit** 单元测试  
- ⭐ **Logback** 日志库  
- ⭐ **Hutool、Lombok** 工具库  

## 源码目录

- `rpc-core`：RPC 框架核心代码
- `rpc-easy`：简易版 RPC 框架（适合新手入门）
- `example-common`：示例代码公用模块
- `example-consumer`：示例服务消费者
- `example-provider`：示例服务提供者
- `example-springboot-consumer`：示例服务消费者（Spring Boot 框架）
- `example-springboot-provider`：示例服务提供者（Spring Boot 框架）
- `rpc-spring-boot-starter`：Spring Boot 注解驱动的 RPC 组件

## 项目教程大纲

### 第一章：RPC 框架简易版

1. RPC 基本概念和作用
2. RPC 框架实现思路 | 基本设计
3. RPC 框架实现思路 | 扩展设计
4. 项目初始化
5. Web 服务器搭建
6. 本地服务注册
7. Protobuf 序列化实现
8. 请求处理器实现
9. 消费者代理设计
10. 测试验证

### 第二章：RPC 框架扩展版

#### 配置与序列化

1. 全局配置加载
2. Protobuf 序列化实现
3. SPI 机制集成序列化器
4. 可扩展序列化（SPI + 工厂模式）

#### 注册中心

1. 注册中心核心能力分析
2. Etcd 介绍与技术选型
3. 基于 Etcd 实现服务注册与发现
4. 可扩展注册中心（SPI + 工厂模式）
5. 心跳检测与服务续期
6. 服务节点下线机制
7. 消费端缓存优化（监听 Etcd 变更）

#### 网络通信

1. 自定义 RPC 协议设计（基于 Protobuf）
2. Netty 服务器端实现
3. Netty 客户端实现
4. 编码/解码器设计
5. 粘包半包问题分析及解决方案

#### 负载均衡与容错

1. 负载均衡策略（随机、轮询、一致性哈希）
2. SPI 机制扩展负载均衡
3. 重试机制（等待策略、多种重试策略）
4. 容错机制（熔断、降级、限流）

#### 框架整合与优化

1. 快速启动类
2. Spring Boot Starter 组件开发
3. 注解驱动
4. 性能优化与监控

## 项目扩展思路

- 增加基于 HTTP/gRPC 的支持
- 增强服务治理能力（流量控制、熔断降级）
- 增加安全认证机制（TLS/Token）



### 🛠️ 为什么要写这个框架？  
现有的 RPC 框架（如 gRPC、Dubbo）虽然功能强大，但内部机制相对复杂，难以直接理解其工作原理。**SimpleRPC 旨在从零构建一个简洁的 RPC 框架，帮助开发者深入学习 RPC 的核心概念，如服务注册、序列化、网络通信等关键技术点**。  

如果你想深入理解 RPC 的实现原理，并希望打造一个可以在项目中落地的轻量级解决方案，那么 SimpleRPC 绝对值得一试！ 🚀  

**文档后续再出呢**
