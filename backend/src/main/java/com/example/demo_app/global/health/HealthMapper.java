package com.example.demo_app.health;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo_app.health.model.HealthCheck;

@Mapper
public interface HealthMapper {
    HealthCheck check();
}
