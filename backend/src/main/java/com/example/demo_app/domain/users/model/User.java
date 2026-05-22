package com.example.demo_app.domain.users.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String userId;
    private String riskLevel;
    private String riskType;
    private String description;
    private String vulnerabilities; // JSON string
}
