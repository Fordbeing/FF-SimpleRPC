package com.ff;

import com.ff.common.model.User;
import com.ff.common.service.UserService;

public class Main {
    public static void main(String[] args) {
        // 消费者进行远程调用，调用provider的方法

        User user = new User();
        user.setUserName("FF");
        user.setAge(18);
        // 调用
        User newUser = userService.getUser(user);
        if (newUser != null) {
            System.out.println("username: " + newUser.getUserName() + ", age: " + newUser.getAge());
        } else {
            System.out.println("user == null");
        }
    }
}