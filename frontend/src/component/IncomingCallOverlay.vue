<script setup lang="ts">
import { ref, watch, onUnmounted, computed, onMounted } from 'vue'
import { useTrainingStore } from '../state/trainingStore'
import { trainingService } from '../service/trainingService'
import { useReportStore } from '../state/reportStore'
import { trainingApi } from '../api/trainingApi'
import { callConnectionService } from '../service/callConnectionService'

const store = useTrainingStore()
const reportStore = useReportStore()

// Call duration timer
const duration = ref(0)
let timerId: any = null

// Speaker/Mute state
const isMutedLocal = ref(false)
const isSpeakerOn = ref(false)

const activeAnalysis = ref<any>(null)
const activeAudio = ref<HTMLAudioElement | null>(null)

const stopActiveAudio = () => {
  if (activeAudio.value) {
    console.log('[Audio] Stopping active speech audio.')
    activeAudio.value.pause()
    activeAudio.value = null
  }
}

// Web Speech API states
let recognition: any = null
const isListening = ref(false)
const userSpokenText = ref('')
const isAttackerSpeaking = ref(false)

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
  
  // Call backend streaming endpoint (/api/v1/calls/stream?text=...)
  const streamUrl = `/api/v1/calls/stream?text=${encodeURIComponent(step.dialogue)}`
  console.log(`[TTS Audio Request] Playing text: "${step.dialogue}" via URL: ${streamUrl}`)
  
  const audioObj = new Audio(streamUrl)
  activeAudio.value = audioObj
  
  audioObj.play().catch(e => {
    console.warn('[Audio Playback Blocked/Failed] Fallback to simulated reading duration.', e)
    const textLength = step.dialogue.length
    const speechDuration = Math.min(Math.max(textLength * 80, 1500), 5000)
    setTimeout(() => {
      if (isAttackerSpeaking.value) {
        isAttackerSpeaking.value = false
        startSTT()
      }
    }, speechDuration)
  })

  audioObj.onended = () => {
    console.log('[TTS Speech Ended] Normal ending. Activating user listener.')
    isAttackerSpeaking.value = false
    activeAudio.value = null
    startSTT()
  }

  audioObj.onerror = (err) => {
    console.error('[TTS Audio Error] Audio stream error. Falling back.', err)
    isAttackerSpeaking.value = false
    activeAudio.value = null
    startSTT()
  }
}

// Logic to evaluate spoken words
const handleUserSpeechInput = async (spokenText: string) => {
  const scenario = store.activeScenario
  if (!scenario) return

  const step = currentVoiceStep.value
  if (!step) return

  // Detect negative/suspicious keywords (SAFE actions)
  const isSuspicious = /사기|사칭|의심|끊어|피싱|경찰|검찰청|금감원|아닌가|신고|아니요|못 믿/i.test(spokenText)
  // Detect positive/compliance keywords (CRITICAL actions)
  const isAccepting = /네|맞아|맞습|예|할게|계좌|알겠|이체|송금|알려주/i.test(spokenText)

  if (store.currentStepIndex < scenario.steps.length - 1) {
    if (isSuspicious) {
      userSpokenText.value = `[포착: ${spokenText}] => 위험을 감지하여 전화를 끊습니다.`
      await recordSuccessHangUp(store.currentStepIndex)
    } else {
      userSpokenText.value = `[포착: ${spokenText}] => 대화 진행 중...`
      const nextStep = store.currentStepIndex + 1
      store.setStepIndex(nextStep)
      callConnectionService.reportProgress('CONNECTED', nextStep)
      setTimeout(() => {
        triggerAttackerSpeech()
      }, 1000)
    }
  } else {
    // Final step (pressure transfer)
    if (isAccepting) {
      userSpokenText.value = `[포착: ${spokenText}] => 금융사기 피해 의심 동작 수행.`
      await recordFailedCall()
    } else if (isSuspicious) {
      userSpokenText.value = `[포착: ${spokenText}] => 마지막 단계에서 안전 종료.`
      await recordSuccessHangUp(store.currentStepIndex)
    } else {
      // General response on final step defaults to warning/failed if the user didn't hang up
      userSpokenText.value = `[포착: ${spokenText}] => 통화 지속으로 최종 노출.`
      await recordFailedCall()
    }
  }
}

// Direct choice selection (button fallbacks)
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

onMounted(() => {
  initSTT()
})

watch(() => store.simStatus as any, (newStatus: any) => {
  if (newStatus === 'CONNECTED') {
    duration.value = 0
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
      :class="['absolute inset-0 z-50 flex flex-col select-none overflow-hidden transition-colors duration-300', store.simStatus === 'CALL_REPORT' ? 'bg-white text-slate-800' : 'bg-slate-950 text-white']"
    >
      <!-- A. RINGING STATE -->
      <div v-if="store.simStatus === 'RINGING' && store.activeScenario" class="flex-1 flex flex-col justify-between py-6 xs:py-12 px-6 animate-fade-in">
        
        <!-- Caller Info Header -->
        <div class="text-center mt-6 xs:mt-12 space-y-2 xs:space-y-3">
          <p class="text-xs xs:text-sm text-blue-400 font-extrabold uppercase tracking-widest animate-pulse">
            훈련 전화 수신 중
          </p>
          <h2 class="text-2xl xs:text-3xl font-black tracking-tight text-white mt-1 whitespace-pre-line">
            {{ store.activeScenario.sender }}
          </h2>
          <p class="text-sm xs:text-base text-slate-300 font-semibold whitespace-pre-line">
            {{ store.activeScenario.title }}
          </p>
        </div>

        <!-- Pulse Ring Phone Graphic -->
        <div class="flex items-center justify-center my-4 xs:my-6">
          <div class="relative flex items-center justify-center w-28 h-28 xs:w-36 xs:h-36">
            <div class="absolute inset-0 rounded-full bg-blue-500/10 animate-ping duration-1000"></div>
            <div class="absolute inset-4 rounded-full bg-blue-500/20 animate-pulse"></div>
            <div class="relative w-14 h-14 xs:w-20 xs:h-20 rounded-full bg-slate-900 border border-slate-800 flex items-center justify-center shadow-xl">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 xs:w-9 xs:h-9 fill-slate-100" viewBox="0 0 24 24">
                <path d="M20 15.5c-1.25 0-2.45-.2-3.57-.57a1.02 1.02 0 0 0-1.02.24l-2.2 2.2a15.045 15.045 0 0 1-6.59-6.59l2.2-2.2c.28-.28.36-.67.25-1.02A11.36 11.36 0 0 1 8.5 4c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.5c0-.55-.45-1-1-1z"/>
              </svg>
            </div>
          </div>
        </div>

        <!-- Ringing Actions -->
        <div class="flex items-center justify-between w-full px-6 xs:px-14 mb-4 xs:mb-8">
          <!-- Accept Column (Left) -->
          <div class="flex flex-col items-center gap-4 xs:gap-6">
            <!-- Remind Me Button -->
            <button class="flex flex-col items-center gap-1.5 active:scale-95 transition-all text-slate-400 hover:text-slate-350">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 12" />
              </svg>
              <span class="text-[10px] font-semibold">나중에 보기</span>
            </button>

            <!-- Accept Button -->
            <div class="flex flex-col items-center gap-1.5 xs:gap-2">
              <button 
                @click="handleAccept"
                class="w-16 h-16 xs:w-20 xs:h-20 rounded-full bg-emerald-500 hover:bg-emerald-400 flex items-center justify-center text-2xl xs:text-3xl shadow-[0_4px_25px_rgba(16,185,129,0.5)] animate-soft-pulse transition-all active:scale-90"
              >
                <!-- Accept Phone Icon -->
                <svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 xs:w-9 xs:h-9 fill-white" viewBox="0 0 24 24">
                  <path d="M20 15.5c-1.25 0-2.45-.2-3.57-.57a1.02 1.02 0 0 0-1.02.24l-2.2 2.2a15.045 15.045 0 0 1-6.59-6.59l2.2-2.2c.28-.28.36-.67.25-1.02A11.36 11.36 0 0 1 8.5 4c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.5c0-.55-.45-1-1-1z"/>
                </svg>
              </button>
              <span class="text-xs xs:text-sm text-slate-300 font-bold mt-1">응답</span>
            </div>
          </div>

          <!-- Decline Column (Right) -->
          <div class="flex flex-col items-center gap-4 xs:gap-6">
            <!-- Message Button -->
            <button class="flex flex-col items-center gap-1.5 active:scale-95 transition-all text-slate-400 hover:text-slate-350">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
              <span class="text-[10px] font-semibold">메시지 보내기</span>
            </button>

            <!-- Decline Button -->
            <div class="flex flex-col items-center gap-1.5 xs:gap-2">
              <button 
                @click="handleDecline"
                class="w-16 h-16 xs:w-20 xs:h-20 rounded-full bg-rose-600 hover:bg-rose-500 flex items-center justify-center text-2xl xs:text-3xl shadow-[0_4px_25px_rgba(225,29,72,0.5)] transition-all active:scale-90"
              >
                <!-- Decline Phone Icon -->
                <svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 xs:w-9 xs:h-9 fill-white rotate-[135deg]" viewBox="0 0 24 24">
                  <path d="M21 16.5c0 .38-.21.71-.53.88l-4.87 2.44c-.38.19-.85.12-1.15-.18l-3.38-3.38c-.3-.3-.37-.77-.18-1.15l2.44-4.87c.17-.32.5-.53.88-.53h4.75c.55 0 1 .45 1 1v5.74zM3 7.5c0-.55.45-1 1-1h4.75c.38 0 .71.21.88.53l2.44 4.87c.19.38.12.85-.18 1.15l-3.38 3.38c-.3.3-.77.37-1.15.18L4.53 14c-.32-.17-.53-.5-.53-.88V7.5z"/>
                </svg>
              </button>
              <span class="text-xs xs:text-sm text-slate-300 font-bold mt-1">거절</span>
            </div>
          </div>
        </div>

      </div>

      <!-- B. CONNECTED ACTIVE CALL STATE -->
      <div v-else-if="store.simStatus === 'CONNECTED' && store.activeScenario" class="flex-1 flex flex-col justify-between py-6 xs:py-12 px-6 animate-fade-in">
        
        <!-- Active Call Header -->
        <div class="text-center mt-6 xs:mt-12 space-y-1 xs:space-y-2">
          <h2 class="text-2xl xs:text-3xl font-black tracking-tight text-white whitespace-pre-line">
            {{ store.activeScenario.sender }}
          </h2>
          <p class="text-sm xs:text-base text-emerald-400 font-extrabold tracking-widest uppercase">
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

        <!-- Dialog Options & Helper Buttons -->
        <div v-if="currentVoiceStep && currentVoiceStep.options && currentVoiceStep.options.length" class="w-full max-w-xs mx-auto my-2 space-y-2 animate-fade-in">
          <p class="text-[10px] text-slate-400 font-extrabold tracking-wider text-center uppercase">
            🗣️ 답변 가이드 (말씀하시거나 직접 클릭하여 진행)
          </p>
          <div class="flex flex-col gap-2">
            <button 
              v-for="(option, idx) in currentVoiceStep.options" 
              :key="idx"
              @click="handleChoiceDirectly(idx)"
              :class="[
                'w-full py-2 xs:py-2.5 px-4 rounded-xl text-xs font-bold text-left transition-all border shadow-sm active:scale-[0.98]',
                idx === 1 
                  ? 'bg-rose-500/10 hover:bg-rose-500/20 border-rose-500/30 text-rose-200 hover:text-rose-100' 
                  : 'bg-white/10 hover:bg-white/15 border-white/10 text-slate-200 hover:text-white'
              ]"
            >
              <div class="flex items-start gap-2">
                <span class="text-[9px] px-1.5 py-0.5 rounded bg-black/30 text-slate-300 font-black mt-0.5 flex-shrink-0">
                  {{ idx === 0 ? '진행' : '대처' }}
                </span>
                <span class="leading-normal">{{ option }}</span>
              </div>
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
              <span>훈련 유형: 보이스피싱</span>
              <span>{{ activeAnalysis.date }}</span>
            </div>
            <h4 class="text-sm font-bold text-slate-855 mt-1">{{ activeAnalysis.scenarioTitle.replace('\n', ' ') }}</h4>
            <div class="flex items-center gap-1.5 text-xs text-slate-500 font-bold mt-1">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 12"/>
              </svg>
              <span>통화 유지 시간: {{ formatDuration(activeAnalysis.duration) }}초</span>
            </div>
          </div>

          <!-- Vulnerability Rating -->
          <div 
            :class="[
              'border rounded-2xl p-4 text-center space-y-1',
              activeAnalysis.vulnerabilityStatus === 'SAFE' ? 'bg-emerald-50 border-emerald-200 text-emerald-700' :
              activeAnalysis.vulnerabilityStatus === 'WARNING' ? 'bg-amber-50 border-amber-200 text-amber-700' :
              'bg-rose-50 border-rose-200 text-rose-700'
            ]"
          >
            <p class="text-xs font-black uppercase tracking-wider">취약 진단 등급</p>
            <p class="text-sm font-extrabold mt-1">
              {{ 
                activeAnalysis.vulnerabilityStatus === 'SAFE' ? '안전 (즉각 대처)' : 
                activeAnalysis.vulnerabilityStatus === 'WARNING' ? '주의 (경고 노출)' : 
                '위험 (송금 피해 고위험)' 
              }}
            </p>
            <p class="text-xs text-slate-500 font-semibold mt-1">
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
            <div class="bg-slate-50/50 border border-slate-200/80 rounded-2xl p-3.5 text-slate-650 leading-relaxed font-semibold text-xs">
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
              <p class="mb-2.5">{{ activeAnalysis.feedback }}</p>
              <div class="border-t border-slate-200 pt-2.5 mt-2.5 space-y-2 text-xs">
                <div class="flex justify-between items-center text-slate-500">
                  <span>피해 발생 신고 (경찰청)</span>
                  <span class="text-slate-800 font-bold">국번없이 112</span>
                </div>
                <div class="flex justify-between items-center text-slate-500">
                  <span>피해 의심 상담 (금융감독원)</span>
                  <span class="text-slate-800 font-bold">국번없이 1332</span>
                </div>
                <div class="flex justify-between items-center text-slate-500">
                  <span>스팸/번호변작 제보 (인터넷진흥원)</span>
                  <span class="text-slate-800 font-bold">국번없이 118</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Phishing Techniques Timeline -->
          <div class="space-y-2">
            <h5 class="font-bold text-slate-700 text-xs flex items-center gap-1.5">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <line x1="18" y1="20" x2="18" y2="10"/>
                <line x1="12" y1="20" x2="12" y2="4"/>
                <line x1="6" y1="20" x2="6" y2="14"/>
              </svg>
              보이스피싱 시나리오 단계별 구성
            </h5>
            <div class="space-y-2">
              <div 
                v-for="(tech, tIdx) in activeAnalysis.techniques" 
                :key="tIdx"
                :class="[
                  'p-3.5 rounded-2xl border transition-all text-left flex gap-3',
                  tIdx === activeAnalysis.hangUpStepIndex 
                    ? 'bg-blue-50 border-blue-200 ring-1 ring-blue-100 text-slate-800' 
                    : 'bg-slate-50/30 border-slate-100 opacity-60 text-slate-500'
                ]"
              >
                <!-- Badge -->
                <div 
                  :class="[
                    'w-6 h-6 rounded-full flex items-center justify-center text-xs font-black flex-shrink-0 mt-0.5',
                    tIdx === activeAnalysis.hangUpStepIndex ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-500'
                  ]"
                >
                  {{ Number(tIdx) + 1 }}
                </div>
                <div class="space-y-1">
                  <div class="flex items-center gap-1.5">
                    <h6 class="font-bold text-xs" :class="tIdx === activeAnalysis.hangUpStepIndex ? 'text-slate-800' : 'text-slate-655'">{{ tech.name }}</h6>
                    <span v-if="tIdx === activeAnalysis.hangUpStepIndex" class="text-[9px] bg-blue-500 text-white font-bold px-1.5 py-0.2 rounded">
                      종료 지점
                    </span>
                  </div>
                  <p class="text-xs leading-normal" :class="tIdx === activeAnalysis.hangUpStepIndex ? 'text-slate-600' : 'text-slate-400'">{{ tech.desc }}</p>
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
