package com.example.demo_app.domain.users;

import com.example.demo_app.api.dto.ProfilingOption;
import com.example.demo_app.api.dto.ProfilingQuestion;
import com.example.demo_app.api.dto.ProfilingResultResponse;
import com.example.demo_app.api.dto.UserProfilingRequest;
import com.example.demo_app.domain.users.UserMapper;
import com.example.demo_app.domain.users.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    private static final List<ProfilingQuestion> PROFILING_QUESTIONS = Arrays.asList(
            new ProfilingQuestion("age", "연령대를 선택해주세요", Arrays.asList(
                    new ProfilingOption("20대 이하", "20s"),
                    new ProfilingOption("30대 ~ 40대", "30_40s"),
                    new ProfilingOption("50대 ~ 60대", "50_60s"),
                    new ProfilingOption("70대 이상", "70s_over")
            )),
            new ProfilingQuestion("finance", "가장 자주 이용하는 금융 서비스는 무엇인가요?", Arrays.asList(
                    new ProfilingOption("스마트폰 뱅킹 앱 (이체/조회)", "smart_banking"),
                    new ProfilingOption("주식/가상자산 투자 앱", "investment"),
                    new ProfilingOption("전화/ARS 또는 오프라인 지점 방문", "offline")
            )),
            new ProfilingQuestion("interest", "최근 주요 관심 분야를 선택해주세요", Arrays.asList(
                    new ProfilingOption("정부 지원 대출 및 저금리 갈아타기", "loan"),
                    new ProfilingOption("모바일 부업 및 재테크 정보", "side_job"),
                    new ProfilingOption("택배 배송 및 온라인 쇼핑 결제", "shopping")
            ))
    );

    public List<ProfilingQuestion> getQuestions() {
        log.info("Fetching profiling questions from UserService");
        return PROFILING_QUESTIONS;
    }

    public String saveProfiling(String userId, UserProfilingRequest request) {
        log.info("Registering profiling for user: {}", userId);
        try {
            String vulnerabilitiesJson = objectMapper.writeValueAsString(request.getVulnerabilities());
            User user = new User(
                    userId,
                    request.getRiskLevel(),
                    request.getRiskType(),
                    request.getDescription(),
                    vulnerabilitiesJson
            );
            userMapper.insertUser(user);
            return "Success";
        } catch (Exception e) {
            log.error("Failed to save profiling", e);
            return "Fail: " + e.getMessage();
        }
    }

    public ProfilingResultResponse analyzeRisk(String userId, Map<String, String> answers) {
        log.info("Analyzing risk for user: {}, answers: {}", userId, answers);
        
        String age = answers.getOrDefault("age", "20s");
        String finance = answers.getOrDefault("finance", "smart_banking");
        String interest = answers.getOrDefault("interest", "loan");

        String riskLevel = "MEDIUM";
        String riskType = "스마트 금융 피싱 주의군";
        String description = "비교적 모바일 환경에 친숙하나 모바일 부업이나 간편 대출 사칭 스미싱에 노출될 수 있습니다.";
        List<String> vulnerabilities = Arrays.asList("저금리 정부지원 사칭 대출 스미싱", "SNS 고수익 부업 알바 사기 문자");

        if ("70s_over".equals(age) || "offline".equals(finance)) {
            riskLevel = "HIGH";
            riskType = "대면/전화 유도 보이스피싱 고위험군";
            description = "공공기관(검찰, 금감원) 혹은 가족을 사칭하여 급박하게 전화를 통해 송금이나 앱 설치를 유도하는 수법에 취약합니다.";
            vulnerabilities = Arrays.asList("가족 사칭 납치/긴급 송금 요구 전화", "금감원/검찰청 사칭 수사 협조 요구 전화");
        } else if ("50_60s".equals(age) || "loan".equals(interest)) {
            riskLevel = "HIGH";
            riskType = "가짜 금융기관 대출 신청 피싱 위험군";
            description = "어려운 경제 상황을 악용한 저금리 대환대출 안내 문자와 신용 확인을 핑계로 한 악성 앱 설치 유도에 노출될 가능성이 높습니다.";
            vulnerabilities = Arrays.asList("정부지원 서민금융 사칭 대출 실행 스미싱", "원격 제어 앱 설치 요구 보이스피싱");
        } else if ("20s".equals(age) && "side_job".equals(interest)) {
            riskLevel = "MEDIUM";
            riskType = "온라인 아르바이트 및 피싱메일 위험군";
            description = "온라인 커뮤니티, 구인 광고를 이용한 대포통장 개설 유도 및 가상 자산 투자 사기 문구에 쉽게 매료될 수 있습니다.";
            vulnerabilities = Arrays.asList("고수익 쇼핑몰 리뷰 작성 알바 사기", "개인정보 유출 안내 메일 피싱");
        } else if ("20s".equals(age) || "30_40s".equals(age)) {
            riskLevel = "LOW";
            riskType = "비교적 양호한 디지털 보안 인식군";
            description = "정상적인 앱스토어 이용 습관과 이중 보안을 잘 인지하고 있으나, 최신 배송 지연 문구나 피싱 결제 문구에는 여전히 의심이 필요합니다.";
            vulnerabilities = Arrays.asList("해외 직구 위장 관세 납부 사기 스미싱", "택배 주소지 오류 수정 안내 문자");
        }

        try {
            String vulnerabilitiesJson = objectMapper.writeValueAsString(vulnerabilities);
            User user = new User(
                    userId,
                    riskLevel,
                    riskType,
                    description,
                    vulnerabilitiesJson
            );
            userMapper.insertUser(user);
        } catch (Exception e) {
            log.error("Failed to save profiling to DB during analysis", e);
        }

        return new ProfilingResultResponse(riskType, riskLevel, description, vulnerabilities);
    }
}
