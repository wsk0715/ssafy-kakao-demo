package com.example.demo_app.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserProfilingRequest {
    private String riskLevel;
    private String riskType;
    private String description;
    private List<String> vulnerabilities;
}
