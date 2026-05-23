<!-- 5-layer architecture: Component (Page) Layer for Simulation -->

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useTrainingStore } from '../state/trainingStore'
import { trainingService } from '../service/trainingService'

const store = useTrainingStore()

onMounted(async () => {
  if (store.scenarios.length === 0) {
    await trainingService.loadScenarios()
  }
})

const start = (scenario: any) => {
  desktopDynamicDialogue.value = ''
  trainingService.startSimulation(scenario)
}

// selectChoice is unused in the text input simulator, commented out to avoid warnings
/*
const selectChoice = async (idx: number) => {
  await trainingService.handleUserChoice(idx)
}
*/

const clickSmsLink = async () => {
  await trainingService.simulateClickLink()
}

const ignoreAction = async () => {
  await trainingService.ignoreTraining()
}

const inputCredentials = async () => {
  await trainingService.simulateInputCredentials()
}

const cancel = () => {
  stopDesktopActiveSSE()
  trainingService.cancelSimulation()
}

const desktopTextInput = ref('')
const desktopDynamicDialogue = ref('')
const isWaitingForResponse = ref(false)
let desktopResponseEventSource: EventSource | null = null

const stopDesktopActiveSSE = () => {
  if (desktopResponseEventSource) {
    console.log('[SSE LLM Desktop] Closing active response SSE.')
    try {
      desktopResponseEventSource.close()
    } catch (e) {
      console.error(e)
    }
    desktopResponseEventSource = null
  }
  isWaitingForResponse.value = false
}

onUnmounted(() => {
  stopDesktopActiveSSE()
})

const submitDesktopTextInput = async () => {
  const text = desktopTextInput.value.trim()
  if (!text || isWaitingForResponse.value) return
  
  desktopTextInput.value = ''
  isWaitingForResponse.value = true
  
  const scenario = store.activeScenario
  if (!scenario) {
    isWaitingForResponse.value = false
    return
  }

  const isSuspicious = /사기|사칭|의심|끊어|피싱|경찰|검찰청|금감원|아닌가|신고|아니요|못 믿/i.test(text)
  
  if (isSuspicious) {
    desktopDynamicDialogue.value = ''
    stopDesktopActiveSSE()
    await trainingService.handleUserChoice(1) // SUCCESS (Safe path)
    return
  }

  // DELETED: Automatic step index increment
  
  stopDesktopActiveSSE()
  desktopDynamicDialogue.value = ''

  const sseUrl = `/api/v1/calls/respond?userId=demo_user&scenarioId=${encodeURIComponent(scenario.id)}&text=${encodeURIComponent(text)}`
  console.log('[SSE LLM Desktop] Connecting to stream:', sseUrl)

  const es = new EventSource(sseUrl)
  desktopResponseEventSource = es

  es.addEventListener('chunk', (event: any) => {
    try {
      const data = JSON.parse(event.data)
      const chunkText = data.text
      // Filter out internal state tags from UI display
      if (chunkText.includes('[STATE:')) return;

      console.log('[SSE LLM Desktop Chunk]:', chunkText)
      if (desktopDynamicDialogue.value) {
        desktopDynamicDialogue.value += ' ' + chunkText
      } else {
        desktopDynamicDialogue.value = chunkText
      }
    } catch (err) {
      console.error('Failed to parse SSE desktop chunk:', err)
    }
  })

  es.addEventListener('complete', async (event: any) => {
    const data = event.data // Format: "STATUS|FULL_TEXT"
    console.log('[SSE LLM Desktop Complete]:', data)
    
    const [status, fullText] = data.split('|')
    es.close()
    
    if (desktopResponseEventSource === es) {
      desktopResponseEventSource = null
      isWaitingForResponse.value = false
    }

    if (status === 'FAILED') {
      await trainingService.handleUserChoice(0) // Trigger Warning Screen
    } else if (status === 'SUCCESS') {
      await trainingService.handleUserChoice(1) // Show Success (IDLE)
    } else {
      // PROCEEDING: Just advance visual step for UI guidance if needed
      const nextStep = Math.min(store.currentStepIndex + 1, scenario.steps.length - 1)
      store.setStepIndex(nextStep)
    }
  })

  es.onerror = (err) => {
    console.warn('[SSE LLM Desktop Connection Error]', err)
    es.close()
    if (desktopResponseEventSource === es) {
      desktopResponseEventSource = null
      isWaitingForResponse.value = false
    }
    if (desktopDynamicDialogue.value === '') {
      desktopDynamicDialogue.value = '연결 상태가 좋지 않아 답변이 지연되고 있습니다.'
    }
  }
}
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- A. IDLE: List Scenarios -->
    <div v-if="store.simStatus === 'IDLE'" class="p-6 space-y-6 flex-1">
      <div class="text-center py-2">
        <h2 class="text-xl font-bold text-slate-800">모의 피싱 훈련소</h2>
        <p class="text-sm text-slate-500 mt-1">상황별 시나리오를 선택하여 실전형 모의 훈련을 진행합니다.</p>
      </div>

      <div class="space-y-4">
        <div 
          v-for="s in store.scenarios"
          :key="s.id"
          class="bg-white border border-slate-200/80 rounded-2xl p-5 flex flex-col justify-between gap-4 shadow-[0_4px_20px_-4px_rgba(0,0,0,0.02)] hover:border-slate-350 transition-all duration-200"
        >
          <div class="flex items-start justify-between">
            <div class="space-y-2">
              <span class="text-xs bg-slate-100 text-slate-650 font-bold px-2 py-0.5 rounded border border-slate-200/50 uppercase tracking-wider">
                {{ s.type === 'VOICE' ? '전화 피싱' : s.type === 'SMS' ? '문자 스미싱' : '이메일 피싱' }}
              </span>
              <h3 class="text-base font-bold text-slate-855 mt-1">{{ s.title }}</h3>
              <p class="text-sm text-slate-500 leading-relaxed">{{ s.content }}</p>
            </div>
            <span class="p-2.5 bg-slate-50 rounded-xl border border-slate-100 flex-shrink-0 text-slate-500">
              <svg v-if="s.type === 'VOICE'" xmlns="http://www.w3.org/2000/svg" class="w-6 h-6 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
              </svg>
              <svg v-else-if="s.type === 'SMS'" xmlns="http://www.w3.org/2000/svg" class="w-6 h-6 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" class="w-6 h-6 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                <polyline points="22,6 12,13 2,6"/>
              </svg>
            </span>
          </div>

          <button 
            @click="start(s)"
            class="w-full bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-sm py-2.5 px-4 rounded-xl border border-slate-200/60 shadow-2xs transition-all active:scale-[0.99]"
          >
            훈련 시작하기
          </button>
        </div>
      </div>
    </div>

    <!-- B. VOICE CALL SIMULATION -->
    <div 
      v-else-if="(store.simStatus as any) === 'CALLING' && store.activeScenario" 
      class="flex-1 bg-slate-50/20 p-6 flex flex-col justify-between items-center text-center animate-fade-in"
    >
      <!-- Caller Info -->
      <div class="mt-12 space-y-3">
        <div class="w-24 h-24 rounded-full bg-slate-100 border border-slate-200/60 flex items-center justify-center mx-auto shadow-sm text-slate-400">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-10 h-10 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
        </div>
        <h3 class="text-lg font-bold text-slate-800">{{ store.activeScenario.sender }}</h3>
        <p class="text-sm text-rose-600 font-bold flex items-center justify-center gap-1.5">
          <span class="h-2 w-2 rounded-full bg-rose-500 animate-pulse"></span>
          수신 중...
        </p>
      </div>

      <!-- Call Dialogue Content -->
      <div class="w-full bg-white border border-slate-200/80 rounded-3xl p-5 max-w-sm my-6 space-y-4 shadow-[0_10px_25px_-5px_rgba(0,0,0,0.02)]">
        <p class="text-xs font-bold text-slate-400 uppercase tracking-widest text-left border-b border-slate-100 pb-2">
          대화 진행 상황
        </p>
        <p class="text-sm font-semibold text-slate-700 text-left leading-relaxed">
          {{ desktopDynamicDialogue || store.activeScenario.steps[store.currentStepIndex]?.dialogue }}
        </p>
      </div>

      <!-- Dialogue input -->
      <div class="w-full max-w-sm space-y-4 pb-10">
        <div class="space-y-1.5">
          <p class="text-xs font-bold text-slate-400 uppercase tracking-widest text-left">
            💬 대답 입력 (텍스트로 대화 진행)
          </p>
          <div class="flex items-center gap-2 bg-white border border-slate-200 rounded-xl p-1.5 shadow-[0_2px_12px_rgba(0,0,0,0.03)] focus-within:border-indigo-500 transition-colors" :class="{'opacity-60 bg-slate-50': isWaitingForResponse}">
            <input 
              v-model="desktopTextInput"
              @keyup.enter="submitDesktopTextInput"
              type="text" 
              :placeholder="isWaitingForResponse ? '답변을 기다리는 중...' : '대답을 입력하세요...'" 
              :disabled="isWaitingForResponse"
              class="flex-1 bg-transparent text-xs text-slate-800 placeholder-slate-400 focus:outline-none px-2.5 py-1.5"
            />
            <button 
              @click="submitDesktopTextInput"
              :disabled="isWaitingForResponse"
              class="bg-indigo-600 hover:bg-indigo-500 active:scale-95 text-white text-[11px] font-bold py-1.5 px-4 rounded-lg transition-all disabled:bg-slate-300 disabled:scale-100"
            >
              {{ isWaitingForResponse ? '...' : '전송' }}
            </button>
          </div>
        </div>

        <button 
          @click="cancel"
          class="w-full bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200/60 text-xs py-3 px-4 rounded-xl font-bold transition-all duration-200 active:scale-[0.99] mt-2"
        >
          통화 거절 / 끊기
        </button>
      </div>
    </div>

    <!-- C. SMS RECEIVED SIMULATION -->
    <div 
      v-else-if="store.simStatus === 'SMS_RECEIVED' && store.activeScenario" 
      class="flex-1 bg-slate-50/20 p-6 flex flex-col justify-between"
    >
      <div class="space-y-4">
        <!-- Messenger Chat Header -->
        <div class="flex items-center justify-between border-b border-slate-100 pb-4">
          <div class="flex items-center gap-3">
            <button @click="cancel" class="text-slate-500 hover:text-slate-700 text-sm font-bold">← 목록</button>
            <div>
              <h3 class="text-sm font-bold text-slate-800">{{ store.activeScenario.sender }}</h3>
              <p class="text-xs text-slate-450 mt-0.5 font-medium">알 수 없는 번호</p>
            </div>
          </div>
        </div>

        <!-- Chat Bubble Mockup -->
        <div class="space-y-3">
          <div class="flex gap-2 max-w-[85%]">
            <div class="w-8 h-8 rounded-xl bg-slate-100 border border-slate-200/50 flex items-center justify-center flex-shrink-0 text-slate-400">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
            </div>
            <div class="bg-white border border-slate-200/80 text-slate-700 p-3.5 rounded-2xl rounded-tl-none text-sm leading-relaxed space-y-2.5 shadow-2xs">
              <p>{{ store.activeScenario.content.split('http')[0] }}</p>
              
              <!-- Simulated phishing URL -->
              <button 
                @click="clickSmsLink"
                class="w-full bg-blue-50 text-blue-600 text-xs hover:bg-blue-100/60 py-2.5 px-3 rounded-lg border border-blue-100 block text-left break-all font-bold"
              >
                http://koreapost-info.xyz
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Action items -->
      <div class="space-y-2.5 pb-10">
        <button 
          @click="ignoreAction"
          class="w-full bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 shadow-2xs font-bold text-sm py-3.5 px-4 rounded-xl transition-all active:scale-[0.99]"
        >
          무시하고 차단하기
        </button>
        <button 
          @click="cancel"
          class="w-full bg-transparent text-slate-400 hover:text-slate-600 text-sm py-2"
        >
          중단
        </button>
      </div>
    </div>

    <!-- D. EMAIL OPENED SIMULATION -->
    <div 
      v-else-if="store.simStatus === 'EMAIL_OPENED' && store.activeScenario" 
      class="flex-1 bg-slate-50/20 p-6 flex flex-col justify-between"
    >
      <div class="space-y-4">
        <!-- Email Header -->
        <div class="border-b border-slate-200 pb-4 space-y-2">
          <div class="flex items-center justify-between">
            <button @click="cancel" class="text-slate-500 hover:text-slate-700 text-sm font-bold">← 목록</button>
            <span class="text-xs bg-slate-100 text-slate-655 px-2 py-0.5 rounded border border-slate-200/40 font-medium">이메일</span>
          </div>
          <div>
            <h3 class="text-sm font-bold text-slate-800 leading-snug">보낸이: {{ store.activeScenario.sender }}</h3>
            <p class="text-xs font-semibold text-slate-500 mt-1">제목: {{ store.activeScenario.title }}</p>
          </div>
        </div>

        <!-- Fake Login / Reset Panel Simulator -->
        <div class="bg-white border border-slate-200/80 rounded-2xl p-5 space-y-4 shadow-2xs">
          <div class="text-center py-2 border-b border-slate-100">
            <div class="w-10 h-10 bg-[#FEE500] text-slate-900 rounded-full flex items-center justify-center font-black mx-auto text-sm shadow-sm">K</div>
            <h4 class="text-sm font-bold text-slate-800 mt-2">Kakao Accounts 보안 알림</h4>
          </div>

          <p class="text-xs text-slate-600 leading-relaxed text-center">
            비정상 위치(러시아)에서 본 계정으로 로그인이 성공했습니다. 본인의 시도가 아닐 경우 
            <strong>아래 버튼을 눌러 즉시 비밀번호를 변경</strong>하세요.
          </p>

          <!-- Input fields to trick user -->
          <div class="space-y-2">
            <input 
              type="text" 
              placeholder="카카오 계정 ID (이메일/번호)" 
              disabled
              class="w-full bg-slate-50 border border-slate-200 rounded-lg p-2 text-xs text-slate-500 cursor-not-allowed"
            />
            <input 
              type="password" 
              placeholder="현재 비밀번호" 
              disabled
              class="w-full bg-slate-50 border border-slate-200 rounded-lg p-2 text-xs text-slate-500 cursor-not-allowed"
            />
          </div>

          <!-- Phishing buttons -->
          <div class="space-y-2">
            <button 
              @click="inputCredentials"
              class="w-full bg-[#FEE500] hover:bg-[#F3DB00] text-slate-900 font-bold text-xs py-2.5 px-3 rounded-lg shadow-sm transition-colors active:scale-[0.99]"
            >
              비밀번호 재설정 및 로그인
            </button>
            <button 
              @click="ignoreAction"
              class="w-full bg-transparent text-slate-400 hover:text-slate-600 text-xs py-1.5"
            >
              본인이 맞으므로 승인
            </button>
          </div>
        </div>
      </div>

      <div class="pb-10 text-center">
        <button 
          @click="cancel"
          class="text-sm text-slate-400 hover:text-slate-600"
        >
          돌아가기
        </button>
      </div>
    </div>

    <!-- E. WARNING / EDUCATION PAGE -->
    <div 
      v-else-if="store.simStatus === 'WARNING_SCREEN' && store.activeScenario" 
      class="flex-1 bg-rose-50/20 p-6 flex flex-col justify-between items-center text-center animate-fade-in"
    >
      <div class="mt-8 space-y-4">
        <div class="w-16 h-16 rounded-full bg-rose-100 border border-rose-200/50 flex items-center justify-center mx-auto shadow-sm animate-pulse text-rose-600">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
            <line x1="12" y1="9" x2="12" y2="13"/>
            <line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
        </div>
        <h2 class="text-xl font-black text-rose-600">피싱 공격 노출 감지!</h2>
        <p class="text-sm text-slate-600 max-w-sm leading-relaxed">
          피싱 사기 행각에 걸려들어 <strong>위험 요건(개인정보 제공 또는 송금 동의)</strong>을 실행하셨습니다.
        </p>
      </div>

      <!-- Explanation Box -->
      <div class="w-full bg-white border border-rose-100 rounded-3xl p-5 max-w-sm my-6 space-y-3 text-left shadow-lg shadow-rose-100/30">
        <h4 class="text-sm font-bold text-rose-600 flex items-center gap-1.5">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
            <path d="M15 14c.2-.2.4-.4.6-.7C16.8 11.7 17 10 15.8 8.3 14.5 6.7 12.2 6 10.5 6.8c-1.7.8-2.5 2.6-2.5 4.5 0 .9.2 1.8.8 2.5.2.3.4.5.6.7.3.3.5.7.5 1.1v1.5c0 .3.2.5.5.5h4c.3 0 .5-.2.5-.5v-1.5c0-.4.2-.8.5-1.1z"/>
            <line x1="9" y1="18" x2="15" y2="18"/>
            <line x1="10" y1="21" x2="14" y2="21"/>
          </svg>
          왜 위험했을까요?
        </h4>
        <p class="text-xs font-semibold text-slate-600 leading-relaxed">
          {{ store.warningMessage }}
        </p>
      </div>

      <div class="w-full max-w-sm pb-10 space-y-2.5">
        <button 
          @click="cancel"
          class="w-full bg-slate-100 hover:bg-slate-200 border border-slate-200/80 text-slate-700 font-bold text-sm py-3.5 px-4 rounded-xl transition-all active:scale-[0.99]"
        >
          훈련 완료 후 돌아가기
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fade-in {
  animation: fadeIn 0.3s ease-out forwards;
}
.text-2xs {
  font-size: 0.7rem;
}
.text-3xs {
  font-size: 0.65rem;
}
</style>
