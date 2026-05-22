package com.example.demo_app.api.dto;

import lombok.Data;

@Data
public class LogPayloadRequest {
    private String scenarioId;
    private String actionType;
    private Integer callDurationSeconds;
    private boolean riskyBehaviorDetected;
}
