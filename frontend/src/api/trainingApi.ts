// 5-layer architecture: API Layer for Phishing Training Logs
// Records user simulation results and fetches monthly statistics (no local mock fallbacks)

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

export const trainingApi = {
  async recordLog(payload: LogPayload): Promise<{ success: boolean }> {
    console.log('[API POST] Sending user action log to backend:', payload)
    const response = await fetch('/api/v1/training/log?userId=demo_user', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })
    if (!response.ok) {
      throw new Error(`Backend training log recording returned non-OK status: ${response.status}`)
    }
    const data = await response.json()
    console.log('Successfully recorded training log on backend:', data)
    return data as { success: boolean }
  },

  async getMonthlyReport(): Promise<MonthlyReport> {
    console.log('[API GET] Fetching monthly report from backend...')
    const response = await fetch('/api/v1/reports/monthly?userId=demo_user')
    if (!response.ok) {
      throw new Error(`Backend monthly report API returned non-OK status: ${response.status}`)
    }
    const data = await response.json()
    console.log('Successfully fetched monthly report from backend:', data)
    return data as MonthlyReport
  }
}
