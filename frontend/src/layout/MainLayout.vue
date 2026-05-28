<!-- 5-layer architecture: Layout Layer -->
<!-- Defines the app container and navigation tab shell -->

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useProfilingStore } from '../state/profilingStore'
import { useTrainingStore } from '../state/trainingStore'
import IncomingCallOverlay from '../component/IncomingCallOverlay.vue'
import TrainingPage from '../component/TrainingPage.vue'

const props = defineProps<{
  activeTab: 'profiling' | 'response' | 'report'
}>()

const emit = defineEmits<{
  (e: 'tabChange', tab: 'profiling' | 'response' | 'report'): void
}>()

const profilingStore = useProfilingStore()
const trainingStore = useTrainingStore()

// 실시간 시각 동기화
const currentTime = ref('')
const updateTime = () => {
  const now = new Date()
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  currentTime.value = `${hours}:${minutes}`
}

let timer: ReturnType<typeof setInterval>

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000) // 1초 단위 업데이트로 실시간 반영
})

onUnmounted(() => {
  clearInterval(timer)
})

// 안드로이드 하단 버튼 처리
const handleBack = () => {
  // 경고 오버레이 등이 켜져 있으면 끄기
  if (trainingStore.simStatus === 'WARNING_SCREEN') {
    trainingStore.stopSimulation()
    emit('tabChange', 'profiling')
  } else if (trainingStore.simStatus === 'SMS_RECEIVED' || trainingStore.simStatus === 'EMAIL_OPENED') {
    trainingStore.stopSimulation()
  } else if (trainingStore.simStatus === 'RINGING' || trainingStore.simStatus === 'CONNECTED') {
    trainingStore.stopSimulation()
  } else if (props.activeTab !== 'profiling') {
    emit('tabChange', 'profiling')
  }
}

const handleHome = () => {
  trainingStore.stopSimulation()
  emit('tabChange', 'profiling')
}

const handleRecentApps = () => {
  // 최근 실행 앱 모달 등을 시뮬레이션
  alert('갤럭시 최근 실행 앱 목록 화면입니다. 홈 버튼을 누르면 첫 화면으로 돌아갑니다.')
}
</script>

<template>
  <div class="min-h-screen bg-slate-100 text-slate-850 font-sans flex items-center justify-center p-0 xs:p-6 select-none">
    <!-- 갤럭시 외부 하드웨어 프레임 시뮬레이터 (물리 버튼 포함) -->
    <div class="relative w-full xs:max-w-[412px] flex items-center justify-center">
      
      <!-- 갤럭시 전용 물리 버튼 (볼륨 키, 전원 키) - PC에서만 표시 -->
      <!-- 볼륨 업 버튼 -->
      <div class="hidden xs:block absolute -right-[3px] top-[140px] w-[3px] h-[55px] bg-slate-750 rounded-r-sm shadow-[1px_0_3px_rgba(0,0,0,0.2)] border-r border-slate-800 transition-all active:translate-x-[-1px] z-0"></div>
      <!-- 볼륨 다운 버튼 -->
      <div class="hidden xs:block absolute -right-[3px] top-[205px] w-[3px] h-[55px] bg-slate-750 rounded-r-sm shadow-[1px_0_3px_rgba(0,0,0,0.2)] border-r border-slate-800 transition-all active:translate-x-[-1px] z-0"></div>
      <!-- 전원/빅스비 버튼 -->
      <div class="hidden xs:block absolute -right-[3px] top-[290px] w-[3px] h-[80px] bg-slate-750 rounded-r-sm shadow-[1px_0_3px_rgba(0,0,0,0.2)] border-r border-slate-800 transition-all active:translate-x-[-1px] z-0" @click="handleHome"></div>

      <!-- 기기 외관 바디 (베젤 및 메탈 링) -->
      <div class="w-full h-screen xs:h-[846px] bg-slate-900 border border-slate-800 xs:border-[10px] xs:border-slate-950 xs:rounded-[50px] xs:shadow-[0_25px_60px_-15px_rgba(0,0,0,0.3),_0_0_0_2px_rgba(255,255,255,0.05)_inset] overflow-hidden flex flex-col relative z-10 bg-white">
        
        <!-- 갤럭시 상단 카메라 펀치홀 -->
        <div class="absolute top-[8px] left-1/2 -translate-x-1/2 w-4 h-4 bg-black rounded-full z-40 border border-slate-900 shadow-[inset_0_1px_2px_rgba(255,255,255,0.2)] flex items-center justify-center">
          <!-- 렌즈 반사 효과 -->
          <div class="w-1.5 h-1.5 bg-blue-950 rounded-full opacity-60"></div>
        </div>

        <!-- [1] 갤럭시 One UI 스타일 상단 상태바 (Status Bar) -->
        <div class="w-full h-7 bg-slate-50/95 backdrop-blur-md px-6 flex items-center justify-between flex-shrink-0 z-30 select-none text-slate-800 text-[11px] font-semibold tracking-tight border-b border-slate-100/50">
          <!-- 좌측: 현재 시각 및 알림 아이콘 -->
          <div class="flex items-center gap-1.5">
            <span>{{ currentTime }}</span>
            <div class="flex items-center gap-1 opacity-70">
              <!-- 전화 알림 아이콘 -->
              <svg xmlns="http://www.w3.org/2000/svg" class="w-3 h-3 fill-current" viewBox="0 0 24 24">
                <path d="M6.62 10.79a15.15 15.15 0 0 0 6.59 6.59l2.2-2.2a1 1 0 0 1 1.11-.27 11.36 11.36 0 0 0 3.58 1.1 1 1 0 0 1 .89 1v3.58a1 1 0 0 1-1 1A16 16 0 0 1 3 4a1 1 0 0 1 1-1h3.58a1 1 0 0 1 1 .89 11.36 11.36 0 0 0 1.1 3.58 1 1 0 0 1-.27 1.11z"/>
              </svg>
              <!-- 메시지 알림 아이콘 -->
              <svg xmlns="http://www.w3.org/2000/svg" class="w-3 h-3 fill-current" viewBox="0 0 24 24">
                <path d="M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 9h12v2H6V9zm8 5H6v-2h8v2zm4-6H6V6h12v2z"/>
              </svg>
            </div>
          </div>
          <!-- 우측: 모바일 신호, Wi-Fi, 배터리 잔량 -->
          <div class="flex items-center gap-1.5 opacity-80">
            <!-- 5G 안테나 -->
            <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24">
              <path d="M2 22h20V2zM17 5v13h-3V8h-3v10H8v-7H5v7H2v4h20V5h-3z" opacity="0.3"/>
              <path d="M19 22h3v-8h-3v8zm-5 0h3V9h-3v13zm-5 0h3v-5H9v5z"/>
            </svg>
            <!-- Wi-Fi -->
            <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24">
              <path d="M12 21l-12-12c4-4 8-6 12-6s8 2 12 6l-12 12z"/>
            </svg>
            <!-- 배터리 잔량 수치 & 아이콘 -->
            <span class="text-[10px] mr-0.5">92%</span>
            <div class="w-5 h-2.5 border border-slate-700/80 rounded-[3px] p-[1px] flex items-center relative">
              <div class="bg-slate-700 h-full w-[80%] rounded-[1px]"></div>
              <div class="w-[1.5px] h-1.2 bg-slate-700 absolute -right-[2.5px] top-[2px] rounded-r-[1px]"></div>
            </div>
          </div>
        </div>

        <!-- [2] 기존 앱 헤더 (상태바 바로 밑에 배치하며 높이 패딩 축소) -->
        <header class="bg-slate-50/90 backdrop-blur-md border-b border-slate-100 px-6 py-2.5 flex items-center justify-between flex-shrink-0 z-20 select-none">
          <div class="flex items-center gap-2">
            <div class="bg-blue-600 text-white font-black px-2 py-0.5 rounded-lg text-sm tracking-tighter shadow-sm">P</div>
            <span class="font-extrabold text-sm tracking-tight text-slate-800">피싱 예방 시뮬레이터</span>
          </div>
          
          <!-- Top Status indicators -->
          <div class="flex items-center gap-2">
            <span 
              v-if="profilingStore.result" 
              :class="[
                'text-[10px] px-2.5 py-0.5 rounded-full font-bold border shadow-sm',
                profilingStore.result.riskLevel === 'HIGH' ? 'bg-rose-50 text-rose-600 border-rose-100' :
                profilingStore.result.riskLevel === 'MEDIUM' ? 'bg-amber-50 text-amber-600 border-amber-100' : 'bg-emerald-50 text-emerald-600 border-emerald-100'
              ]"
            >
              {{ profilingStore.result.riskType }}
            </span>
            <span v-else class="text-[10px] bg-slate-100 text-slate-500 border border-slate-200/60 px-2.5 py-0.5 rounded-full font-semibold">
              진단 필요
            </span>
          </div>
        </header>

        <!-- [3] 메인 스크롤 화면 (Main Scrollable Screen Slot) -->
        <main class="flex-1 overflow-y-auto bg-slate-50/30">
          <slot />
        </main>

        <!-- 글로벌 오버레이 영역 -->
        <IncomingCallOverlay />
        
        <!-- Global Training Overlay for SMS/Email/Warning -->
        <TrainingPage 
          v-if="trainingStore.simStatus === 'SMS_RECEIVED' || trainingStore.simStatus === 'EMAIL_OPENED' || trainingStore.simStatus === 'WARNING_SCREEN'" 
          class="absolute inset-x-0 bottom-[40px] top-[28px] z-30 bg-white flex flex-col"
        />

        <!-- [4] 기존 앱 네비게이션 바 -->
        <nav class="bg-white border-t border-slate-100 py-2.5 px-4 flex justify-around flex-shrink-0 z-20 shadow-[0_-4px_20px_-4px_rgba(0,0,0,0.03)] select-none">
          <!-- 1. 자가진단 (Profiling) -->
          <button 
            @click="emit('tabChange', 'profiling')"
            :class="[
              'flex-1 flex flex-col items-center gap-1 transition-all duration-200 active:scale-[0.98]',
              activeTab === 'profiling' ? 'text-blue-600 font-bold' : 'text-slate-400 hover:text-slate-600'
            ]"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
              <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/>
              <rect x="8" y="2" width="8" height="4" rx="1" ry="1"/>
            </svg>
            <span class="text-xs">자가진단</span>
          </button>

          <!-- 2. 안전대응 (Response) -->
          <button 
            @click="emit('tabChange', 'response')"
            :class="[
              'flex-1 flex flex-col items-center gap-1 transition-all duration-200 active:scale-[0.98]',
              activeTab === 'response' ? 'text-blue-600 font-bold' : 'text-slate-400 hover:text-slate-600'
            ]"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
            <span class="text-xs">안전대응</span>
          </button>

          <!-- 3. 리포트 (Report) -->
          <button 
            @click="emit('tabChange', 'report')"
            :class="[
              'flex-1 flex flex-col items-center gap-1 transition-all duration-200 active:scale-[0.98]',
              activeTab === 'report' ? 'text-blue-600 font-bold' : 'text-slate-400 hover:text-slate-600'
            ]"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
              <line x1="18" y1="20" x2="18" y2="10"/>
              <line x1="12" y1="20" x2="12" y2="4"/>
              <line x1="6" y1="20" x2="6" y2="14"/>
            </svg>
            <span class="text-xs">리포트</span>
          </button>
        </nav>

        <!-- [5] 안드로이드 하단 3버튼 소프트웨어 네비게이션 바 -->
        <div class="w-full h-10 bg-slate-50 border-t border-slate-100 flex items-center justify-around flex-shrink-0 z-30 select-none px-6 pb-1">
          <!-- 1. 최근 앱 버튼 (|||) -->
          <button @click="handleRecentApps" class="text-slate-400 hover:text-slate-700 active:scale-90 transition-all p-2 flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 fill-none stroke-current stroke-[2.2]" viewBox="0 0 24 24">
              <path d="M8 6v12M12 6v12M16 6v12"/>
            </svg>
          </button>
          
          <!-- 2. 홈 버튼 (○) -->
          <button @click="handleHome" class="text-slate-400 hover:text-slate-700 active:scale-90 transition-all p-2 flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 fill-none stroke-current stroke-[2.2]" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="6" />
            </svg>
          </button>
          
          <!-- 3. 뒤로 가기 버튼 (<) -->
          <button @click="handleBack" class="text-slate-400 hover:text-slate-700 active:scale-90 transition-all p-2 flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 fill-none stroke-current stroke-[2.2]" viewBox="0 0 24 24">
              <path d="M15 18l-6-6 6-6"/>
            </svg>
          </button>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
/* Disable default scrollbars in simulation shell for phone-like look */
::-webkit-scrollbar {
  width: 4px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}
</style>
