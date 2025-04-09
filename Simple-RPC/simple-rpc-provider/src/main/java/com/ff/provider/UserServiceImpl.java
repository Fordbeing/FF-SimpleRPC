package com.ff.provider;

import com.ff.common.model.User;
import com.ff.common.service.UserService;

public class UserServiceImpl implements UserService {
    @Override
    public User getUser(User user) {
        System.out.println("姓名：" + user.getUserName() + " 年龄:" + user.getAge());
        return user;
    }
}
