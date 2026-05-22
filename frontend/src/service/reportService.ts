// 5-layer architecture: Service Layer for Reports and Matches
// Retrieves analysis report data and maps incidents to the correct emergency organizations

import { trainingApi } from '../api/trainingApi'
import { useReportStore } from '../state/reportStore'

export interface ResponseContact {
  orgName: string
  phoneNumber: string
  description: string
  urgency: 'HIGH' | 'MEDIUM' | 'LOW'
}

export const reportService = {
  async loadMonthlyReport(): Promise<void> {
    const store = useReportStore()
    store.setLoading(true)
    try {
      const data = await trainingApi.getMonthlyReport()
      store.setReport(data)
    } catch (error) {
      console.error('Failed to load monthly report:', error)
    } finally {
      store.setLoading(false)
    }
  },

  getResponseContacts(failedTypes: string[]): ResponseContact[] {
    const contacts: ResponseContact[] = []

    // Always append basic emergency numbers
    contacts.push({
      orgName: '경찰청 (피해 신고)',
      phoneNumber: '112',
      description: '실제 송금 피해 발생 시 즉시 지급 정지 및 수사 요청을 위해 연락해야 합니다.',
      urgency: 'HIGH'
    })

    // Conditional emergency contacts based on attack vector
    const hasVoice = failedTypes.some(t => t.includes('전화') || t.includes('보이스피싱'))
    const hasSmsOrMail = failedTypes.some(t => t.includes('스미싱') || t.includes('메일') || t.includes('링크'))

    if (hasVoice) {
      contacts.push({
        orgName: '금융감독원 (피해 상담)',
        phoneNumber: '1394', // Using FSS/fraud report consult number requested by user
        description: '의심스러운 대환대출 권유나 수사기관 사칭 전화에 대해 전문 상담 및 피해 신고를 제공합니다.',
        urgency: 'MEDIUM'
      })
    }

    if (hasSmsOrMail) {
      contacts.push({
        orgName: '한국인터넷진흥원 (KISA)',
        phoneNumber: '118',
        description: '스미싱 링크 클릭으로 인한 악성 앱 유출, 번호 변작, 스팸 피해 신고 및 대처법 안내.',
        urgency: 'HIGH'
      })
    }

    // Default backup
    if (contacts.length < 3) {
      contacts.push({
        orgName: '인터넷진흥원 해킹·스팸신고센터',
        phoneNumber: '118',
        description: '사이버 보안 침해사고 및 악성 앱 탐지 분석 상담센터.',
        urgency: 'MEDIUM'
      })
    }

    return contacts
  }
}
