package com.example.demo_app.domain.scenarios;

import com.example.demo_app.domain.scenarios.model.Scenario;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ScenarioMapper {
    List<Scenario> getAllScenarios();
    Scenario getScenarioById(@Param("id") String id);
    void insertScenario(Scenario scenario);
}
