package com.ff.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ff.model.RpcRequest;
import com.ff.model.RpcResponse;

import java.io.IOException;

/*
 * 基于 Jackson 实现的 JSON 序列化器
 *
 * 问题说明：
 * JSON 序列化/反序列化存在泛型类型擦除的问题，特别是 List<User> 或 Object 类型字段。
 * 在反序列化时，由于泛型信息丢失，Jackson 默认会将对象解析为 LinkedHashMap 或 ArrayList，
 * 无法还原为原始业务对象，因此需要进行手动类型转换处理。
 *
 * 示例：
 * {
 *   "id": 123,
 *   "name": "Tom"
 * }
 * 被识别为 Map 而不是 User 类型，需要额外处理恢复原始类型。
 */
public class JsonSerializer implements Serializer {

    // ObjectMapper 是线程安全的，推荐使用单例，避免频繁创建带来的性能开销
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将对象序列化为字节数组
     *
     * @param object 要序列化的对象
     * @return 字节数组
     * @throws IOException 序列化失败时抛出
     */
    @Override
    public <T> byte[] serialize(T object) throws IOException {
        return objectMapper.writeValueAsBytes(object);
    }

    /**
     * 将字节数组反序列化为指定类型的对象
     *
     * @param bytes     字节数组
     * @param classType 目标类型
     * @return 反序列化后的对象
     * @throws IOException 反序列化失败时抛出
     */
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> classType) throws IOException {
        // 基础反序列化
        T obj = objectMapper.readValue(bytes, classType);

        // 针对 RpcRequest/RpcResponse 做特殊处理，解决泛型擦除的问题
        if (obj instanceof RpcResponse) {
            return handlerResponse((RpcResponse) obj, classType);
        }
        if (obj instanceof RpcRequest) {
            return handlerRequest((RpcRequest) obj, classType);
        }
        return obj;
    }

    /**
     * 特殊处理 RpcRequest 的反序列化，修复参数 Object[] 中元素被解析为 LinkedHashMap 的问题
     *
     * @param rpcRequest 初步反序列化后的对象
     * @param classType  目标类型（RpcRequest.class）
     * @return 处理完泛型后的对象
     * @throws IOException 转换失败时抛出
     */
    private <T> T handlerRequest(RpcRequest rpcRequest, Class<T> classType) throws IOException {
        Class<?>[] parameterTypes = rpcRequest.getParameterTypes(); // 每个参数声明的类型
        Object[] parameters = rpcRequest.getParameters();           // 实际反序列化的值

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            Object paramValue = parameters[i];

            // 如果类型不一致（通常是 LinkedHashMap），则手动转换成声明的类型
            if (paramValue != null && !parameterType.isAssignableFrom(paramValue.getClass())) {
                try {
                    parameters[i] = objectMapper.convertValue(paramValue, parameterType);
                } catch (IllegalArgumentException e) {
                    throw new IOException("参数类型转换失败: " + paramValue + " -> " + parameterType, e);
                }
            }
        }

        return classType.cast(rpcRequest);
    }

    /**
     * 特殊处理 RpcResponse 的反序列化，修复 result 字段中 Object 类型泛型丢失问题
     *
     * @param rpcResponse 初步反序列化后的对象
     * @param classType   目标类型（RpcResponse.class）
     * @return 处理完泛型后的对象
     * @throws IOException 转换失败时抛出
     */
    private <T> T handlerResponse(RpcResponse rpcResponse, Class<T> classType) throws IOException {
        Object rawResult = rpcResponse.getResult();
        Class<?> resultType = rpcResponse.getParameterType(); // 真实结果的类型

        try {
            // 将 result 从 LinkedHashMap 转换为实际类型（如 User、List<Order>）
            Object realResult = objectMapper.convertValue(rawResult, resultType);
            rpcResponse.setResult(realResult);
        } catch (IllegalArgumentException e) {
            throw new IOException("返回值类型转换失败: " + rawResult + " -> " + resultType, e);
        }
        // 返回泛型结果 <T> rpcResponse
        return classType.cast(rpcResponse);
    }
}
