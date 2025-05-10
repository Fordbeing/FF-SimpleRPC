package com.ff.server;

import com.ff.serializer.JdkSerializer;
import com.ff.serializer.Serializer;
import com.ff.spi.SpiLoader;

public class RpcServerFactory {

    static {
        SpiLoader.load(RpcServer.class);
    }

    // 默认使用的服务器实例（当key找不到对应实例时返回）
    private static final RpcServer DEFAULT_RPCSERVER = new RpcHttpServer();

    /**
     * 根据指定key返回对应的 web 服务器实例
     *
     * @param key 服务器实例标识（如 "http"、"netty"、"vert.x"）
     * @return 对应的RpcServer实例，如果找不到则返回默认的HTTP服务器
     */
    public static RpcServer getInstance(String key) {
        RpcServer instance = SpiLoader.getInstance(RpcServer.class, key);
        if(instance == null){
            return DEFAULT_RPCSERVER;
        }
        return instance;
    }


}
