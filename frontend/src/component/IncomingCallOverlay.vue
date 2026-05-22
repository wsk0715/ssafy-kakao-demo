<script setup lang="ts">
import { ref, watch, onUnmounted, computed } from 'vue'
import { useTrainingStore } from '../state/trainingStore'
import { trainingService } from '../service/trainingService'
import { useReportStore } from '../state/reportStore'
import { trainingApi } from '../api/trainingApi'

const store = useTrainingStore()
const reportStore = useReportStore()

// Call duration timer
const duration = ref(0)
let timerId: any = null

// Speaker/Mute state
const isMutedLocal = ref(false)
const isSpeakerOn = ref(false)

const activeAnalysis = ref<any>(null)

const formatDuration = (sec: number) => {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const currentVoiceStepIndex = computed(() => {
  if (duration.value < 10) return 0
  if (duration.value < 25) return 1
  return 2
})

const currentVoiceStep = computed(() => {
  const scenario = store.activeScenario
  if (!scenario || !scenario.steps) return null
  const idx = currentVoiceStepIndex.value
  return scenario.steps[idx] || scenario.steps[scenario.steps.length - 1]
})

const completeCallWithAnalysis = async () => {
  const scenario = store.activeScenario
  if (!scenario) return

  if (timerId) {
    clearInterval(timerId)
    timerId = null
  }

  const time = duration.value
  let stepIndex = currentVoiceStepIndex.value
  let stepName = ''
  let status: 'SAFE' | 'WARNING' | 'CRITICAL' = 'SAFE'
  let explanation = ''
  let feedback = ''
  let result: 'SUCCESS' | 'FAILED' = 'SUCCESS'

  if (time < 10) {
    stepIndex = 0
    status = 'SAFE'
    result = 'SUCCESS'
  } else if (time < 25) {
    stepIndex = 1
    status = 'WARNING'
    result = 'SUCCESS'
  } else {
    stepIndex = 2
    status = 'CRITICAL'
    result = 'FAILED'
  }

  // Retrieve details dynamically from the scenario
  const stage = scenario.stageDetails?.[stepIndex] || {
    stageName: `${stepIndex + 1}단계`,
    techniqueName: '피싱 수법',
    techniqueDesc: '피싱 통화에 지속 대응함.',
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

  stepName = stage.stageName
  explanation = stage.vulnerabilityExplanation[status]
  feedback = stage.feedback[status]

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
    actionType: result === 'SUCCESS' ? 'HUNG_UP_SUCCESS' : 'ENTERED_DATA',
    callDurationSeconds: time,
    riskyBehaviorDetected: status === 'CRITICAL'
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
}

watch(() => store.simStatus as any, (newStatus: any) => {
  if (newStatus === 'CONNECTED') {
    duration.value = 0
    if (timerId) clearInterval(timerId)
    timerId = setInterval(() => {
      duration.value++
    }, 1000)
  } else if (newStatus !== 'RINGING' && newStatus !== 'CONNECTED' && newStatus !== 'CALL_REPORT') {
    if (timerId) {
      clearInterval(timerId)
      timerId = null
    }
  }
})

watch(() => duration.value, async (newVal) => {
  if (newVal >= 45 && store.simStatus === 'CONNECTED') {
    await completeCallWithAnalysis()
  }
})

onUnmounted(() => {
  if (timerId) clearInterval(timerId)
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
    await completeCallWithAnalysis()
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
      class="absolute inset-0 z-50 flex flex-col bg-slate-950 text-white select-none overflow-hidden"
    >
      <!-- A. RINGING STATE -->
      <div v-if="store.simStatus === 'RINGING' && store.activeScenario" class="flex-1 flex flex-col justify-between py-12 px-6 animate-fade-in">
        
        <!-- Caller Info Header -->
        <div class="text-center mt-12 space-y-3">
          <p class="text-sm text-blue-400 font-extrabold uppercase tracking-widest animate-pulse">
            훈련 전화 수신 중
          </p>
          <h2 class="text-3xl font-black tracking-tight text-white mt-1 whitespace-pre-line">
            {{ store.activeScenario.sender }}
          </h2>
          <p class="text-base text-slate-300 font-semibold whitespace-pre-line">
            {{ store.activeScenario.title }}
          </p>
        </div>

        <!-- Pulse Ring Phone Graphic -->
        <div class="flex items-center justify-center my-6">
          <div class="relative flex items-center justify-center w-36 h-36">
            <div class="absolute inset-0 rounded-full bg-blue-500/10 animate-ping duration-1000"></div>
            <div class="absolute inset-4 rounded-full bg-blue-500/20 animate-pulse"></div>
            <div class="relative w-20 h-20 rounded-full bg-slate-900 border border-slate-800 flex items-center justify-center shadow-xl">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-9 h-9 fill-slate-100" viewBox="0 0 24 24">
                <path d="M20 15.5c-1.25 0-2.45-.2-3.57-.57a1.02 1.02 0 0 0-1.02.24l-2.2 2.2a15.045 15.045 0 0 1-6.59-6.59l2.2-2.2c.28-.28.36-.67.25-1.02A11.36 11.36 0 0 1 8.5 4c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.5c0-.55-.45-1-1-1z"/>
              </svg>
            </div>
          </div>
        </div>

        <!-- Ringing Actions -->
        <div class="flex items-center justify-between w-full px-14 mb-8">
          <!-- Accept Column (Left) -->
          <div class="flex flex-col items-center gap-6">
            <!-- Remind Me Button -->
            <button class="flex flex-col items-center gap-1.5 active:scale-95 transition-all text-slate-400 hover:text-slate-350">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 12" />
              </svg>
              <span class="text-[10px] font-semibold">나중에 보기</span>
            </button>

            <!-- Accept Button -->
            <div class="flex flex-col items-center gap-2">
              <button 
                @click="handleAccept"
                class="w-20 h-20 rounded-full bg-emerald-500 hover:bg-emerald-400 flex items-center justify-center text-3xl shadow-[0_4px_25px_rgba(16,185,129,0.5)] animate-soft-pulse transition-all active:scale-90"
              >
                <!-- Accept Phone Icon -->
                <svg xmlns="http://www.w3.org/2000/svg" class="w-9 h-9 fill-white" viewBox="0 0 24 24">
                  <path d="M20 15.5c-1.25 0-2.45-.2-3.57-.57a1.02 1.02 0 0 0-1.02.24l-2.2 2.2a15.045 15.045 0 0 1-6.59-6.59l2.2-2.2c.28-.28.36-.67.25-1.02A11.36 11.36 0 0 1 8.5 4c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.5c0-.55-.45-1-1-1z"/>
                </svg>
              </button>
              <span class="text-sm text-slate-300 font-bold mt-1">응답</span>
            </div>
          </div>

          <!-- Decline Column (Right) -->
          <div class="flex flex-col items-center gap-6">
            <!-- Message Button -->
            <button class="flex flex-col items-center gap-1.5 active:scale-95 transition-all text-slate-400 hover:text-slate-350">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
              <span class="text-[10px] font-semibold">메시지 보내기</span>
            </button>

            <!-- Decline Button -->
            <div class="flex flex-col items-center gap-2">
              <button 
                @click="handleDecline"
                class="w-20 h-20 rounded-full bg-rose-600 hover:bg-rose-500 flex items-center justify-center text-3xl shadow-[0_4px_25px_rgba(225,29,72,0.5)] transition-all active:scale-90"
              >
                <!-- Decline Phone Icon -->
                <svg xmlns="http://www.w3.org/2000/svg" class="w-9 h-9 fill-white rotate-[135deg]" viewBox="0 0 24 24">
                  <path d="M21 16.5c0 .38-.21.71-.53.88l-4.87 2.44c-.38.19-.85.12-1.15-.18l-3.38-3.38c-.3-.3-.37-.77-.18-1.15l2.44-4.87c.17-.32.5-.53.88-.53h4.75c.55 0 1 .45 1 1v5.74zM3 7.5c0-.55.45-1 1-1h4.75c.38 0 .71.21.88.53l2.44 4.87c.19.38.12.85-.18 1.15l-3.38 3.38c-.3.3-.77.37-1.15.18L4.53 14c-.32-.17-.53-.5-.53-.88V7.5z"/>
                </svg>
              </button>
              <span class="text-sm text-slate-300 font-bold mt-1">거절</span>
            </div>
          </div>
        </div>

      </div>

      <!-- B. CONNECTED ACTIVE CALL STATE -->
      <div v-slot-if="store.simStatus === 'CONNECTED' && store.activeScenario" v-else-if="store.simStatus === 'CONNECTED' && store.activeScenario" class="flex-1 flex flex-col justify-between py-12 px-6 animate-fade-in">
        
        <!-- Active Call Header -->
        <div class="text-center mt-12 space-y-2">
          <h2 class="text-3xl font-black tracking-tight text-white whitespace-pre-line">
            {{ store.activeScenario.sender }}
          </h2>
          <p class="text-base text-emerald-400 font-extrabold tracking-widest uppercase">
            {{ formatDuration(duration) }}
          </p>
        </div>

        <!-- Live Caption Banner (iOS style semi-transparent card) -->
        <div v-if="currentVoiceStep" class="w-full max-w-xs mx-auto bg-white/10 backdrop-blur-md border border-white/10 rounded-2xl p-4 my-2 text-left space-y-1.5 shadow-[0_8px_32px_rgba(0,0,0,0.37)] animate-fade-in">
          <div class="flex items-center gap-1.5">
            <span class="w-1.5 h-1.5 rounded-full bg-blue-400 animate-pulse"></span>
            <span class="text-[9px] font-extrabold text-blue-400 uppercase tracking-widest">실시간 통화 자막 피드</span>
          </div>
          <p class="text-xs font-semibold text-slate-100 leading-relaxed">
            "{{ currentVoiceStep.dialogue }}"
          </p>
        </div>

        <!-- Phone Grid Actions (Mock iOS Style) -->
        <div class="grid grid-cols-3 gap-y-8 gap-x-8 max-w-xs mx-auto my-auto text-center">
          <button @click="toggleMute" :class="['flex flex-col items-center justify-center w-20 h-20 mx-auto rounded-full transition-all', isMutedLocal ? 'bg-white text-slate-950 shadow-lg' : 'bg-white/10 hover:bg-white/20 text-white']">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 fill-current" viewBox="0 0 24 24">
              <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z"/>
            </svg>
            <span class="text-[13px] font-bold mt-1.5">소리 끔</span>
          </button>
          <button class="flex flex-col items-center justify-center w-20 h-20 mx-auto bg-white/10 hover:bg-white/20 text-white rounded-full opacity-40 cursor-not-allowed">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 fill-current" viewBox="0 0 24 24">
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
            <span class="text-[13px] font-bold mt-1.5">키패드</span>
          </button>
          <button @click="toggleSpeaker" :class="['flex flex-col items-center justify-center w-20 h-20 mx-auto rounded-full transition-all', isSpeakerOn ? 'bg-white text-slate-950 shadow-lg' : 'bg-white/10 hover:bg-white/20 text-white']">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 fill-current" viewBox="0 0 24 24">
              <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/>
            </svg>
            <span class="text-[13px] font-bold mt-1.5">스피커</span>
          </button>
          <button class="flex flex-col items-center justify-center w-20 h-20 mx-auto bg-white/10 hover:bg-white/20 text-white rounded-full opacity-40 cursor-not-allowed">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 fill-current" viewBox="0 0 24 24">
              <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
            </svg>
            <span class="text-[13px] font-bold mt-1.5">통화 추가</span>
          </button>
          <button class="flex flex-col items-center justify-center w-20 h-20 mx-auto bg-white/10 hover:bg-white/20 text-white rounded-full opacity-40 cursor-not-allowed">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 fill-current" viewBox="0 0 24 24">
              <path d="M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z"/>
            </svg>
            <span class="text-[13px] font-bold mt-1.5">FaceTime</span>
          </button>
          <button class="flex flex-col items-center justify-center w-20 h-20 mx-auto bg-white/10 hover:bg-white/20 text-white rounded-full opacity-40 cursor-not-allowed">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 fill-current" viewBox="0 0 24 24">
              <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
            </svg>
            <span class="text-[13px] font-bold mt-1.5">연락처</span>
          </button>
        </div>

        <!-- Hang Up Button -->
        <div class="flex flex-col items-center mb-8">
          <button 
            @click="handleDecline"
            class="w-20 h-20 rounded-full bg-rose-600 hover:bg-rose-500 flex items-center justify-center text-3xl shadow-[0_4px_25px_rgba(225,29,72,0.5)] transition-all active:scale-90"
          >
            <!-- Decline Phone Icon -->
            <svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8 fill-white rotate-[135deg]" viewBox="0 0 24 24">
              <path d="M21 16.5c0 .38-.21.71-.53.88l-4.87 2.44c-.38.19-.85.12-1.15-.18l-3.38-3.38c-.3-.3-.37-.77-.18-1.15l2.44-4.87c.17-.32.5-.53.88-.53h4.75c.55 0 1 .45 1 1v5.74zM3 7.5c0-.55.45-1 1-1h4.75c.38 0 .71.21.88.53l2.44 4.87c.19.38.12.85-.18 1.15l-3.38 3.38c-.3.3-.77.37-1.15.18L4.53 14c-.32-.17-.53-.5-.53-.88V7.5z"/>
            </svg>
          </button>
          <span class="text-sm text-slate-300 font-bold mt-2">통화 종료</span>
        </div>

      </div>

      <!-- C. CALL REPORT STATE -->
      <div v-else-if="store.simStatus === 'CALL_REPORT' && activeAnalysis" class="flex-1 flex flex-col justify-between py-6 px-6 overflow-hidden animate-fade-in">
        
        <!-- Header -->
        <div class="text-center pb-4 border-b border-white/10">
          <h3 class="text-sm font-black text-slate-100">보이스피싱 모의 훈련 보고서</h3>
          <p class="text-[10px] text-slate-400 mt-1">실시간 대처 시간을 분석한 안심 진단표입니다.</p>
        </div>

        <!-- Scrollable Report Contents -->
        <div class="flex-1 overflow-y-auto my-4 pr-1 space-y-4 text-xs scroll-container">
          
          <!-- Scenario Info Card -->
          <div class="bg-white/5 border border-white/10 rounded-2xl p-4 space-y-2">
            <div class="flex justify-between items-center text-[9px] text-slate-400 font-bold">
              <span>훈련 유형: 보이스피싱</span>
              <span>{{ activeAnalysis.date }}</span>
            </div>
            <h4 class="text-xs font-black text-white mt-1">{{ activeAnalysis.scenarioTitle.replace('\n', ' ') }}</h4>
            <div class="flex items-center gap-1.5 text-[10px] text-slate-400 font-bold mt-1">
              <svg xmlns="http://www.w3.org/2056/svg" class="w-3.5 h-3.5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
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
              activeAnalysis.vulnerabilityStatus === 'SAFE' ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' :
              activeAnalysis.vulnerabilityStatus === 'WARNING' ? 'bg-amber-500/10 border-amber-500/20 text-amber-400' :
              'bg-rose-500/10 border-rose-500/20 text-rose-400'
            ]"
          >
            <p class="text-[9px] font-black uppercase tracking-wider">취약 진단 등급</p>
            <p class="text-base font-black mt-1">
              {{ 
                activeAnalysis.vulnerabilityStatus === 'SAFE' ? '안전 (즉각 대처)' : 
                activeAnalysis.vulnerabilityStatus === 'WARNING' ? '주의 (경고 노출)' : 
                '위험 (송금 피해 고위험)' 
              }}
            </p>
            <p class="text-[10px] text-slate-350 font-semibold mt-1">
              {{ activeAnalysis.hangUpStepName }} 에서 통화 종료
            </p>
          </div>

          <!-- Analysis Explanation -->
          <div class="space-y-1.5">
            <h5 class="font-bold text-slate-250">🔍 대처 취약점 분석</h5>
            <div class="bg-white/5 border border-white/5 rounded-2xl p-3.5 text-slate-300 leading-relaxed font-medium text-[11px]">
              {{ activeAnalysis.vulnerabilityExplanation }}
            </div>
          </div>

          <!-- Prevention Advice / Feedback -->
          <div class="space-y-1.5">
            <h5 class="font-bold text-slate-250">💡 안전 대응 가이드</h5>
            <div class="bg-white/5 border border-white/5 rounded-2xl p-3.5 text-slate-350 leading-relaxed font-medium text-[11px]">
              <p class="mb-2.5">{{ activeAnalysis.feedback }}</p>
              <div class="border-t border-white/10 pt-2.5 mt-2.5 space-y-2 text-[10px]">
                <div class="flex justify-between items-center text-slate-400">
                  <span>피해 발생 신고 (경찰청)</span>
                  <span class="text-white font-extrabold">국번없이 112</span>
                </div>
                <div class="flex justify-between items-center text-slate-400">
                  <span>피해 의심 상담 (금융감독원)</span>
                  <span class="text-white font-extrabold">국번없이 1332</span>
                </div>
                <div class="flex justify-between items-center text-slate-400">
                  <span>스팸/번호변작 제보 (인터넷진흥원)</span>
                  <span class="text-white font-extrabold">국번없이 118</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Phishing Techniques Timeline -->
          <div class="space-y-2">
            <h5 class="font-bold text-slate-250">📊 보이스피싱 시나리오 단계별 구성</h5>
            <div class="space-y-2">
              <div 
                v-for="(tech, tIdx) in activeAnalysis.techniques" 
                :key="tIdx"
                :class="[
                  'p-3.5 rounded-2xl border transition-all text-left flex gap-3',
                  tIdx === activeAnalysis.hangUpStepIndex 
                    ? 'bg-blue-600/10 border-blue-500/30 ring-1 ring-blue-500/20 shadow-[0_0_15px_rgba(59,130,246,0.15)]' 
                    : 'bg-white/5 border-white/5 opacity-60'
                ]"
              >
                <!-- Badge -->
                <div 
                  :class="[
                    'w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black flex-shrink-0 mt-0.5',
                    tIdx === activeAnalysis.hangUpStepIndex ? 'bg-blue-600 text-white animate-pulse' : 'bg-white/10 text-slate-350'
                  ]"
                >
                  {{ Number(tIdx) + 1 }}
                </div>
                <div class="space-y-1">
                  <div class="flex items-center gap-1.5">
                    <h6 class="font-bold text-slate-200">{{ tech.name }}</h6>
                    <span v-if="tIdx === activeAnalysis.hangUpStepIndex" class="text-[9px] bg-blue-500 text-white font-bold px-1.5 py-0.2 rounded">
                      종료 지점
                    </span>
                  </div>
                  <p class="text-[11px] text-slate-400 leading-normal">{{ tech.desc }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Privacy/Non-collection Notice -->
          <p class="text-[9px] text-slate-500 text-center leading-relaxed mt-4">
            ※ 본 훈련은 통화 음성 데이터 및 개인정보를 수집하거나 서버로 전송하지 않으며, 시나리오 진행도와 전화 끊기 타이밍만을 활용하여 기재된 안심 분석 리포트입니다.
          </p>

        </div>

        <!-- Confirm Button -->
        <button 
          @click="closeReport"
          class="w-full bg-blue-600 hover:bg-blue-500 active:scale-95 transition-all text-white font-bold text-sm py-3 px-4 rounded-xl shadow-[0_4px_20px_rgba(37,99,235,0.4)] mt-2"
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
