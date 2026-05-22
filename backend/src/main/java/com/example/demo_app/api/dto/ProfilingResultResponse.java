package com.example.demo_app.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfilingResultResponse {
    private String riskType;
    private String riskLevel;
    private String description;
    private List<String> vulnerabilities;
}
