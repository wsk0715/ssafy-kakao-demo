package com.example.demo_app.domain.scenarios;

import com.example.demo_app.domain.scenarios.ScenarioMapper;
import com.example.demo_app.domain.scenarios.model.Scenario;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScenarioService {

    private final ScenarioMapper scenarioMapper;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> getScenarios() {
        log.info("Fetching all scenarios from database via ScenarioService");
        try {
            List<Scenario> rawScenarios = scenarioMapper.getAllScenarios();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Scenario s : rawScenarios) {
                result.add(convertScenarioToMap(s));
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch scenarios", e);
            return Collections.emptyList();
        }
    }

    public Map<String, Object> getScenarioById(String id) {
        log.info("Fetching scenario by id: {} via ScenarioService", id);
        try {
            Scenario s = scenarioMapper.getScenarioById(id);
            if (s == null) {
                return Collections.emptyMap();
            }
            return convertScenarioToMap(s);
        } catch (Exception e) {
            log.error("Failed to fetch scenario by id: {}", id, e);
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> convertScenarioToMap(Scenario s) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("type", s.getType());
        map.put("title", s.getTitle());
        map.put("sender", s.getSender());
        map.put("content", s.getContent());
        map.put("attackerAction", s.getAttackerAction());
        map.put("warningExplanation", s.getWarningExplanation());
        
        try {
            if (s.getSteps() != null) {
                map.put("steps", objectMapper.readValue(s.getSteps(), new TypeReference<List<Map<String, Object>>>() {}));
            } else {
                map.put("steps", Collections.emptyList());
            }
        } catch (Exception e) {
            log.error("Failed to parse steps JSON for scenario: {}", s.getId(), e);
            map.put("steps", Collections.emptyList());
        }
        
        try {
            if (s.getStageDetails() != null) {
                map.put("stageDetails", objectMapper.readValue(s.getStageDetails(), new TypeReference<List<Map<String, Object>>>() {}));
            }
        } catch (Exception e) {
            log.error("Failed to parse stageDetails JSON for scenario: {}", s.getId(), e);
        }
        
        try {
            if (s.getSmsEmailReport() != null) {
                map.put("smsEmailReport", objectMapper.readValue(s.getSmsEmailReport(), new TypeReference<Map<String, Object>>() {}));
            }
        } catch (Exception e) {
            log.error("Failed to parse smsEmailReport JSON for scenario: {}", s.getId(), e);
        }
        
        return map;
    }
}
