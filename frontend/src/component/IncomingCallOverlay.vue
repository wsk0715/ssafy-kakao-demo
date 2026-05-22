<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { useTrainingStore } from '../state/trainingStore'
import { trainingService } from '../service/trainingService'

const store = useTrainingStore()

// Call duration timer
const duration = ref(0)
let timerId: any = null

// Speaker/Mute state
const isMutedLocal = ref(false)
const isSpeakerOn = ref(false)

const formatDuration = (sec: number) => {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

watch(() => store.simStatus as any, (newStatus: any) => {
  if (newStatus === 'CONNECTED') {
    duration.value = 0
    if (timerId) clearInterval(timerId)
    timerId = setInterval(() => {
      duration.value++
    }, 1000)
  } else if (newStatus !== 'RINGING' && newStatus !== 'CONNECTED') {
    if (timerId) {
      clearInterval(timerId)
      timerId = null
    }
  }
})

onUnmounted(() => {
  if (timerId) clearInterval(timerId)
})

const handleDecline = () => {
  trainingService.cancelSimulation()
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
</script>

<template>
  <Transition name="slide-up">
    <div 
      v-if="store.simStatus === 'RINGING' || store.simStatus === 'CONNECTED'" 
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
      <div v-else-if="store.simStatus === 'CONNECTED' && store.activeScenario" class="flex-1 flex flex-col justify-between py-16 px-8 animate-fade-in">
        
        <!-- Active Call Header -->
        <div class="text-center mt-12 space-y-2">
          <h2 class="text-4xl font-black tracking-tight text-white whitespace-pre-line">
            {{ store.activeScenario.sender }}
          </h2>
          <p class="text-base text-emerald-400 font-extrabold tracking-widest uppercase">
            {{ formatDuration(duration) }}
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
</style>
