<script setup lang="ts">
import { ref, watch, onUnmounted, computed, onMounted } from 'vue'
import { useTrainingStore } from '../state/trainingStore'
import { trainingService } from '../service/trainingService'
import { useReportStore } from '../state/reportStore'
import { trainingApi } from '../api/trainingApi'
import { callConnectionService } from '../service/callConnectionService'

const store = useTrainingStore()
const reportStore = useReportStore()

// 발신인 데이터 분리 및 사칭 표시 정제 (현실적인 피싱 훈련을 위해)
const parsedCaller = computed(() => {
  const sender = store.activeScenario?.sender || ''
  if (!sender) return { name: '알 수 없음', phone: '010-5851-3110' }
  
  const lines = sender.split('\n').map(l => l.trim())
  let phone = ''
  let name = ''
  
  if (lines.length >= 2) {
    phone = lines[0] // 예: 02-1301-XXXX
    name = lines[1]  // 예: (서울중앙지검 사칭)
  } else {
    const phonePattern = /^[0-9-X]{7,15}$/i
    if (phonePattern.test(sender)) {
      phone = sender
      name = '알 수 없는 번호'
    } else {
      name = sender
      phone = '010-5851-3110'
    }
  }
  
  // "(사칭)" 등의 텍스트는 수신 화면에서 가려 피싱 훈련 효과를 극대화
  if (name.includes('사칭')) {
    name = name.replace(/\s*\(|\)|사칭/g, '').trim()
  }
  
  return { name, phone }
})

// Call duration timer
const duration = ref(0)
let timerId: any = null

// Speaker/Mute state
const isMutedLocal = ref(false)
const isSpeakerOn = ref(false)

const activeAnalysis = ref<any>(null)
const activeAudio = ref<HTMLAudioElement | null>(null)

let responseEventSource: EventSource | null = null
interface AudioQueueItem {
  text: string
  audio: HTMLAudioElement
}
let audioQueue: AudioQueueItem[] = []
let isAudioPlaying = false
let isSseCompleted = false

let silenceTimer: any = null
let silenceNudgeCount = 0

const clearSilenceTimer = () => {
  if (silenceTimer) {
    console.log('[Silence Timer] Clearing active silence timer.')
    clearTimeout(silenceTimer)
    silenceTimer = null
  }
}

const startSilenceTimer = () => {
  clearSilenceTimer()
  if (store.simStatus !== 'CONNECTED' || isAttackerSpeaking.value || !isSseCompleted) {
    console.log('[Silence Timer] Guard active: Skipping timer start as attacker is speaking or stream is active.')
    return
  }
  console.log('[Silence Timer] Starting 3-second countdown for user response...')
  silenceTimer = setTimeout(() => {
    handleSilence()
  }, 3000)
}

const handleSilence = async () => {
  if (store.simStatus !== 'CONNECTED') return
  silenceNudgeCount++
  if (silenceNudgeCount === 1) {
    console.log('[Silence Handler] 1st silence timeout (3s). Sending normal nudge request to LLM.')
    handleUserSpeechInput('(대답 없음)')
  } else {
    console.log(`[Silence Handler] ${silenceNudgeCount}th silence timeout. Sending coercive nudge request to LLM.`)
    handleUserSpeechInput('(대답 없음 - 매우 강압적이고 다급하며 위협적인 어조로 상대방을 강하게 압박하고 독촉)')
  }
}

const stopActiveAudio = () => {
  clearSilenceTimer()
  if (responseEventSource) {
    console.log('[SSE LLM] Closing active response SSE.')
    try {
      responseEventSource.close()
    } catch (e) {
      console.error(e)
    }
    responseEventSource = null
  }
  
  // Stop and release all preloaded audio objects to cancel background network requests
  audioQueue.forEach(item => {
    try {
      item.audio.pause()
      item.audio.src = ''
      item.audio.load()
    } catch (e) {
      // ignore
    }
  })
  audioQueue = []
  isAudioPlaying = false
  isSseCompleted = false

  if (activeAudio.value) {
    console.log('[Audio] Stopping active speech audio.')
    try {
      activeAudio.value.pause()
      activeAudio.value.src = ''
      activeAudio.value.load()
    } catch (e) {
      console.error(e)
    }
    activeAudio.value = null
  }
}

// Web Speech API states
let recognition: any = null
const isListening = ref(false)
const userSpokenText = ref('')
const isAttackerSpeaking = ref(false)
const actuallyHeardText = ref('') // AI의 대사 중 실제 스피커로 출력된 부분 추적
const textInput = ref('')
const dynamicDialogue = ref('')

const reportInterruption = async (lastHeard: string) => {
  const scenario = store.activeScenario
  if (!scenario) return
  
  console.log('[Interruption] Reporting to backend. Last heard:', lastHeard)
  try {
    await fetch(`/api/v1/calls/interrupt?userId=demo_user&scenarioId=${encodeURIComponent(scenario.id)}&lastHeardText=${encodeURIComponent(lastHeard)}`, {
      method: 'POST'
    })
  } catch (e) {
    console.error('[Interruption] Failed to report interruption:', e)
  }
}

const submitTextInput = () => {
  const text = textInput.value.trim()
  if (!text) return
  
  textInput.value = ''
  userSpokenText.value = text
  console.log('[Text Input Submitted]:', text)
  handleUserSpeechInput(text)
}

const formatDuration = (sec: number) => {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const currentVoiceStepIndex = computed(() => store.currentStepIndex)

const currentVoiceStep = computed(() => {
  const scenario = store.activeScenario
  if (!scenario || !scenario.steps) return null
  const idx = currentVoiceStepIndex.value
  return scenario.steps[idx] || scenario.steps[scenario.steps.length - 1]
})


// STT (Speech Recognition) Initialization
const initSTT = () => {
  const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  if (!SpeechRecognition) {
    console.warn('STT (Speech Recognition) is not supported in this browser.')
    return
  }
  
  recognition = new SpeechRecognition()
  recognition.continuous = false
  recognition.interimResults = true
  recognition.lang = 'ko-KR'
  
  recognition.onstart = () => {
    isListening.value = true
    userSpokenText.value = '듣고 있습니다...'
  }
  
  recognition.onresult = (event: any) => {
    let interimTranscript = ''
    let finalTranscript = ''
    
    for (let i = event.resultIndex; i < event.results.length; ++i) {
      if (event.results[i].isFinal) {
        finalTranscript += event.results[i][0].transcript
      } else {
        interimTranscript += event.results[i][0].transcript
      }
    }
    
    const transcript = finalTranscript || interimTranscript
    if (transcript) {
      userSpokenText.value = transcript
    }
    
    if (finalTranscript) {
      console.log('[STT Final Result]:', finalTranscript)
      handleUserSpeechInput(finalTranscript)
    }
  }
  
  recognition.onerror = (event: any) => {
    console.error('[STT error]', event.error)
    isListening.value = false
  }
  
  recognition.onend = () => {
    isListening.value = false
  }
}

const startSTT = () => {
  if (recognition && !isListening.value && store.simStatus === 'CONNECTED') {
    try {
      recognition.start()
    } catch (e) {
      console.error('Failed to start STT:', e)
    }
  }
}

const stopSTT = () => {
  if (recognition && isListening.value) {
    try {
      recognition.stop()
    } catch (e) {
      console.error('Failed to stop STT:', e)
    }
  }
}

// Synthesize and play Attacker Speech (Local ChatTTS stream integration)
const triggerAttackerSpeech = () => {
  const step = currentVoiceStep.value
  if (!step) return

  stopSTT()
  userSpokenText.value = ''
  stopActiveAudio()
  
  isAttackerSpeaking.value = true
  
  const dialogueToPlay = dynamicDialogue.value || step.dialogue
  
  // Call backend streaming endpoint (/api/v1/calls/stream?text=...)
  const streamUrl = `/api/v1/calls/stream?text=${encodeURIComponent(dialogueToPlay)}`
  console.log(`[TTS Audio Request] Playing text: "${dialogueToPlay}" via URL: ${streamUrl}`)
  
  const audioObj = new Audio(streamUrl)
  activeAudio.value = audioObj
  
  audioObj.play().catch(e => {
    console.warn('[Audio Playback Blocked/Failed] Fallback to simulated reading duration.', e)
    const textLength = dialogueToPlay.length
    const speechDuration = Math.min(Math.max(textLength * 80, 1500), 5000)
    setTimeout(() => {
      if (isAttackerSpeaking.value) {
        isAttackerSpeaking.value = false
        startSTT()
        startSilenceTimer()
      }
    }, speechDuration)
  })

  audioObj.onended = () => {
    console.log('[TTS Speech Ended] Normal ending. Activating user listener.')
    isAttackerSpeaking.value = false
    activeAudio.value = null
    startSTT()
    startSilenceTimer()
  }

  audioObj.onerror = (err) => {
    console.error('[TTS Audio Error] Audio stream error. Falling back.', err)
    isAttackerSpeaking.value = false
    activeAudio.value = null
    startSTT()
    startSilenceTimer()
  }
}

// Logic to evaluate spoken words
const handleUserSpeechInput = async (spokenText: string) => {
  const scenario = store.activeScenario
  if (!scenario) return

  const step = currentVoiceStep.value
  if (!step) return

  if (spokenText !== '(대답 없음)' && !spokenText.includes('(대답 없음')) {
    silenceNudgeCount = 0
  }

  userSpokenText.value = `[포착: ${spokenText.replace(/\(.*\)/,'').trim() || '침묵'}] => 대화 진행 중...`
  const nextStep = Math.min(store.currentStepIndex + 1, scenario.steps.length - 1)
  store.setStepIndex(nextStep)
  callConnectionService.reportProgress('CONNECTED', nextStep)

  // PHASE 1: 인터럽트 발생 시 서버에 보고하여 컨텍스트 롤백 수행
  if (isAttackerSpeaking.value && isAudioPlaying) {
    const lastHeard = actuallyHeardText.value.trim()
    if (lastHeard) {
      await reportInterruption(lastHeard)
    }
  }

  // Reset audio queue state and stop any existing audio
  stopActiveAudio()
  clearSilenceTimer()
  
  isAttackerSpeaking.value = true
  dynamicDialogue.value = ''
  actuallyHeardText.value = '' // 초기화
  isSseCompleted = false
  isAudioPlaying = false
  audioQueue = []

  const playNextAudio = () => {
    if (audioQueue.length === 0) {
      if (isSseCompleted) {
        console.log('[Audio Queue] Finished playing all streamed chunks. Attacker speech truly ended.')
        isAttackerSpeaking.value = false
        activeAudio.value = null
        startSTT()
        startSilenceTimer()
      }
      return
    }

    isAudioPlaying = true
    const item = audioQueue.shift()!
    activeAudio.value = item.audio

    console.log(`[TTS Streaming] Playing chunk: "${item.text}"`)

    item.audio.play().catch(e => {
      console.warn('[Audio Queue Playback Blocked] Using fallback reading timer.', e)
      const speechDuration = Math.min(Math.max(item.text.length * 85, 1500), 4500)
      setTimeout(() => {
        actuallyHeardText.value += (actuallyHeardText.value ? ' ' : '') + item.text
        isAudioPlaying = false
        playNextAudio()
      }, speechDuration)
    })

    item.audio.onended = () => {
      actuallyHeardText.value += (actuallyHeardText.value ? ' ' : '') + item.text
      isAudioPlaying = false
      playNextAudio()
    }

    item.audio.onerror = (err) => {
      console.error('[TTS Audio Queue Error] Failed to play chunk. Skipping.', err)
      isAudioPlaying = false
      playNextAudio()
    }
  }

  const sseUrl = `/api/v1/calls/respond?userId=demo_user&scenarioId=${encodeURIComponent(scenario.id)}&text=${encodeURIComponent(spokenText)}`
  console.log('[SSE LLM] Connecting to stream:', sseUrl)
  
  const es = new EventSource(sseUrl)
  responseEventSource = es

  es.addEventListener('chunk', (event: any) => {
    try {
      const data = JSON.parse(event.data)
      const chunkText = data.text
      console.log('[SSE LLM Chunk]:', chunkText)

      // Append text
      if (dynamicDialogue.value) {
        dynamicDialogue.value += ' ' + chunkText
      } else {
        dynamicDialogue.value = chunkText
      }

      // Preload audio as soon as text chunk is received
      const streamUrl = `/api/v1/calls/stream?text=${encodeURIComponent(chunkText)}`
      const audioObj = new Audio(streamUrl)
      audioObj.preload = 'auto' // Browser begins downloading / backend begins generating TTS

      audioQueue.push({
        text: chunkText,
        audio: audioObj
      })

      if (!isAudioPlaying) {
        playNextAudio()
      }
    } catch (err) {
      console.error('Failed to parse SSE chunk:', err)
    }
  })

  es.addEventListener('complete', (event: any) => {
    console.log('[SSE LLM Complete] Finished stream:', event.data)
    isSseCompleted = true
    es.close()
    if (responseEventSource === es) {
      responseEventSource = null
    }
    // If audio is not playing and queue is empty, finish
    if (!isAudioPlaying && audioQueue.length === 0) {
      isAttackerSpeaking.value = false
      activeAudio.value = null
      startSTT()
      startSilenceTimer()
    }
  })

  es.onerror = (err) => {
    console.warn('[SSE LLM Connection Error]', err)
    isSseCompleted = true
    es.close()
    if (responseEventSource === es) {
      responseEventSource = null
    }
    if (dynamicDialogue.value === '') {
      // 로봇 대사 삭제. 침묵 타이머가 작동하여 AI가 재촉하도록 유도.
    }
    if (!isAudioPlaying) {
      playNextAudio()
    }
  }
}

// Direct choice selection (button fallbacks - commented out as unused in text-input flow)
/*
const handleChoiceDirectly = async (choiceIndex: number) => {
  const scenario = store.activeScenario
  if (!scenario) return

  stopSTT()

  if (store.currentStepIndex < scenario.steps.length - 1) {
    if (choiceIndex === 1) {
      // Hung up / Suspicious
      await recordSuccessHangUp(store.currentStepIndex)
    } else {
      // Accept / Continue
      const nextStep = store.currentStepIndex + 1
      store.setStepIndex(nextStep)
      callConnectionService.reportProgress('CONNECTED', nextStep)
      setTimeout(() => {
        triggerAttackerSpeech()
      }, 500)
    }
  } else {
    // Last step
    if (choiceIndex === 0) {
      // Sent money
      await recordFailedCall()
    } else {
      // Refused
      await recordSuccessHangUp(store.currentStepIndex)
    }
  }
}
*/

const recordSuccessHangUp = async (stepIndex: number) => {
  const scenario = store.activeScenario
  if (!scenario) return

  stopActiveAudio()

  if (timerId) {
    clearInterval(timerId)
    timerId = null
  }

  const time = duration.value
  const status = stepIndex === 0 ? 'SAFE' : 'WARNING'
  const result = 'SUCCESS'

  const stage = scenario.stageDetails?.[stepIndex] || {
    stageName: `${stepIndex + 1}단계`,
    techniqueName: '피싱 수법',
    techniqueDesc: '피싱 전화를 분석하고 수신을 거절하거나 조기 차단함.',
    vulnerabilityExplanation: {
      SAFE: '통화를 빠르게 종료하여 위협 노출을 피했습니다.',
      WARNING: '통화를 일정 시간 유지했으나 중간에 끊었습니다.',
      CRITICAL: '송금 또는 최종 이체 위협 단계까지 들었습니다.'
    },
    feedback: {
      SAFE: '사칭 전화는 즉시 차단하십시오.',
      WARNING: '앞으로는 의심 징후가 있을 때 더 빨리 종료하십시오.',
      CRITICAL: '송금 요구나 원격 설치 요구는 100% 사기입니다.'
    }
  }

  const stepName = stage.stageName
  const explanation = stage.vulnerabilityExplanation[status]
  const feedback = stage.feedback[status]

  const techniques = scenario.stageDetails?.map(d => ({
    step: `${d.stepIndex + 1}단계`,
    name: d.techniqueName,
    desc: d.techniqueDesc
  })) || [
    { step: '1단계', name: '공공기관 사칭 및 접촉', desc: '의심을 낮추고 본인 신원을 확인합니다.' },
    { step: '2단계', name: '사법 절차 및 약정 위반 협박', desc: '심리적 공포 및 당황 상태를 조성합니다.' },
    { step: '3단계', name: '자산 송금 압박', desc: '임시 계좌로의 자금 이체를 강요합니다.' }
  ]

  const analysis = {
    date: new Date().toLocaleDateString('ko-KR', { month: '2-digit', day: '2-digit' }).replace('. ', '-').replace('.', ''),
    scenarioId: scenario.id,
    scenarioTitle: scenario.title,
    scenarioType: scenario.type,
    duration: time,
    hangUpStepIndex: stepIndex,
    hangUpStepName: stepName,
    vulnerabilityStatus: status,
    vulnerabilityExplanation: explanation,
    feedback: feedback,
    techniques: techniques,
    result: result
  }

  await trainingApi.recordLog({
    scenarioId: scenario.id,
    actionType: 'HUNG_UP_SUCCESS',
    callDurationSeconds: time,
    riskyBehaviorDetected: false
  })

  reportStore.addHistoryItem({
    title: scenario.title,
    result: result,
    scenarioId: scenario.id,
    scenarioType: 'VOICE',
    duration: time,
    hangUpStepIndex: stepIndex,
    hangUpStepName: stepName,
    vulnerabilityStatus: status,
    vulnerabilityExplanation: explanation,
    feedback: feedback,
    techniques: techniques
  })

  activeAnalysis.value = analysis
  store.setSimStatus('CALL_REPORT')
  callConnectionService.reportProgress('IDLE', stepIndex)
}

/*
const recordFailedCall = async () => {
  const scenario = store.activeScenario
  if (!scenario) return

  stopActiveAudio()

  if (timerId) {
    clearInterval(timerId)
    timerId = null
  }

  const time = duration.value
  const stepIndex = 2 // Failed at final stage
  const status = 'CRITICAL'
  const result = 'FAILED'

  const stage = scenario.stageDetails?.[stepIndex] || {
    stageName: '3단계 (이체 송금 강요)',
    techniqueName: '자산 이체 압박',
    techniqueDesc: '피싱 사기범의 임시 가상 계좌 송금 압박에 굴함.',
    vulnerabilityExplanation: {
      SAFE: '이체를 거부하고 전화를 끊었습니다.',
      WARNING: '전화를 도중에 끊어 피해를 피했습니다.',
      CRITICAL: '보이스피싱의 최종 단계인 임시 계좌 자금 송금 요구 및 구속 위협까지 전화를 끊지 않고 지속하여 실제 금융 피해로 이어질 가능성이 매우 큽니다.'
    },
    feedback: {
      SAFE: '잘 대처하셨습니다.',
      WARNING: '조기에 통화를 차단하십시오.',
      CRITICAL: '수사기관이나 금융감독원은 어떤 경우에도 전화상으로 돈을 보내라고 하지 않습니다. 이 요구가 나온 시점까지 들으셨다면 위험에 극도로 노출된 상태입니다.'
    }
  }

  const stepName = stage.stageName
  const explanation = stage.vulnerabilityExplanation[status]
  const feedback = stage.feedback[status]

  const techniques = scenario.stageDetails?.map(d => ({
    step: `${d.stepIndex + 1}단계`,
    name: d.techniqueName,
    desc: d.techniqueDesc
  })) || [
    { step: '1단계', name: '공공기관 사칭 및 접촉', desc: '의심을 낮추고 본인 신원을 확인합니다.' },
    { step: '2단계', name: '사법 절차 및 약정 위반 협박', desc: '심리적 공포 및 당황 상태를 조성합니다.' },
    { step: '3단계', name: '자산 송금 압박', desc: '임시 계좌로의 자금 이체를 강요합니다.' }
  ]

  const analysis = {
    date: new Date().toLocaleDateString('ko-KR', { month: '2-digit', day: '2-digit' }).replace('. ', '-').replace('.', ''),
    scenarioId: scenario.id,
    scenarioTitle: scenario.title,
    scenarioType: scenario.type,
    duration: time,
    hangUpStepIndex: stepIndex,
    hangUpStepName: stepName,
    vulnerabilityStatus: status,
    vulnerabilityExplanation: explanation,
    feedback: feedback,
    techniques: techniques,
    result: result
  }

  await trainingApi.recordLog({
    scenarioId: scenario.id,
    actionType: 'ENTERED_DATA',
    callDurationSeconds: time,
    riskyBehaviorDetected: true
  })

  reportStore.addHistoryItem({
    title: scenario.title,
    result: result,
    scenarioId: scenario.id,
    scenarioType: 'VOICE',
    duration: time,
    hangUpStepIndex: stepIndex,
    hangUpStepName: stepName,
    vulnerabilityStatus: status,
    vulnerabilityExplanation: explanation,
    feedback: feedback,
    techniques: techniques
  })

  activeAnalysis.value = analysis
  store.setSimStatus('CALL_REPORT')
  callConnectionService.reportProgress('WARNING_SCREEN', stepIndex)
}
*/

onMounted(() => {
  initSTT()
})

watch(() => store.simStatus as any, (newStatus: any) => {
  if (newStatus === 'CONNECTED') {
    duration.value = 0
    dynamicDialogue.value = ''
    if (timerId) clearInterval(timerId)
    timerId = setInterval(() => {
      duration.value++
    }, 1000)
    
    // Begin scammer conversation with delay
    setTimeout(() => {
      triggerAttackerSpeech()
    }, 800)
    
  } else if (newStatus !== 'RINGING' && newStatus !== 'CONNECTED' && newStatus !== 'CALL_REPORT') {
    if (timerId) {
      clearInterval(timerId)
      timerId = null
    }
    stopSTT()
    stopActiveAudio()
  }
})

onUnmounted(() => {
  if (timerId) clearInterval(timerId)
  stopSTT()
  stopActiveAudio()
})

const handleDecline = async () => {
  if (store.simStatus === 'RINGING') {
    const scenario = store.activeScenario
    if (scenario) {
      reportStore.addHistoryItem({
        title: scenario.title,
        result: 'SUCCESS',
        scenarioId: scenario.id,
        scenarioType: 'VOICE',
        duration: 0,
        hangUpStepIndex: 0,
        hangUpStepName: '통화 수신 거절',
        vulnerabilityStatus: 'SAFE',
        vulnerabilityExplanation: '전화를 아예 수신하지 않고 바로 거절하여 보이스피싱 공격을 원천적으로 차단했습니다.',
        feedback: '모르는 번호의 전화를 수신 거절하는 것은 가장 확실하고 즉각적인 피싱 대응법입니다.',
        techniques: []
      })
      await trainingApi.recordLog({
        scenarioId: scenario.id,
        actionType: 'HUNG_UP_SUCCESS',
        callDurationSeconds: 0,
        riskyBehaviorDetected: false
      })
    }
    trainingService.cancelSimulation()
  } else if (store.simStatus === 'CONNECTED') {
    // User hangs up during call
    await recordSuccessHangUp(store.currentStepIndex)
  }
}

const handleAccept = () => {
  trainingService.acceptCall()
}

const toggleMute = () => {
  isMutedLocal.value = !isMutedLocal.value
}

const toggleSpeaker = () => {
  isSpeakerOn.value = !isSpeakerOn.value
}

const closeReport = () => {
  activeAnalysis.value = null
  duration.value = 0
  trainingService.cancelSimulation()
}
</script>

<template>
  <Transition name="slide-up">
    <div 
      v-if="store.simStatus === 'RINGING' || store.simStatus === 'CONNECTED' || store.simStatus === 'CALL_REPORT'" 
      :class="['absolute inset-x-0 top-[28px] bottom-[40px] z-25 flex flex-col select-none overflow-hidden transition-colors duration-300', store.simStatus === 'CALL_REPORT' ? 'bg-white text-slate-800' : 'bg-slate-950 text-white']"
    >
      <!-- A. RINGING STATE (갤럭시 One UI 수신 전화 화면 테마) -->
      <div 
        v-if="store.simStatus === 'RINGING' && store.activeScenario" 
        class="flex-1 flex flex-col justify-between py-8 px-6 animate-fade-in text-white relative overflow-hidden bg-gradient-to-b from-[#1c1221] via-[#232145] to-[#45372f] h-full"
      >
        <!-- 부드러운 오로라 블러 배경 장식 -->
        <div class="absolute -top-10 -left-10 w-48 h-48 bg-purple-600/25 rounded-full filter blur-3xl z-0"></div>
        <div class="absolute -bottom-10 -right-10 w-48 h-48 bg-orange-600/15 rounded-full filter blur-3xl z-0"></div>
        <div class="absolute bottom-1/3 left-10 w-40 h-40 bg-blue-600/20 rounded-full filter blur-3xl z-0"></div>

        <div class="z-10 flex-1 flex flex-col justify-between h-full">
          <!-- 상단 헤더 정보 -->
          <div class="text-center mt-10 space-y-1">
            <p class="text-[11px] font-semibold text-white/70 tracking-widest">
              수신전화
            </p>
            <h2 class="text-3xl font-bold tracking-tight text-white mt-3 leading-tight">
              {{ parsedCaller.name }}
            </h2>
            <p class="text-sm text-white/80 font-medium pt-1">
              휴대전화 {{ parsedCaller.phone }}
            </p>
          </div>

          <!-- 중간 통화 상태 & 통화 어시스트 캡슐 -->
          <div class="my-auto text-center space-y-5">
            <div class="flex items-center justify-center gap-1.5 text-[11px] text-white/80 font-medium">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-3 h-3 fill-current rotate-90" viewBox="0 0 24 24">
                <path d="M20.01 15.38c-1.23 0-2.42-.2-3.53-.57a.978.978 0 0 0-1.02.24l-2.2 2.2a15.149 15.149 0 0 1-6.59-6.59l2.2-2.2c.28-.28.36-.67.25-1.02A11.36 11.36 0 0 0 8.5 3.99c0-.55-.45-1-1-1H4.01c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.61c0-.55-.45-1-1-1z"/>
              </svg>
              <span>마지막 통화: 일요일</span>
            </div>

            <!-- 통화 어시스트 캡슐 버튼 (반투명 라운드 뱃지) -->
            <div class="inline-flex items-center gap-1.5 bg-white/10 backdrop-blur-lg border border-white/10 rounded-full px-5 py-2 text-xs font-semibold text-white shadow-xs hover:bg-white/15 transition-all">
              <span>✨</span>
              <span>통화 어시스트</span>
            </div>
          </div>

          <!-- 하단 수신/거절 원형 슬라이드 버튼 레이아웃 -->
          <div class="flex items-center justify-between w-full px-8 mb-6">
            <!-- 수신 버튼 (Green) -->
            <div class="flex flex-col items-center gap-2 relative">
              <div class="p-1.5 bg-white/5 border border-white/5 rounded-full shadow-inner relative flex items-center justify-center">
                <!-- 은은한 핑(Ping) 효과 레이어 -->
                <div class="absolute w-16 h-16 rounded-full bg-[#00d69b]/40 animate-ping duration-1000 z-0"></div>
                
                <button 
                  @click="handleAccept"
                  class="w-16 h-16 rounded-full bg-[#00d69b] hover:bg-[#00c28d] flex items-center justify-center shadow-lg transition-all duration-150 active:scale-90 z-10"
                >
                  <!-- Accept Phone Icon -->
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 fill-white" viewBox="0 0 24 24">
                    <path d="M20 15.5c-1.25 0-2.45-.2-3.57-.57a1.02 1.02 0 0 0-1.02.24l-2.2 2.2a15.045 15.045 0 0 1-6.59-6.59l2.2-2.2c.28-.28.36-.67.25-1.02A11.36 11.36 0 0 1 8.5 4c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.5c0-.55-.45-1-1-1z"/>
                  </svg>
                </button>
              </div>
            </div>

            <!-- 거절 버튼 (Red) -->
            <div class="flex flex-col items-center gap-2">
              <div class="p-1.5 bg-white/5 border border-white/5 rounded-full shadow-inner">
                <button 
                  @click="handleDecline"
                  class="w-16 h-16 rounded-full bg-[#ff5b5b] hover:bg-[#eb5252] flex items-center justify-center shadow-lg transition-all duration-150 active:scale-90"
                >
                  <!-- Decline Phone Icon (수신 수화기를 rotate-[135deg] 회전시켜 엎어놓은 형태) -->
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 fill-white rotate-[135deg]" viewBox="0 0 24 24">
                    <path d="M20 15.5c-1.25 0-2.45-.2-3.57-.57a1.02 1.02 0 0 0-1.02.24l-2.2 2.2a15.045 15.045 0 0 1-6.59-6.59l2.2-2.2c.28-.28.36-.67.25-1.02A11.36 11.36 0 0 1 8.5 4c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.5c0-.55-.45-1-1-1z"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <!-- 최하단 메시지 보내기 (이중 제스처 바 제거) -->
          <div class="text-center pb-4">
            <span class="text-xs font-semibold text-white/70 block hover:text-white/90 cursor-pointer">
              메시지 보내기
            </span>
          </div>
        </div>
      </div>

      <!-- B. CONNECTED ACTIVE CALL STATE -->
      <div v-else-if="store.simStatus === 'CONNECTED' && store.activeScenario" class="flex-1 flex flex-col justify-between py-6 xs:py-12 px-6 animate-fade-in">
        
        <!-- Active Call Header -->
        <div class="text-center mt-6 xs:mt-12 space-y-1">
          <h2 class="text-2xl xs:text-3xl font-bold tracking-tight text-white">
            {{ parsedCaller.name }}
          </h2>
          <p class="text-xs text-white/60 font-medium">
            {{ parsedCaller.phone }}
          </p>
          <p class="text-sm text-emerald-400 font-extrabold tracking-widest mt-1 uppercase">
            {{ formatDuration(duration) }}
          </p>
        </div>

        <!-- Live Caption Banner (iOS style semi-transparent card) -->
        <div v-if="currentVoiceStep && false" class="w-full max-w-xs mx-auto bg-white/10 backdrop-blur-md border border-white/10 rounded-2xl p-4 my-2 text-left space-y-1.5 shadow-[0_8px_32px_rgba(0,0,0,0.37)] animate-fade-in">
          <div class="flex items-center gap-1.5">
            <span class="w-1.5 h-1.5 rounded-full bg-blue-400 animate-pulse"></span>
            <span class="text-[9px] font-extrabold text-blue-400 uppercase tracking-widest">실시간 통화 자막 피드</span>
          </div>
          <p class="text-xs font-semibold text-slate-100 leading-relaxed">
            "{{ currentVoiceStep?.dialogue }}"
          </p>
        </div>

        <!-- User Voice Input & Recognition Feed -->
        <div v-if="false && (isListening || userSpokenText)" class="w-full max-w-xs mx-auto bg-emerald-500/10 backdrop-blur-md border border-emerald-500/20 rounded-2xl p-4 my-2 text-left space-y-1.5 shadow-[0_8px_32px_rgba(0,0,0,0.15)] animate-fade-in">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-1.5">
              <span class="relative flex h-2 w-2">
                <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span class="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
              </span>
              <span class="text-[9px] font-extrabold text-emerald-400 uppercase tracking-widest">내 음성 실시간 인식</span>
            </div>
            <span class="text-[9px] text-emerald-300/80 font-bold bg-emerald-500/20 px-1.5 py-0.5 rounded">
              {{ isListening ? '마이크 켜짐' : '인식 완료' }}
            </span>
          </div>
          <p class="text-xs font-semibold text-emerald-100 leading-relaxed italic">
            {{ userSpokenText || '대답을 기다리는 중...' }}
          </p>
        </div>

        <!-- Real-time Text Interaction Input -->
        <div v-if="currentVoiceStep" class="w-full max-w-xs mx-auto my-3 space-y-2 animate-fade-in">
          <p class="text-[10px] text-slate-400 font-extrabold tracking-wider text-center uppercase">
            💬 답변 입력 (텍스트로 대화하기)
          </p>
          <div class="flex items-center gap-2 bg-white/10 backdrop-blur-md border border-white/10 rounded-2xl p-1.5 px-3 shadow-[0_4px_30px_rgba(0,0,0,0.1)]">
            <input 
              v-model="textInput"
              @keyup.enter="submitTextInput"
              type="text" 
              placeholder="대답을 입력하세요..." 
              class="flex-1 bg-transparent text-xs text-white placeholder-slate-400 focus:outline-none py-1.5 px-1"
            />
            <button 
              @click="submitTextInput"
              class="bg-emerald-600 hover:bg-emerald-500 active:scale-95 text-white text-[11px] font-bold py-1.5 px-3.5 rounded-xl transition-all"
            >
              전송
            </button>
          </div>
        </div>

        <!-- Phone Grid Actions (Mock iOS Style) -->
        <div class="grid grid-cols-3 gap-y-4 xs:gap-y-8 gap-x-4 xs:gap-x-8 max-w-xs mx-auto my-auto text-center">
          <button @click="toggleMute" :class="['flex flex-col items-center justify-center w-16 h-16 xs:w-20 xs:h-20 mx-auto rounded-full transition-all', isMutedLocal ? 'bg-white text-slate-950 shadow-lg' : 'bg-white/10 hover:bg-white/20 text-white']">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6 xs:w-7 xs:h-7 fill-current" viewBox="0 0 24 24">
              <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z"/>
            </svg>
            <span class="text-[11px] xs:text-[13px] font-bold mt-1">소리 끔</span>
          </button>
          <button class="flex flex-col items-center justify-center w-16 h-16 xs:w-20 xs:h-20 mx-auto bg-white/10 hover:bg-white/20 text-white rounded-full opacity-40 cursor-not-allowed">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6 xs:w-7 xs:h-7 fill-current" viewBox="0 0 24 24">
              <circle cx="6" cy="5" r="2"/>
              <circle cx="12" cy="5" r="2"/>
              <circle cx="18" cy="5" r="2"/>
              <circle cx="6" cy="12" r="2"/>
              <circle cx="12" cy="12" r="2"/>
              <circle cx="18" cy="12" r="2"/>
              <circle cx="6" cy="19" r="2"/>
              <circle cx="12" cy="19" r="2"/>
              <circle cx="18" cy="19" r="2"/>
            </svg>
            <span class="text-[11px] xs:text-[13px] font-bold mt-1">키패드</span>
          </button>
          <button @click="toggleSpeaker" :class="['flex flex-col items-center justify-center w-16 h-16 xs:w-20 xs:h-20 mx-auto rounded-full transition-all', isSpeakerOn ? 'bg-white text-slate-950 shadow-lg' : 'bg-white/10 hover:bg-white/20 text-white']">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6 xs:w-7 xs:h-7 fill-current" viewBox="0 0 24 24">
              <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/>
            </svg>
            <span class="text-[11px] xs:text-[13px] font-bold mt-1">스피커</span>
          </button>
          <button class="flex flex-col items-center justify-center w-16 h-16 xs:w-20 xs:h-20 mx-auto bg-white/10 hover:bg-white/20 text-white rounded-full opacity-40 cursor-not-allowed">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6 xs:w-7 xs:h-7 fill-current" viewBox="0 0 24 24">
              <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
            </svg>
            <span class="text-[11px] xs:text-[13px] font-bold mt-1">통화 추가</span>
          </button>
          <button class="flex flex-col items-center justify-center w-16 h-16 xs:w-20 xs:h-20 mx-auto bg-white/10 hover:bg-white/20 text-white rounded-full opacity-40 cursor-not-allowed">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6 xs:w-7 xs:h-7 fill-current" viewBox="0 0 24 24">
              <path d="M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z"/>
            </svg>
            <span class="text-[11px] xs:text-[13px] font-bold mt-1">FaceTime</span>
          </button>
          <button class="flex flex-col items-center justify-center w-16 h-16 xs:w-20 xs:h-20 mx-auto bg-white/10 hover:bg-white/20 text-white rounded-full opacity-40 cursor-not-allowed">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6 xs:w-7 xs:h-7 fill-current" viewBox="0 0 24 24">
              <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
            </svg>
            <span class="text-[11px] xs:text-[13px] font-bold mt-1">연락처</span>
          </button>
        </div>

        <!-- Hang Up Button -->
        <div class="flex flex-col items-center mb-4 xs:mb-8">
          <button 
            @click="handleDecline"
            class="w-16 h-16 xs:w-20 xs:h-20 rounded-full bg-rose-600 hover:bg-rose-500 flex items-center justify-center text-2xl xs:text-3xl shadow-[0_4px_25px_rgba(225,29,72,0.5)] transition-all active:scale-90"
          >
            <!-- Decline Phone Icon -->
            <svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 xs:w-8 xs:h-8 fill-white rotate-[135deg]" viewBox="0 0 24 24">
              <path d="M21 16.5c0 .38-.21.71-.53.88l-4.87 2.44c-.38.19-.85.12-1.15-.18l-3.38-3.38c-.3-.3-.37-.77-.18-1.15l2.44-4.87c.17-.32.5-.53.88-.53h4.75c.55 0 1 .45 1 1v5.74zM3 7.5c0-.55.45-1 1-1h4.75c.38 0 .71.21.88.53l2.44 4.87c.19.38.12.85-.18 1.15l-3.38 3.38c-.3.3-.77.37-1.15.18L4.53 14c-.32-.17-.53-.5-.53-.88V7.5z"/>
            </svg>
          </button>
          <span class="text-xs xs:text-sm text-slate-300 font-bold mt-2">통화 종료</span>
        </div>

      </div>

      <!-- C. CALL REPORT STATE -->
      <div v-else-if="store.simStatus === 'CALL_REPORT' && activeAnalysis" class="flex-1 flex flex-col justify-between py-6 px-6 overflow-hidden animate-fade-in text-slate-800">
        
        <!-- Header -->
        <div class="text-center pb-4 border-b border-slate-100">
          <h3 class="text-sm font-bold text-slate-800">보이스피싱 모의 훈련 보고서</h3>
          <p class="text-xs text-slate-450 mt-1">실시간 대처 시간을 분석한 안심 진단표입니다.</p>
        </div>

        <!-- Scrollable Report Contents -->
        <div class="flex-1 overflow-y-auto my-4 pr-1 space-y-4 text-xs scroll-container">
          
          <!-- Scenario Info Card -->
          <div class="bg-slate-50 border border-slate-200/60 rounded-2xl p-4 space-y-2">
            <div class="flex justify-between items-center text-xs text-slate-450 font-bold">
              <span class="flex items-center gap-1">
                <span class="w-1.5 h-1.5 rounded-full bg-blue-500"></span>
                훈련 유형: 보이스피싱
              </span>
              <span>{{ activeAnalysis.date }}</span>
            </div>
            <h4 class="text-sm font-bold text-slate-800 mt-1">{{ activeAnalysis.scenarioTitle.replace('\n', ' ') }}</h4>
            
            <!-- 통화 유지 시간 & 위험 노출 게이지 바 -->
            <div class="pt-2 border-t border-slate-200/50">
              <div class="flex items-center justify-between text-xs text-slate-500 font-bold">
                <div class="flex items-center gap-1.5">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                    <circle cx="12" cy="12" r="10"/>
                    <polyline points="12 6 12 12 16 12"/>
                  </svg>
                  <span>통화 유지 시간: <strong class="text-slate-800">{{ formatDuration(activeAnalysis.duration) }}</strong></span>
                </div>
                <span class="text-[10px]" :class="activeAnalysis.duration < 10 ? 'text-emerald-600' : activeAnalysis.duration < 30 ? 'text-amber-600' : 'text-rose-600'">
                  {{ activeAnalysis.duration < 10 ? '우수' : activeAnalysis.duration < 30 ? '미흡' : '위험' }}
                </span>
              </div>
              
              <!-- 게이지 바 디자인 -->
              <div class="mt-2 space-y-1">
                <div class="w-full bg-slate-200/80 h-1.5 rounded-full overflow-hidden">
                  <div 
                    :class="[
                      'h-full rounded-full transition-all duration-500',
                      activeAnalysis.duration < 10 ? 'bg-emerald-500' :
                      activeAnalysis.duration < 30 ? 'bg-amber-500' : 'bg-rose-500'
                    ]"
                    :style="{ width: Math.min((activeAnalysis.duration / 60) * 100, 100) + '%' }"
                  ></div>
                </div>
                <div class="flex justify-between text-[8px] text-slate-400 font-black uppercase tracking-wider">
                  <span>안전 종료 (10초내)</span>
                  <span>위험 전조 (30초)</span>
                  <span>피해 위험 (60초+)</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Vulnerability Rating -->
          <div 
            :class="[
              'border rounded-2xl p-5 text-center space-y-1.5 shadow-xs transition-all duration-300',
              activeAnalysis.vulnerabilityStatus === 'SAFE' ? 'bg-emerald-50/80 border-emerald-200 text-emerald-800' :
              activeAnalysis.vulnerabilityStatus === 'WARNING' ? 'bg-amber-50/80 border-amber-200 text-amber-800' :
              'bg-rose-50/80 border-rose-200 text-rose-800'
            ]"
          >
            <p class="text-[11px] font-extrabold uppercase tracking-widest opacity-75">취약 진단 등급</p>
            <p class="text-2xl font-black mt-1 tracking-tight">
              {{ 
                activeAnalysis.vulnerabilityStatus === 'SAFE' ? '안전 (즉각 대처)' : 
                activeAnalysis.vulnerabilityStatus === 'WARNING' ? '주의 (경고 노출)' : 
                '위험 (송금 피해 고위험)' 
              }}
            </p>
            <p class="text-xs font-bold mt-1 opacity-70">
              {{ activeAnalysis.hangUpStepName }} 에서 통화 종료
            </p>
          </div>

          <!-- Analysis Explanation -->
          <div class="space-y-1.5">
            <h5 class="font-bold text-slate-700 text-xs flex items-center gap-1.5">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <circle cx="11" cy="11" r="8"/>
                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
              대처 취약점 분석
            </h5>
            <div class="bg-slate-50/50 border border-slate-200/80 rounded-2xl p-3.5 text-slate-650 leading-relaxed font-semibold text-xs border-l-4 border-l-slate-400">
              {{ activeAnalysis.vulnerabilityExplanation }}
            </div>
          </div>

          <!-- Prevention Advice / Feedback -->
          <div class="space-y-1.5">
            <h5 class="font-bold text-slate-700 text-xs flex items-center gap-1.5">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <path d="M15 14c.2-.2.4-.4.6-.7C16.8 11.7 17 10 15.8 8.3 14.5 6.7 12.2 6 10.5 6.8c-1.7.8-2.5 2.6-2.5 4.5 0 .9.2 1.8.8 2.5.2.3.4.5.6.7.3.3.5.7.5 1.1v1.5c0 .3.2.5.5.5h4c.3 0 .5-.2.5-.5v-1.5c0-.4.2-.8.5-1.1z"/>
                <line x1="9" y1="18" x2="15" y2="18"/>
                <line x1="10" y1="21" x2="14" y2="21"/>
              </svg>
              안전 대응 가이드
            </h5>
            <div class="bg-slate-50/50 border border-slate-200/80 rounded-2xl p-3.5 text-slate-650 leading-relaxed font-semibold text-xs">
              <p class="mb-3">{{ activeAnalysis.feedback }}</p>
              
              <!-- 긴급 번호 버튼/다이얼 뱃지화 고도화 -->
              <div class="border-t border-slate-200 pt-3 mt-3 space-y-2 text-xs">
                <div class="flex justify-between items-center text-slate-500">
                  <span class="font-medium">피해 발생 신고 (경찰청)</span>
                  <a href="tel:112" class="bg-white hover:bg-slate-50 text-slate-800 font-extrabold px-2.5 py-1.2 rounded-lg border border-slate-200 flex items-center gap-1 shadow-2xs active:scale-95 transition-all">
                    <svg xmlns="http://www.w3.org/2000/svg" class="w-3 h-3 fill-slate-500" viewBox="0 0 24 24">
                      <path d="M6.62 10.79a15.15 15.15 0 0 0 6.59 6.59l2.2-2.2a1 1 0 0 1 1.11-.27 11.36 11.36 0 0 0 3.58 1.1 1 1 0 0 1 .89 1v3.58a1 1 0 0 1-1 1A16 16 0 0 1 3 4a1 1 0 0 1 1-1h3.58a1 1 0 0 1 1 .89 11.36 11.36 0 0 0 1.1 3.58 1 1 0 0 1-.27 1.11z"/>
                    </svg>
                    <span>112</span>
                  </a>
                </div>
                <div class="flex justify-between items-center text-slate-500">
                  <span class="font-medium">피해 의심 상담 (금융감독원)</span>
                  <a href="tel:1332" class="bg-white hover:bg-slate-50 text-slate-800 font-extrabold px-2.5 py-1.2 rounded-lg border border-slate-200 flex items-center gap-1 shadow-2xs active:scale-95 transition-all">
                    <svg xmlns="http://www.w3.org/2000/svg" class="w-3 h-3 fill-slate-500" viewBox="0 0 24 24">
                      <path d="M6.62 10.79a15.15 15.15 0 0 0 6.59 6.59l2.2-2.2a1 1 0 0 1 1.11-.27 11.36 11.36 0 0 0 3.58 1.1 1 1 0 0 1 .89 1v3.58a1 1 0 0 1-1 1A16 16 0 0 1 3 4a1 1 0 0 1 1-1h3.58a1 1 0 0 1 1 .89 11.36 11.36 0 0 0 1.1 3.58 1 1 0 0 1-.27 1.11z"/>
                    </svg>
                    <span>1332</span>
                  </a>
                </div>
                <div class="flex justify-between items-center text-slate-500">
                  <span class="font-medium">스팸/번호변작 제보 (KISA)</span>
                  <a href="tel:118" class="bg-white hover:bg-slate-50 text-slate-800 font-extrabold px-2.5 py-1.2 rounded-lg border border-slate-200 flex items-center gap-1 shadow-2xs active:scale-95 transition-all">
                    <svg xmlns="http://www.w3.org/2000/svg" class="w-3 h-3 fill-slate-500" viewBox="0 0 24 24">
                      <path d="M6.62 10.79a15.15 15.15 0 0 0 6.59 6.59l2.2-2.2a1 1 0 0 1 1.11-.27 11.36 11.36 0 0 0 3.58 1.1 1 1 0 0 1 .89 1v3.58a1 1 0 0 1-1 1A16 16 0 0 1 3 4a1 1 0 0 1 1-1h3.58a1 1 0 0 1 1 .89 11.36 11.36 0 0 0 1.1 3.58 1 1 0 0 1-.27 1.11z"/>
                    </svg>
                    <span>118</span>
                  </a>
                </div>
              </div>
            </div>
          </div>

          <!-- Phishing Techniques Timeline (수직선 잇는 타임라인 레이아웃으로 변경) -->
          <div class="space-y-3">
            <h5 class="font-bold text-slate-700 text-xs flex items-center gap-1.5">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <line x1="18" y1="20" x2="18" y2="10"/>
                <line x1="12" y1="20" x2="12" y2="4"/>
                <line x1="6" y1="20" x2="6" y2="14"/>
              </svg>
              보이스피싱 시나리오 단계별 구성
            </h5>
            
            <div class="relative pl-6 space-y-4 before:absolute before:left-3 before:top-2 before:bottom-2 before:w-[2px] before:bg-slate-200">
              <div 
                v-for="(tech, tIdx) in activeAnalysis.techniques" 
                :key="tIdx"
                :class="[
                  'relative p-3.5 rounded-2xl border transition-all text-left flex gap-3 shadow-2xs',
                  tIdx === activeAnalysis.hangUpStepIndex 
                    ? 'bg-blue-50 border-blue-200 text-slate-800 border-l-[5px] border-l-blue-600' 
                    : 'bg-slate-50/40 border-slate-100 opacity-60 text-slate-500'
                ]"
              >
                <!-- 타임라인 불릿 (Dot) -->
                <div 
                  :class="[
                    'absolute -left-[23px] top-4.5 w-3.5 h-3.5 rounded-full border-2 flex items-center justify-center text-[7px] font-black z-10',
                    tIdx === activeAnalysis.hangUpStepIndex 
                      ? 'bg-blue-600 border-blue-200 text-white ring-4 ring-blue-100/50' 
                      : 'bg-slate-200 border-white text-slate-500'
                  ]"
                >
                </div>
                
                <div class="space-y-1 flex-1">
                  <div class="flex items-center gap-1.5">
                    <h6 class="font-extrabold text-xs" :class="tIdx === activeAnalysis.hangUpStepIndex ? 'text-slate-800' : 'text-slate-655'">{{ tech.name }}</h6>
                    <span v-if="tIdx === activeAnalysis.hangUpStepIndex" class="text-[8px] bg-blue-500 text-white font-bold px-1.5 py-0.2 rounded-md shadow-2xs">
                      종료 지점
                    </span>
                  </div>
                  <p class="text-[11px] leading-relaxed" :class="tIdx === activeAnalysis.hangUpStepIndex ? 'text-slate-600' : 'text-slate-450'">{{ tech.desc }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Privacy/Non-collection Notice -->
          <p class="text-[10px] text-slate-400 text-center leading-relaxed mt-4">
            ※ 본 훈련은 통화 음성 데이터 및 개인정보를 수집하거나 서버로 전송하지 않으며, 시나리오 진행도와 전화 끊기 타이밍만을 활용하여 기재된 안심 분석 리포트입니다.
          </p>

        </div>

        <!-- Confirm Button -->
        <button 
          @click="closeReport"
          class="w-full bg-blue-600 hover:bg-blue-700 active:scale-[0.99] transition-all text-white font-bold text-xs py-3.5 px-4 rounded-xl shadow-[0_4px_20px_rgba(37,99,235,0.15)] mt-2"
        >
          리포트 닫기 및 훈련 종료
        </button>

      </div>

    </div>
  </Transition>
</template>

<style scoped>
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.animate-fade-in {
  animation: fadeIn 0.3s ease-out forwards;
}

@keyframes softPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
    box-shadow: 0 4px 25px rgba(16, 185, 129, 0.5);
  }
  50% {
    transform: scale(1.05);
    opacity: 0.92;
    box-shadow: 0 4px 35px rgba(16, 185, 129, 0.75);
  }
}
.animate-soft-pulse {
  animation: softPulse 2s infinite ease-in-out;
}

/* Custom Scrollbar for Report */
.scroll-container {
  scrollbar-width: thin;
}
.scroll-container::-webkit-scrollbar {
  width: 4px;
}
.scroll-container::-webkit-scrollbar-track {
  background: transparent;
}
.scroll-container::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 4px;
}
.scroll-container::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.25);
}
</style>
