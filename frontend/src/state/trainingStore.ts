// 5-layer architecture: State Layer for Training Simulation
// Manages the active phishing scenario run and warning trigger states

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { type Scenario } from '../api/scenarioApi'

export type SimStatus = 'IDLE' | 'RINGING' | 'CONNECTED' | 'SMS_RECEIVED' | 'EMAIL_OPENED' | 'WARNING_SCREEN' | 'CALL_REPORT'

export const useTrainingStore = defineStore('training', () => {
  const scenarios = ref<Scenario[]>([])
  const activeScenario = ref<Scenario | null>(null)
  const simStatus = ref<SimStatus>('IDLE')
  const currentStepIndex = ref(0)
  const isMuted = ref(false)
  const warningMessage = ref('')

  const setScenarios = (list: Scenario[]) => {
    scenarios.value = list
  }

  const startSimulation = (scenario: Scenario) => {
    activeScenario.value = scenario
    currentStepIndex.value = 0
    warningMessage.value = ''
    
    if (scenario.type === 'VOICE') {
      simStatus.value = 'RINGING'
    } else if (scenario.type === 'SMS') {
      simStatus.value = 'SMS_RECEIVED'
    } else {
      simStatus.value = 'EMAIL_OPENED'
    }
  }

  const acceptCall = () => {
    simStatus.value = 'CONNECTED'
  }

  const setStepIndex = (index: number) => {
    currentStepIndex.value = index
  }

  const setSimStatus = (status: SimStatus) => {
    simStatus.value = status
  }

  const triggerWarning = (explanation: string) => {
    warningMessage.value = explanation
    simStatus.value = 'WARNING_SCREEN'
  }

  const stopSimulation = () => {
    activeScenario.value = null
    simStatus.value = 'IDLE'
    currentStepIndex.value = 0
  }

  return {
    scenarios,
    activeScenario,
    simStatus,
    currentStepIndex,
    isMuted,
    warningMessage,
    setScenarios,
    startSimulation,
    acceptCall,
    setStepIndex,
    setSimStatus,
    triggerWarning,
    stopSimulation
  }
})
