import { useTrainingStore } from '../state/trainingStore'
import { scenarioApi } from '../api/scenarioApi'

let eventSource: EventSource | null = null

export const callConnectionService = {
  connect(userId: string = 'demo_user') {
    if (eventSource) {
      eventSource.close()
    }
    
    const backendUrl = `/api/v1/calls/connect?userId=${userId}`
    console.log(`[SSE] Connecting to: ${backendUrl}`)
    
    eventSource = new EventSource(backendUrl)
    
    eventSource.onopen = () => {
      console.log('[SSE] Connection opened successfully')
    }
    
    eventSource.onerror = (err) => {
      console.error('[SSE] Connection error/closed:', err)
    }
    
    eventSource.addEventListener('incoming-call', async (event: MessageEvent) => {
      console.log('[SSE] Received incoming call:', event.data)
      try {
        const payload = JSON.parse(event.data)
        const store = useTrainingStore()
        
        const scenarios = await scenarioApi.getScenarios()
        const scenario = scenarios.find(s => s.id === payload.scenarioId) || scenarios[0]
        
        // Start ringing simulation
        store.startSimulation(scenario)
        this.reportProgress('RINGING', 0)
      } catch (e) {
        console.error('[SSE] Failed to process incoming call payload:', e)
      }
    })
  },
  
  disconnect() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
  },
  
  playRingtone() {
    // No-op for PPT demo
  },
  
  stopRingtone() {
    // No-op for PPT demo
  },
  
  async reportProgress(status: string, currentStep: number, userId: string = 'demo_user') {
    try {
      const response = await fetch(`/api/v1/calls/progress?userId=${userId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ status, currentStep })
      })
      if (!response.ok) {
        console.warn('[SSE] Progress report returned non-OK status:', response.status)
      }
    } catch (e) {
      console.error('[SSE] Failed to report call progress to backend:', e)
    }
  }
}
