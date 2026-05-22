// 5-layer architecture: API Layer for Scenario Generation
// Fetches scenarios from backend (no local mock fallbacks)

export interface StageDetail {
  stepIndex: number
  stageName: string
  techniqueName: string
  techniqueDesc: string
  vulnerabilityExplanation: {
    SAFE: string
    WARNING: string
    CRITICAL: string
  }
  feedback: {
    SAFE: string
    WARNING: string
    CRITICAL: string
  }
}

export interface SmsEmailReport {
  success: {
    vulnerabilityExplanation: string
    feedback: string
  }
  failed: {
    vulnerabilityExplanation: string
    feedback: string
  }
  techniques: { step: string; name: string; desc: string }[]
}

export interface Scenario {
  id: string
  type: 'VOICE' | 'SMS' | 'EMAIL'
  title: string
  sender: string
  content: string
  attackerAction: string
  steps: {
    dialogue: string
    isAttacker: boolean
    options?: string[]
  }[]
  warningExplanation: string
  stageDetails?: StageDetail[]
  smsEmailReport?: SmsEmailReport
}

export const scenarioApi = {
  async getScenarios(): Promise<Scenario[]> {
    console.log('[API GET] Fetching scenarios from backend...')
    const response = await fetch('/api/v1/scenarios')
    if (!response.ok) {
      throw new Error(`Backend scenarios API returned non-OK status: ${response.status}`)
    }
    const data = await response.json()
    console.log('Successfully fetched scenarios from backend:', data)
    return data as Scenario[]
  }
}
