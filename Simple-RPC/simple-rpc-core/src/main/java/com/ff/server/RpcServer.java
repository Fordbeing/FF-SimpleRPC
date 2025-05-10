package com.ff.server;

import java.io.IOException;

public interface RpcServer {
    void start(int port) throws IOException;

    byte[] sendPost(String url, byte[] contentBytes) throws Exception;
}
