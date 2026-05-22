package com.example.demo_app.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfilingQuestion {
    private String id;
    private String text;
    private List<ProfilingOption> options;
}
