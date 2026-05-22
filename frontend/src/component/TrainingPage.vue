<!-- 5-layer architecture: Component (Page) Layer for Simulation -->

<script setup lang="ts">
import { onMounted } from 'vue'
import { useTrainingStore } from '../state/trainingStore'
import { trainingService } from '../service/trainingService'

const store = useTrainingStore()

onMounted(async () => {
  if (store.scenarios.length === 0) {
    await trainingService.loadScenarios()
  }
})

const start = (scenario: any) => {
  trainingService.startSimulation(scenario)
}

const selectChoice = async (idx: number) => {
  await trainingService.handleUserChoice(idx)
}

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
  trainingService.cancelSimulation()
}
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- A. IDLE: List Scenarios -->
    <div v-if="store.simStatus === 'IDLE'" class="p-6 space-y-6 flex-1">
      <div class="text-center py-2">
        <h2 class="text-xl font-bold text-slate-800">모의 피싱 훈련소</h2>
        <p class="text-xs text-slate-500 mt-1">상황별 시나리오를 선택하여 실전형 모의 훈련을 진행합니다.</p>
      </div>

      <div class="space-y-4">
        <div 
          v-for="s in store.scenarios"
          :key="s.id"
          class="bg-white border border-slate-200/80 rounded-2xl p-5 flex flex-col justify-between gap-4 shadow-[0_4px_20px_-4px_rgba(0,0,0,0.02)] hover:border-slate-350 transition-all duration-200"
        >
          <div class="flex items-start justify-between">
            <div class="space-y-2">
              <span class="text-[9px] bg-slate-100 text-slate-650 font-bold px-2 py-0.5 rounded border border-slate-200/50 uppercase tracking-wider">
                {{ s.type === 'VOICE' ? '전화 피싱' : s.type === 'SMS' ? '문자 스미싱' : '이메일 피싱' }}
              </span>
              <h3 class="text-sm font-bold text-slate-850 mt-1">{{ s.title }}</h3>
              <p class="text-[11px] text-slate-500 leading-relaxed">{{ s.content }}</p>
            </div>
            <span class="text-2xl p-2 bg-slate-50 rounded-xl border border-slate-100 flex-shrink-0">
              {{ s.type === 'VOICE' ? '📞' : s.type === 'SMS' ? '💬' : '📧' }}
            </span>
          </div>

          <button 
            @click="start(s)"
            class="w-full bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs py-2.5 px-4 rounded-xl border border-slate-200/60 shadow-2xs transition-all active:scale-[0.98]"
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
        <div class="w-24 h-24 rounded-full bg-slate-100 border border-slate-200/60 flex items-center justify-center text-3xl mx-auto shadow-sm">
          👤
        </div>
        <h3 class="text-lg font-bold text-slate-800">{{ store.activeScenario.sender }}</h3>
        <p class="text-xs text-rose-600 font-bold flex items-center justify-center gap-1.5">
          <span class="h-2 w-2 rounded-full bg-rose-500 animate-pulse"></span>
          수신 중...
        </p>
      </div>

      <!-- Call Dialogue Content -->
      <div class="w-full bg-white border border-slate-200/80 rounded-3xl p-5 max-w-sm my-6 space-y-4 shadow-[0_10px_25px_-5px_rgba(0,0,0,0.02)]">
        <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest text-left border-b border-slate-100 pb-2">
          대화 진행 상황
        </p>
        <p class="text-xs font-semibold text-slate-700 text-left leading-relaxed">
          {{ store.activeScenario.steps[store.currentStepIndex]?.dialogue }}
        </p>
      </div>

      <!-- Dialogue choices -->
      <div class="w-full space-y-2.5 max-w-sm pb-10">
        <button
          v-for="(opt, idx) in store.activeScenario.steps[store.currentStepIndex]?.options"
          :key="idx"
          @click="selectChoice(idx)"
          class="w-full bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 hover:border-slate-350 text-xs py-3.5 px-4 rounded-xl text-left font-bold transition-all duration-200 active:scale-[0.98] leading-normal shadow-2xs"
        >
          {{ opt }}
        </button>

        <button 
          @click="cancel"
          class="w-full bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200/60 text-xs py-3 px-4 rounded-xl font-bold transition-all duration-200 active:scale-[0.98] mt-4"
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
            <button @click="cancel" class="text-slate-500 hover:text-slate-700 text-xs font-bold">← 목록</button>
            <div>
              <h3 class="text-xs font-bold text-slate-800">{{ store.activeScenario.sender }}</h3>
              <p class="text-[9px] text-slate-450 mt-0.5 font-medium">알 수 없는 번호</p>
            </div>
          </div>
        </div>

        <!-- Chat Bubble Mockup -->
        <div class="space-y-3">
          <div class="flex gap-2 max-w-[85%]">
            <div class="w-8 h-8 rounded-xl bg-slate-100 border border-slate-200/50 flex items-center justify-center text-sm flex-shrink-0">
              💬
            </div>
            <div class="bg-white border border-slate-200/80 text-slate-700 p-3.5 rounded-2xl rounded-tl-none text-xs leading-relaxed space-y-2.5 shadow-2xs">
              <p>{{ store.activeScenario.content.split('http')[0] }}</p>
              
              <!-- Simulated phishing URL -->
              <button 
                @click="clickSmsLink"
                class="w-full bg-blue-50 text-blue-600 text-2xs hover:bg-blue-100/60 py-2.5 px-3 rounded-lg border border-blue-100 block text-left break-all font-bold"
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
          class="w-full bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 shadow-2xs font-bold text-xs py-3.5 px-4 rounded-xl transition-all"
        >
          무시하고 차단하기
        </button>
        <button 
          @click="cancel"
          class="w-full bg-transparent text-slate-400 hover:text-slate-600 text-xs py-2"
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
            <button @click="cancel" class="text-slate-500 hover:text-slate-700 text-xs font-bold">← 목록</button>
            <span class="text-[9px] bg-slate-100 text-slate-650 px-2 py-0.5 rounded border border-slate-200/40 font-medium">이메일</span>
          </div>
          <div>
            <h3 class="text-xs font-bold text-slate-800 leading-snug">보낸이: {{ store.activeScenario.sender }}</h3>
            <p class="text-2xs font-semibold text-slate-500 mt-1">제목: {{ store.activeScenario.title }}</p>
          </div>
        </div>

        <!-- Fake Login / Reset Panel Simulator -->
        <div class="bg-white border border-slate-200/80 rounded-2xl p-5 space-y-4 shadow-2xs">
          <div class="text-center py-2 border-b border-slate-100">
            <div class="w-10 h-10 bg-[#FEE500] text-slate-900 rounded-full flex items-center justify-center font-black mx-auto text-sm shadow-sm">K</div>
            <h4 class="text-xs font-bold text-slate-800 mt-2">Kakao Accounts 보안 알림</h4>
          </div>

          <p class="text-2xs text-slate-600 leading-relaxed text-center">
            비정상 위치(러시아)에서 본 계정으로 로그인이 성공했습니다. 본인의 시도가 아닐 경우 
            <strong>아래 버튼을 눌러 즉시 비밀번호를 변경</strong>하세요.
          </p>

          <!-- Input fields to trick user -->
          <div class="space-y-2">
            <input 
              type="text" 
              placeholder="카카오 계정 ID (이메일/번호)" 
              disabled
              class="w-full bg-slate-50 border border-slate-200 rounded-lg p-2 text-3xs text-slate-500 cursor-not-allowed"
            />
            <input 
              type="password" 
              placeholder="현재 비밀번호" 
              disabled
              class="w-full bg-slate-50 border border-slate-200 rounded-lg p-2 text-3xs text-slate-500 cursor-not-allowed"
            />
          </div>

          <!-- Phishing buttons -->
          <div class="space-y-2">
            <button 
              @click="inputCredentials"
              class="w-full bg-[#FEE500] hover:bg-[#F3DB00] text-slate-900 font-bold text-2xs py-2.5 px-3 rounded-lg shadow-sm transition-colors"
            >
              비밀번호 재설정 및 로그인
            </button>
            <button 
              @click="ignoreAction"
              class="w-full bg-transparent text-slate-400 hover:text-slate-600 text-3xs py-1.5"
            >
              본인이 맞으므로 승인
            </button>
          </div>
        </div>
      </div>

      <div class="pb-10 text-center">
        <button 
          @click="cancel"
          class="text-xs text-slate-400 hover:text-slate-600"
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
        <div class="w-16 h-16 rounded-full bg-rose-100 text-rose-600 border border-rose-200/50 flex items-center justify-center text-3xl mx-auto shadow-sm animate-pulse">
          🚨
        </div>
        <h2 class="text-lg font-black text-rose-600">피싱 공격 노출 감지!</h2>
        <p class="text-xs text-slate-600 max-w-sm leading-relaxed">
          피싱 사기 행각에 걸려들어 <strong>위험 요건(개인정보 제공 또는 송금 동의)</strong>을 실행하셨습니다.
        </p>
      </div>

      <!-- Explanation Box -->
      <div class="w-full bg-white border border-rose-100 rounded-3xl p-5 max-w-sm my-6 space-y-3 text-left shadow-lg shadow-rose-100/30">
        <h4 class="text-xs font-bold text-rose-600 flex items-center gap-1.5">
          💡 왜 위험했을까요?
        </h4>
        <p class="text-2xs font-semibold text-slate-600 leading-relaxed">
          {{ store.warningMessage }}
        </p>
      </div>

      <div class="w-full max-w-sm pb-10 space-y-2.5">
        <button 
          @click="cancel"
          class="w-full bg-slate-100 hover:bg-slate-200 border border-slate-200/80 text-slate-700 font-bold text-xs py-3.5 px-4 rounded-xl transition-all"
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
