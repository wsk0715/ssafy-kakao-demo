package com.example.demo_app.global.health;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo_app.global.health.model.HealthCheck;

@Mapper
public interface HealthMapper {
    HealthCheck check();
}
