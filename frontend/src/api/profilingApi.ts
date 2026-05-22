// 5-layer architecture: API Layer for Profiling
// Defines profiling question schemas and mock endpoint

export interface ProfilingQuestion {
  id: string
  text: string
  options: { label: string; value: string }[]
}

export interface ProfilingResult {
  riskType: string
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  description: string
  vulnerabilities: string[]
}

const mockQuestions: ProfilingQuestion[] = [
  {
    id: 'age',
    text: '연령대를 선택해주세요',
    options: [
      { label: '20대 이하', value: '20s' },
      { label: '30대 ~ 40대', value: '30_40s' },
      { label: '50대 ~ 60대', value: '50_60s' },
      { label: '70대 이상', value: '70s_over' }
    ]
  },
  {
    id: 'finance',
    text: '가장 자주 이용하는 금융 서비스는 무엇인가요?',
    options: [
      { label: '스마트폰 뱅킹 앱 (이체/조회)', value: 'smart_banking' },
      { label: '주식/가상자산 투자 앱', value: 'investment' },
      { label: '전화/ARS 또는 오프라인 지점 방문', value: 'offline' }
    ]
  },
  {
    id: 'interest',
    text: '최근 주요 관심 분야를 선택해주세요',
    options: [
      { label: '정부 지원 대출 및 저금리 갈아타기', value: 'loan' },
      { label: '모바일 부업 및 재테크 정보', value: 'side_job' },
      { label: '택배 배송 및 온라인 쇼핑 결제', value: 'shopping' }
    ]
  }
]

export const profilingApi = {
  async getQuestions(): Promise<ProfilingQuestion[]> {
    return new Promise((resolve) => {
      setTimeout(() => resolve(mockQuestions), 100)
    })
  },

  async analyzeRisk(answers: Record<string, string>): Promise<ProfilingResult> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const age = answers.age || '20s'
        const finance = answers.finance || 'smart_banking'
        const interest = answers.interest || 'loan'

        let riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' = 'MEDIUM'
        let riskType = '스마트 금융 피싱 주의군'
        let description = '비교적 모바일 환경에 친숙하나 모바일 부업이나 간편 대출 사칭 스미싱에 노출될 수 있습니다.'
        let vulnerabilities = ['저금리 정부지원 사칭 대출 스미싱', 'SNS 고수익 부업 알바 사기 문자']

        if (age === '70s_over' || finance === 'offline') {
          riskLevel = 'HIGH'
          riskType = '대면/전화 유도 보이스피싱 고위험군'
          description = '공공기관(검찰, 금감원) 혹은 가족을 사칭하여 급박하게 전화를 통해 송금이나 앱 설치를 유도하는 수법에 취약합니다.'
          vulnerabilities = ['가족 사칭 납치/긴급 송금 요구 전화', '금감원/검찰청 사칭 수사 협조 요구 전화']
        } else if (age === '50_60s' || interest === 'loan') {
          riskLevel = 'HIGH'
          riskType = '가짜 금융기관 대출 신청 피싱 위험군'
          description = '어려운 경제 상황을 악용한 저금리 대환대출 안내 문자와 신용 확인을 핑계로 한 악성 앱 설치 유도에 노출될 가능성이 높습니다.'
          vulnerabilities = ['정부지원 서민금융 사칭 대출 실행 스미싱', '원격 제어 앱 설치 요구 보이스피싱']
        } else if (age === '20s' && interest === 'side_job') {
          riskLevel = 'MEDIUM'
          riskType = '온라인 아르바이트 및 피싱메일 위험군'
          description = '온라인 커뮤니티, 구인 광고를 이용한 대포통장 개설 유도 및 가상 자산 투자 사기 문구에 쉽게 매료될 수 있습니다.'
          vulnerabilities = ['고수익 쇼핑몰 리뷰 작성 알바 사기', '개인정보 유출 안내 메일 피싱']
        } else if (age === '20s' || age === '30_40s') {
          riskLevel = 'LOW'
          riskType = '비교적 양호한 디지털 보안 인식군'
          description = '정상적인 앱스토어 이용 습관과 이중 보안을 잘 인지하고 있으나, 최신 배송 지연 문구나 피싱 결제 문구에는 여전히 의심이 필요합니다.'
          vulnerabilities = ['해외 직구 위장 관세 납부 사기 스미싱', '택배 주소지 오류 수정 안내 문자']
        }

        resolve({ riskType, riskLevel, description, vulnerabilities })
      }, 200)
    })
  }
}
