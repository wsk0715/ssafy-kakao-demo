package com.example.demo_app.calls;

import com.example.demo_app.api.dto.ProgressReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;

@Tag(name = "Call Simulator API", description = "모의 피싱 가상 통화 시뮬레이터 제어 및 실시간 연동 API")
@RequestMapping("/api/v1/calls")
public interface CallApi {

    @Operation(summary = "실시간 SSE 통화 채널 연결", description = "서버와 웹 브라우저 간의 실시간 단방향 이벤트를 주고받기 위한 SSE(Server-Sent Events) 커넥션을 맺습니다.")
    @ApiResponse(responseCode = "200", description = "SSE Emitter 채널 반환")
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter connect(
            @Parameter(description = "연결을 요청하는 사용자 고유 식별자", example = "demo_user")
            @RequestParam(value = "userId", defaultValue = "demo_user") String userId
    );

    @Operation(summary = "모의 보이스피싱 전화 발신 트리거", description = "지정된 시나리오에 맞는 모의 전화를 브라우저(SSE 채널)에 수신(Ringing) 상태로 강제 발송합니다.")
    @ApiResponse(responseCode = "200", description = "발신 트리거 성공 ('Success' 반환)")
    @PostMapping("/trigger")
    String triggerCall(
            @Parameter(description = "전화를 수신할 사용자 고유 식별자", example = "demo_user")
            @RequestParam(value = "userId", defaultValue = "demo_user") String userId,
            @Parameter(description = "모의 전화에 사용할 피싱 시나리오 ID", example = "voice_prosecutor")
            @RequestParam(value = "scenarioId", defaultValue = "voice_prosecutor") String scenarioId
    );

    @Operation(summary = "훈련 시뮬레이션 실시간 진행 상태 기록", description = "사용자의 통화 수신, 연결, 통화 지속, 다이얼로그 단계 및 비정상 행위 검출 등 실시간 상태 변화를 기록합니다.")
    @ApiResponse(responseCode = "200", description = "기록 완료 ('Logged' 반환)")
    @PostMapping("/progress")
    String reportProgress(
            @Parameter(description = "훈련을 진행 중인 사용자 고유 식별자", example = "demo_user")
            @RequestParam(value = "userId", defaultValue = "demo_user") String userId,
            @RequestBody ProgressReport report
    );

    @Operation(summary = "텍스트 기반 실시간 WAV 음성 스트리밍", description = "전달받은 텍스트를 Local ChatTTS 엔진을 거쳐 실시간 WAV 오디오 바이너리 스트림으로 변환하여 반환합니다.")
    @ApiResponse(responseCode = "200", description = "WAV 오디오 스트림 반환")
    @GetMapping(value = "/stream")
    org.springframework.http.ResponseEntity<byte[]> streamAudio(
            @Parameter(description = "음성으로 변환할 텍스트 대사", example = "서울중앙지검 김민수 검사입니다.")
            @RequestParam(value = "text") String text
    );

    @Operation(summary = "실시간 LLM 기반 피싱 대화 응답 생성 (스트리밍)", description = "사용자의 입력 텍스트에 대응하는 보이스피싱 공격자의 대사를 실시간으로 생성하여 SSE 스트림으로 반환합니다.")
    @ApiResponse(responseCode = "200", description = "SSE 스트림 반환")
    @GetMapping(value = "/respond", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter getLlmResponse(
            @Parameter(description = "연결된 사용자 고유 식별자", example = "demo_user")
            @RequestParam("userId") String userId,
            @Parameter(description = "모의 전화에 사용할 피싱 시나리오 ID", example = "voice_prosecutor")
            @RequestParam("scenarioId") String scenarioId,
            @Parameter(description = "사용자의 입력 텍스트", example = "네, 무슨 일이죠?")
            @RequestParam("text") String text
    );
}


