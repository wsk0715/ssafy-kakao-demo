// 5-layer architecture: State Layer for Profiling
// Manages profiling state using Pinia

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { type ProfilingResult, type ProfilingQuestion } from '../api/profilingApi'

export const useProfilingStore = defineStore('profiling', () => {
  const questions = ref<ProfilingQuestion[]>([])
  const answers = ref<Record<string, string>>({})
  const result = ref<ProfilingResult | null>(null)
  const isLoading = ref(false)

  const setQuestions = (qList: ProfilingQuestion[]) => {
    questions.value = qList
  }

  const setAnswer = (questionId: string, optionValue: string) => {
    answers.value = {
      ...answers.value,
      [questionId]: optionValue
    }
  }

  const setResult = (res: ProfilingResult) => {
    result.value = res
  }

  const setLoading = (loading: boolean) => {
    isLoading.value = loading
  }

  const resetProfiling = () => {
    answers.value = {}
    result.value = null
  }

  return {
    questions,
    answers,
    result,
    isLoading,
    setQuestions,
    setAnswer,
    setResult,
    setLoading,
    resetProfiling
  }
})
