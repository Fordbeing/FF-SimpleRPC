package com.ff.common.service;

import com.ff.common.model.User;

public interface UserService {

    User getUser(User user); // 获取用户

    default short getNum(){
        return 1;
    }
}
