// 5-layer architecture: Service Layer for Simulation
// Manages the orchestration of phishing simulation sessions and recording user actions

import { scenarioApi, type Scenario } from '../api/scenarioApi'
import { trainingApi } from '../api/trainingApi'
import { useTrainingStore } from '../state/trainingStore'
import { useReportStore } from '../state/reportStore'
import { callConnectionService } from './callConnectionService'

export const trainingService = {
  async loadScenarios(): Promise<void> {
    const store = useTrainingStore()
    try {
      const list = await scenarioApi.getScenarios()
      store.setScenarios(list)
    } catch (error) {
      console.error('Failed to load training scenarios:', error)
    }
  },

  startSimulation(scenario: Scenario): void {
    const store = useTrainingStore()
    store.startSimulation(scenario)
    callConnectionService.reportProgress(store.simStatus, 0)
  },

  acceptCall(): void {
    const store = useTrainingStore()
    store.acceptCall()
    callConnectionService.stopRingtone()
    callConnectionService.reportProgress('CONNECTED', 0)
  },

  async handleUserChoice(choiceIndex: number): Promise<void> {
    const store = useTrainingStore()
    const reportStore = useReportStore()
    const scenario = store.activeScenario
    if (!scenario) return

    // Voice scenario handling
    if (scenario.type === 'VOICE') {
      // If the user choose the risky path (usually index 0 in the mock steps)
      if (store.currentStepIndex === scenario.steps.length - 1) {
        if (choiceIndex === 0) {
          // Risky behavior (e.g. transferring money)
          await trainingApi.recordLog({
            scenarioId: scenario.id,
            actionType: 'ENTERED_DATA',
            callDurationSeconds: 45,
            riskyBehaviorDetected: true
          })
          reportStore.addHistoryItem({ title: scenario.title, result: 'FAILED' })
          store.triggerWarning(scenario.warningExplanation)
          callConnectionService.stopRingtone()
          callConnectionService.reportProgress('WARNING_SCREEN', store.currentStepIndex)
        } else {
          // Safe action (suspicious, hang up)
          await trainingApi.recordLog({
            scenarioId: scenario.id,
            actionType: 'HUNG_UP_SUCCESS',
            callDurationSeconds: 45,
            riskyBehaviorDetected: false
          })
          reportStore.addHistoryItem({ title: scenario.title, result: 'SUCCESS' })
          store.setSimStatus('IDLE')
          callConnectionService.stopRingtone()
          callConnectionService.reportProgress('IDLE', store.currentStepIndex)
        }
      } else {
        // Advance script
        if (choiceIndex === 1 && store.currentStepIndex === 0) {
          // Hung up immediately
          await trainingApi.recordLog({
            scenarioId: scenario.id,
            actionType: 'HUNG_UP_SUCCESS',
            callDurationSeconds: 5,
            riskyBehaviorDetected: false
          })
          reportStore.addHistoryItem({ title: scenario.title, result: 'SUCCESS' })
          store.setSimStatus('IDLE')
          callConnectionService.stopRingtone()
          callConnectionService.reportProgress('IDLE', store.currentStepIndex)
        } else {
          const nextStep = store.currentStepIndex + 1
          store.setStepIndex(nextStep)
          callConnectionService.reportProgress('CONNECTED', nextStep)
        }
      }
    } else {
      // SMS / Email scenario handling
      if (choiceIndex === 0) {
        // Clicked fake link
        await trainingApi.recordLog({
          scenarioId: scenario.id,
          actionType: 'CLICK_LINK',
          riskyBehaviorDetected: true
        })
        reportStore.addHistoryItem({ title: scenario.title, result: 'FAILED' })
        store.triggerWarning(scenario.warningExplanation)
      } else {
        // Ignored or flagged as spam
        await trainingApi.recordLog({
          scenarioId: scenario.id,
          actionType: 'BLOCKED_SMS',
          riskyBehaviorDetected: false
        })
        reportStore.addHistoryItem({ title: scenario.title, result: 'SUCCESS' })
        store.setSimStatus('IDLE')
      }
    }
  },

  // Direct mock actions for UI triggers
  async simulateClickLink(): Promise<void> {
    const store = useTrainingStore()
    const reportStore = useReportStore()
    const scenario = store.activeScenario
    if (!scenario) return

    await trainingApi.recordLog({
      scenarioId: scenario.id,
      actionType: 'CLICK_LINK',
      riskyBehaviorDetected: true
    })
    reportStore.addHistoryItem({ title: scenario.title, result: 'FAILED' })
    store.triggerWarning(scenario.warningExplanation)
  },

  async simulateInputCredentials(): Promise<void> {
    const store = useTrainingStore()
    const reportStore = useReportStore()
    const scenario = store.activeScenario
    if (!scenario) return

    await trainingApi.recordLog({
      scenarioId: scenario.id,
      actionType: 'ENTERED_DATA',
      riskyBehaviorDetected: true
    })
    reportStore.addHistoryItem({ title: scenario.title, result: 'FAILED' })
    store.triggerWarning(scenario.warningExplanation)
  },

  async ignoreTraining(): Promise<void> {
    const store = useTrainingStore()
    const reportStore = useReportStore()
    const scenario = store.activeScenario
    if (!scenario) return

    await trainingApi.recordLog({
      scenarioId: scenario.id,
      actionType: 'BLOCKED_SMS',
      riskyBehaviorDetected: false
    })
    reportStore.addHistoryItem({ title: scenario.title, result: 'SUCCESS' })
    store.setSimStatus('IDLE')
  },

  cancelSimulation(): void {
    const store = useTrainingStore()
    callConnectionService.stopRingtone()
    callConnectionService.reportProgress('IDLE', store.currentStepIndex)
    store.stopSimulation()
  }
}
