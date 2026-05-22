package com.example.demo_app.domain.users;

import com.example.demo_app.domain.users.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    void insertUser(User user);
    User getUserById(@Param("userId") String userId);
}
