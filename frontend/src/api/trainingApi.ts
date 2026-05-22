// 5-layer architecture: API Layer for Phishing Training Logs
// Records user simulation results and fetches monthly statistics

export interface LogPayload {
  scenarioId: string
  actionType: 'CLICK_LINK' | 'ENTERED_DATA' | 'COMPLETED_CALL' | 'HUNG_UP_SUCCESS' | 'BLOCKED_SMS'
  callDurationSeconds?: number
  riskyBehaviorDetected: boolean
}

export interface HistoryItem {
  date: string
  title: string
  result: 'SUCCESS' | 'FAILED'
  scenarioId?: string
  scenarioType?: 'VOICE' | 'SMS' | 'EMAIL'
  duration?: number
  hangUpStepIndex?: number
  hangUpStepName?: string
  vulnerabilityStatus?: 'SAFE' | 'WARNING' | 'CRITICAL'
  vulnerabilityExplanation?: string
  feedback?: string
  techniques?: { step: string; name: string; desc: string }[]
}

export interface MonthlyReport {
  month: string
  totalSimulations: number
  riskyActionsCount: number
  vulnerabilityRate: number // percentage
  topVulnerabilities: { type: string; score: number }[]
  history: HistoryItem[]
}

const mockReport: MonthlyReport = {
  month: '2026년 05월',
  totalSimulations: 8,
  riskyActionsCount: 3,
  vulnerabilityRate: 37.5,
  topVulnerabilities: [
    { type: '대출 사칭 스미싱 링크 클릭', score: 90 },
    { type: '가족 긴급 송금 요구 보이스피싱 대응', score: 65 },
    { type: '가짜 로그인 화면 ID/비밀번호 입력 시도', score: 40 }
  ],
  history: [
    {
      date: '05-22',
      title: '구글 비정상 로그인 메일 피싱',
      result: 'SUCCESS',
      scenarioId: 'email_security',
      scenarioType: 'EMAIL',
      duration: 0,
      vulnerabilityStatus: 'SAFE',
      vulnerabilityExplanation: '보안 안내 메일의 링크 도메인이 구글 공식 도메인이 아닌 피싱 도메인임을 확인하고 비밀번호 입력을 거부하여 대응에 성공했습니다.',
      feedback: '보안 안내 메일을 수신했을 때는 링크 주소창의 도메인 철자가 공식 사이트와 일치하는지 철저히 확인해야 합니다.',
      techniques: [
        { step: '1단계', name: '비정상 로그인 시도 메일 전송', desc: '계정이 도용되었다는 거짓 경고 메일로 위기감을 자극합니다.' },
        { step: '2단계', name: '가짜 계정 복구 사이트 연결', desc: '공식 사이트와 거의 동일한 로고와 레이아웃의 가짜 로그인 창으로 정보 입력을 유도합니다.' }
      ]
    },
    {
      date: '05-20',
      title: '국민은행 대환대출 권유 전화',
      result: 'FAILED',
      scenarioId: 'voice_loan',
      scenarioType: 'VOICE',
      duration: 35,
      hangUpStepIndex: 2,
      hangUpStepName: '3단계 (기존 대출 즉시 상환 요구)',
      vulnerabilityStatus: 'CRITICAL',
      vulnerabilityExplanation: '저금리 전환 대출 조건으로 기존 대출금을 금융사 임시 가상계좌로 즉시 이체하라는 요구 단계까지 듣고 통화를 지속하여 금융 사기 피해 가능성이 극도로 높습니다.',
      feedback: '대환 대출 시 기존 대출금을 특정인 명의 계좌나 가상계좌로 직접 상환하라는 금융사는 100% 사기입니다.',
      techniques: [
        { step: '1단계', name: '우량 은행 사칭 및 특별 대환대출 제안', desc: '저금리 신용 대출이 가능하다며 사용자를 유혹합니다.' },
        { step: '2단계', name: '기존 대출 약정 위반 및 계약 해지 공포 유발', desc: '동시 대출은 금융법 위반이라며 기존 대출을 당장 갚아야 한다고 협박합니다.' },
        { step: '3단계', name: '가상 계좌 송금 및 현금 수금인 전달 요구', desc: '지정 계좌로 이체하거나 직원을 보낼 테니 현금을 인출하여 전달하라고 요구합니다.' }
      ]
    },
    {
      date: '05-19',
      title: '서울중앙지검 김민수 검사 전화',
      result: 'SUCCESS',
      scenarioId: 'voice_prosecutor',
      scenarioType: 'VOICE',
      duration: 8,
      hangUpStepIndex: 0,
      hangUpStepName: '1단계 (수사기관 사칭 및 신원 확인)',
      vulnerabilityStatus: 'SAFE',
      vulnerabilityExplanation: '전화 시작 8초 시점에 서울중앙지검 검사를 사칭하는 피싱 전화를 인지하고 빠르게 차단하였습니다.',
      feedback: '모르는 번호의 수사기관 주장 전화는 즉시 끊고 해당 검찰청 공식 번호로 사실을 확인해야 합니다.',
      techniques: [
        { step: '1단계', name: '공공기관(검찰) 사칭 및 신원 확인', desc: '공신력 있는 기관 이름을 대어 의심을 낮추고 본인 여부를 확인합니다.' },
        { step: '2단계', name: '사법 절차 언급 및 범죄 연루 공포 조장', desc: '대포통장 등 법적 처벌 가능성을 언급해 심리적 공황 상태를 유발합니다.' },
        { step: '3단계', name: '자산 보호 명목 임시 계좌 송금 압박', desc: '구속영장 청구 등의 긴박한 위협과 함께 금감원 계좌로의 즉시 이체를 강요합니다.' }
      ]
    },
    {
      date: '05-18',
      title: '택배 배송 오류 스미싱 링크',
      result: 'FAILED',
      scenarioId: 'sms_delivery',
      scenarioType: 'SMS',
      duration: 0,
      vulnerabilityStatus: 'CRITICAL',
      vulnerabilityExplanation: '택배 배송지 주소 오류 사칭 문자에 포함된 불분명한 웹링크(URL)를 클릭하여 개인 정보 입력 및 악성 앱 다운로드 위험에 노출되었습니다.',
      feedback: '택배사 및 우체국은 문자 메시지에 절대 외부 링크를 전송하지 않습니다. 주소지 변경이나 반송 알림 문자의 링크는 100% 스미싱이므로 누르지 않고 무시해야 합니다.',
      techniques: [
        { step: '1단계', name: '택배 배송 불가 알림', desc: '주소 오류 등의 생활 밀착형 핑계로 링크 클릭을 유도합니다.' },
        { step: '2단계', name: '개인정보 입력 탈취', desc: '본인인증을 유도하는 가짜 사이트에서 연락처, 이름 등을 입력받아 수집합니다.' }
      ]
    }
  ]
};

export const trainingApi = {
  async recordLog(payload: LogPayload): Promise<{ success: boolean }> {
    console.log('[API POST] user action logged:', payload)
    return new Promise((resolve) => {
      setTimeout(() => resolve({ success: true }), 100)
    })
  },

  async getMonthlyReport(): Promise<MonthlyReport> {
    return new Promise((resolve) => {
      setTimeout(() => resolve(mockReport), 150)
    })
  }
}
