package com.ff.common.model;

import lombok.Data;

import java.io.Serializable;

// 后续要序列化，所以此处实现序列化接口
@Data
public class User implements Serializable {
    private String userName; // 姓名
    private int age; // 年龄
}
