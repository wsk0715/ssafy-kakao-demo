# 🛡️ 피싱 예방 모의 훈련 시뮬레이터 (Phishing Prevention Simulator)

개인 맞춤형 위험 진단 및 대화형 모의 피싱 시나리오 훈련을 제공하여, 사용자의 피싱 대응력을 극대화하고 보안 의식을 고취시키는 데모 서비스입니다.

---

## 🎨 주요 기능 (Key Features)

### 1. 개인 맞춤형 위험 프로파일링 (Self-Diagnosis)
*   사용자의 연령대, 자주 쓰는 금융 서비스, 관심 분야 설문을 토대로 취약 위험 등급 및 피싱 위협군을 정밀 진단합니다.
*   진단된 취약 유형에 알맞은 맞춤 대응 가이드와 위험 수법을 제공합니다.

### 2. 맞춤형 모의 훈련 즉시 연동 (Tailored Training Start)
*   자가진단 완료 후, 결과 화면에서 **"나에게 맞춘 모의 훈련 시작하기"** 버튼을 클릭하여 진단 결과와 매핑된 모의 보이스피싱 시나리오로 원클릭 훈련에 진입할 수 있습니다.

### 3. 실시간 모의 훈련 (Interactive Simulations)
*   **보이스피싱 (Voice Phishing)**: 실감 나는 다크 모드 수신 화면 및 iOS 통화 UI를 구현하고, 실시간 통화 대사 자막과 통화 지속 시간 분석을 수행합니다.
*   **스미싱 및 이메일 피싱 (SMS/Email)**: 택배 배송 정보 수정 알림이나 카카오 계정 보안 경고 메일 등을 재현하여 악성 링크 클릭 및 계정 정보 입력의 위험을 훈련합니다.

### 4. 결과 분석 리포트 및 이력 관리 (Vulnerability Reports & Dashboard)
*   **취약 진단 등급 평가**: 통화 차단 시간(0~10초: 안전 / 10~25초: 주의 / 25초 이상: 위험)을 정밀 계산하여 직관적으로 진단합니다.
*   **공격 기법 타임라인**: 훈련 단계별 사기범의 심리 유도 및 사칭 기법을 타임라인으로 시각화하며, 종료한 단계를 강조하여 제공합니다.
*   **안심 처리 보장**: 사용자의 오디오 음성 데이터나 민감 개인정보는 서버로 수집되지 않음을 명시하여 개인정보 보호를 최우선으로 다룹니다.
*   **리포트 이력**: 진행된 모든 모의 훈련 로그가 저장되며, 이력 카드를 클릭하면 언제든지 동일한 결과 보고서 팝업 모달을 확인할 수 있습니다.

---

## 💻 기술 스택 (Tech Stack)

### Frontend
*   **Core**: Vue 3.5 (Composition API, TypeScript)
*   **State Management**: Pinia
*   **Styling**: TailwindCSS v4
*   **Bundler**: Vite
*   **Build Tool**: Vue-TSC

### Backend (Integration Support)
*   **Framework**: Spring Boot 4.0 (Java 21)
*   **Database**: MySQL 8.x
*   **Mapper**: MyBatis
*   **API Doc**: SpringDoc OpenAPI
*   **Simulation Trigger**: SSE (Server-Sent Events)를 활용한 동적인 실시간 전화 푸시 전송 기능 탑재
