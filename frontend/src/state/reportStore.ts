// 5-layer architecture: State Layer for Reports
// Manages the vulnerability reports state using Pinia

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { type MonthlyReport } from '../api/trainingApi'

export const useReportStore = defineStore('report', () => {
  const report = ref<MonthlyReport | null>(null)
  const isLoading = ref(false)

  const setReport = (data: MonthlyReport) => {
    report.value = data
  }

  const setLoading = (loading: boolean) => {
    isLoading.value = loading
  }

  const addHistoryItem = (item: any) => {
    if (report.value) {
      const now = new Date()
      const dateStr = `${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
      
      report.value.history.unshift({
        date: dateStr,
        title: item.title,
        result: item.result,
        scenarioId: item.scenarioId,
        scenarioType: item.scenarioType,
        duration: item.duration,
        hangUpStepIndex: item.hangUpStepIndex,
        hangUpStepName: item.hangUpStepName,
        vulnerabilityStatus: item.vulnerabilityStatus,
        vulnerabilityExplanation: item.vulnerabilityExplanation,
        feedback: item.feedback,
        techniques: item.techniques
      })
      
      // Update ratios
      report.value.totalSimulations++
      if (item.result === 'FAILED') {
        report.value.riskyActionsCount++
      }
      report.value.vulnerabilityRate = Math.round(
        (report.value.riskyActionsCount / report.value.totalSimulations) * 1000
      ) / 10
    }
  }

  return {
    report,
    isLoading,
    setReport,
    setLoading,
    addHistoryItem
  }
})
