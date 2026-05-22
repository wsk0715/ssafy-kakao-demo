package com.example.demo_app.domain.scenarios.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Scenario {
    private String id;
    private String type; // VOICE, SMS, EMAIL
    private String title;
    private String sender;
    private String content;
    private String attackerAction;
    private String warningExplanation;
    private String steps; // JSON string
    private String stageDetails; // JSON string
    private String smsEmailReport; // JSON string
}
