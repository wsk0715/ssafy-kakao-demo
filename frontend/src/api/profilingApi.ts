// 5-layer architecture: API Layer for Profiling
// Fetches questions and calculates risk profile by calling backend endpoints (no mock logic)

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

export const profilingApi = {
  async getQuestions(): Promise<ProfilingQuestion[]> {
    console.log('[API GET] Fetching profiling questions from backend...')
    const response = await fetch('/api/v1/profiling/questions')
    if (!response.ok) {
      throw new Error(`Failed to fetch profiling questions: ${response.statusText}`)
    }
    const data = await response.json()
    console.log('Successfully fetched profiling questions from backend:', data)
    return data as ProfilingQuestion[]
  },

  async analyzeRisk(answers: Record<string, string>): Promise<ProfilingResult> {
    console.log('[API POST] Sending answers to backend for analysis...', answers)
    const response = await fetch('/api/v1/profiling/analyze?userId=demo_user', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(answers)
    })
    if (!response.ok) {
      throw new Error(`Failed to analyze risk on backend: ${response.statusText}`)
    }
    const data = await response.json()
    console.log('Successfully analyzed and saved risk in backend:', data)
    return data as ProfilingResult
  }
}
