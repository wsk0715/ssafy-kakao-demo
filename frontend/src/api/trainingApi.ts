// 5-layer architecture: API Layer for Phishing Training Logs
// Records user simulation results and fetches monthly statistics

export interface LogPayload {
  scenarioId: string
  actionType: 'CLICK_LINK' | 'ENTERED_DATA' | 'COMPLETED_CALL' | 'HUNG_UP_SUCCESS' | 'BLOCKED_SMS'
  callDurationSeconds?: number
  riskyBehaviorDetected: boolean
}

export interface MonthlyReport {
  month: string
  totalSimulations: number
  riskyActionsCount: number
  vulnerabilityRate: number // percentage
  topVulnerabilities: { type: string; score: number }[]
  history: { date: string; title: string; result: 'SUCCESS' | 'FAILED' }[]
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
    { date: '05-18', title: '택배 배송 오류 스미싱 링크', result: 'FAILED' },
    { date: '05-19', title: '서울중앙지검 김민수 검사 전화', result: 'SUCCESS' },
    { date: '05-20', title: '국민은행 대환대출 권유 전화', result: 'FAILED' },
    { date: '05-22', title: '구글 비정상 로그인 메일 피싱', result: 'SUCCESS' }
  ]
}

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
