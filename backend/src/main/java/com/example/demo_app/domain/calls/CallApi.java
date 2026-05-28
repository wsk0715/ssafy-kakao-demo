package com.example.demo_app.domain.calls;

import com.example.demo_app.api.dto.ProgressReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "실시간 통화 시뮬레이션", description = "보이스피싱 공격자와의 실시간 상호작용을 위한 API")
@RequestMapping("/api/v1/calls")
public interface CallApi {

    @Operation(summary = "SSE 연결 수립", description = "실시간 시뮬레이션 업데이트를 위한 서버 연결을 시작합니다.")
    @GetMapping("/connect")
    SseEmitter connect(
            @Parameter(description = "사용자 ID", example = "demo_user")
            @RequestParam String userId
    );

    @Operation(summary = "공격자 응답 요청", description = "사용자의 발화 내용을 전달하고 공격자의 다음 대사를 SSE로 수신합니다.")
    @GetMapping(value = "/respond", produces = "text/event-stream")
    SseEmitter respond(
            @Parameter(description = "사용자 ID", example = "demo_user")
            @RequestParam String userId,
            @Parameter(description = "시나리오 ID", example = "voice_prosecutor")
            @RequestParam String scenarioId,
            @Parameter(description = "사용자 발화 내용", example = "여보세요? 누구세요?")
            @RequestParam String text
    );

    @Operation(summary = "음성 스트리밍", description = "텍스트를 음성으로 변환하여 오디오 스트림을 반환합니다.")
    @GetMapping("/stream")
    void streamAudio(
            @Parameter(description = "음성으로 변환할 텍스트", example = "안녕하세요. 중앙지검 검사 김철수입니다.")
            @RequestParam String text,
            jakarta.servlet.http.HttpServletResponse response
    );

    @Operation(summary = "훈련 진행 보고", description = "시뮬레이션 진행 상태를 서버에 기록합니다.")
    @PostMapping("/progress")
    void reportProgress(@RequestBody ProgressReport report);

    @Operation(summary = "훈련 전화 강제 트리거", description = "특정 사용자에게 모의 피싱 전화를 강제로 발신합니다 (SSE 이벤트 전송).")
    @PostMapping("/trigger")
    void triggerCall(
            @Parameter(description = "대상 사용자 ID", example = "demo_user")
            @RequestParam String userId,
            @Parameter(description = "실행할 시나리오 ID", example = "voice_prosecutor")
            @RequestParam String scenarioId
    );

    @Operation(summary = "대화 인터럽트(끊기) 보고", description = "사용자가 공격자의 말을 중간에 끊었을 때 실제 들은 구간을 보고하여 대화 맥락을 동기화합니다.")
    @PostMapping("/interrupt")
    void interrupt(
            @Parameter(description = "사용자 ID", example = "demo_user")
            @RequestParam String userId,
            @Parameter(description = "시나리오 ID", example = "voice_prosecutor")
            @RequestParam String scenarioId,
            @Parameter(description = "사용자가 실제 들은 텍스트 지점", example = "중앙지검 검사입니다")
            @RequestParam String lastHeardText
    );
}
