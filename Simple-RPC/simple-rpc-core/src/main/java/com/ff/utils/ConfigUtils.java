package com.ff.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.dialect.Props;

import java.util.Properties;

/*
 用于加载配置文件，将 RPC 静态配置转为通过配置项进行配置，
 即采用 application.properties配置

 */
public class ConfigUtils {
    public static <T> T loadConfig(Class<T> className, String prefix) {
        return loadConfig(className, prefix, "");
    }


    public static <T> T loadConfig(Class<T> className, String prefix, String environment) {
        // 对了配置文件进行拼接 application.properties
        StringBuilder configFileBuilder = new StringBuilder("application");
        // 环境配置
        if (StrUtil.isNotBlank(environment)) {
            configFileBuilder.append("-");
        }
        configFileBuilder.append(".properties");

        // 构建好配置文件之后
        // 加载配置文件
        Props props = new Props(configFileBuilder.toString());
        return props.toBean(className, prefix);
    }
}
