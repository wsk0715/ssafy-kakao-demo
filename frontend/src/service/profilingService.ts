// 5-layer architecture: Service Layer for Profiling
// Coordinates loading profiling questions and analyzing profiling survey responses

import { profilingApi } from '../api/profilingApi'
import { useProfilingStore } from '../state/profilingStore'

export const profilingService = {
  async loadQuestions(): Promise<void> {
    const store = useProfilingStore()
    store.setLoading(true)
    try {
      const qList = await profilingApi.getQuestions()
      store.setQuestions(qList)
    } catch (error) {
      console.error('Failed to load profiling questions:', error)
    } finally {
      store.setLoading(false)
    }
  },

  submitAnswer(questionId: string, value: string): void {
    const store = useProfilingStore()
    store.setAnswer(questionId, value)
  },

  async analyzeRiskProfile(): Promise<void> {
    const store = useProfilingStore()
    store.setLoading(true)
    try {
      const res = await profilingApi.analyzeRisk(store.answers)
      store.setResult(res)
    } catch (error) {
      console.error('Failed to analyze risk profile:', error)
    } finally {
      store.setLoading(false)
    }
  },

  reset(): void {
    const store = useProfilingStore()
    store.resetProfiling()
  }
}
