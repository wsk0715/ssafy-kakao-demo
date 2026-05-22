package com.example.demo_app.api;

import com.example.demo_app.api.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Phishing API", description = "피싱 예방 모의 훈련 및 자가진단 프로파일링 API")
@RequestMapping("/api/v1")
public interface PhishingApi {

    @Operation(summary = "자가진단 프로파일링 수동 저장", description = "사용자의 자가진단 위험도 분석 결과를 직접 DB에 수동 저장합니다.")
    @ApiResponse(responseCode = "200", description = "저장 성공 ('Success' 반환)")
    @PostMapping("/profiling")
    String saveProfiling(
            @Parameter(description = "사용자 고유 식별자", example = "demo_user")
            @RequestParam(value = "userId", defaultValue = "demo_user") String userId,
            @RequestBody UserProfilingRequest request
    );

    @Operation(summary = "모의 훈련 시나리오 전체 조회", description = "DB에 적재된 모든 모의 훈련용 시나리오 리스트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "시나리오 리스트 반환")
    @GetMapping("/scenarios")
    List<Map<String, Object>> getScenarios();

    @Operation(summary = "모의 훈련 시나리오 단건 조회", description = "ID를 기반으로 특정 모의 훈련 시나리오의 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "시나리오 상세 맵 반환")
    @GetMapping("/scenarios/{id}")
    Map<String, Object> getScenarioById(
            @Parameter(description = "시나리오 고유 ID", example = "voice_prosecutor")
            @PathVariable("id") String id
    );

    @Operation(summary = "모의 훈련 이력 로깅", description = "사용자가 진행한 모의 피싱 훈련 로그(훈련 결과, 대응 동작 등)를 DB에 적재합니다.")
    @ApiResponse(responseCode = "200", description = "성공 여부 맵 반환")
    @PostMapping("/training/log")
    Map<String, Object> recordTrainingLog(
            @Parameter(description = "사용자 고유 식별자", example = "demo_user")
            @RequestParam(value = "userId", defaultValue = "demo_user") String userId,
            @RequestBody LogPayloadRequest request
    );

    @Operation(summary = "월간 훈련 분석 리포트 생성", description = "특정 사용자의 월간 훈련 횟수, 취약 행위 수, 취약점 유형별 위험도 점수 및 훈련 로그 요약 리스트를 집계하여 반환합니다.")
    @ApiResponse(responseCode = "200", description = "월간 통계 및 이력 요약 리포트 반환")
    @GetMapping("/reports/monthly")
    Map<String, Object> getMonthlyReport(
            @Parameter(description = "사용자 고유 식별자", example = "demo_user")
            @RequestParam(value = "userId", defaultValue = "demo_user") String userId
    );

    @Operation(summary = "자가진단 질문 목록 조회", description = "사용자 맞춤형 프로파일링을 위한 선택형 진단 문항 목록을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "질문지 목록 반환")
    @GetMapping("/profiling/questions")
    List<ProfilingQuestion> getQuestions();

    @Operation(summary = "자가진단 답변 분석 및 DB 저장", description = "사용자가 응답한 자가진단 결과를 평가하여 위험도 수준 및 취약점을 도출하고, 이를 DB에 기록한 뒤 결과를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "위험도 분석 최종 리포트 결과 반환")
    @PostMapping("/profiling/analyze")
    ProfilingResultResponse analyzeRisk(
            @Parameter(description = "사용자 고유 식별자", example = "demo_user")
            @RequestParam(value = "userId", defaultValue = "demo_user") String userId,
            @RequestBody Map<String, String> answers
    );
}
