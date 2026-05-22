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
            <div class="relative w-20 h-20 rounded-full bg-slate-900 border border-slate-800 flex items-center justify-center text-3xl shadow-xl">
              📞
            </div>
          </div>
        </div>

        <!-- Ringing Actions -->
        <div class="flex items-center justify-between w-full px-14 mb-8">
          <!-- Accept Button (Left) -->
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

          <!-- Decline Button (Right) -->
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
        <div class="grid grid-cols-3 gap-y-8 gap-x-8 max-w-xs mx-auto my-12 text-center">
          <button @click="toggleMute" :class="['flex flex-col items-center justify-center w-20 h-20 mx-auto rounded-full transition-all', isMutedLocal ? 'bg-white text-slate-950 shadow-lg' : 'bg-white/10 hover:bg-white/20 text-white']">
            <span class="text-3xl">🎙️</span>
            <span class="text-[13px] font-bold mt-1.5">소리 끔</span>
          </button>
          <button class="flex flex-col items-center justify-center w-20 h-20 mx-auto bg-white/10 hover:bg-white/20 text-white rounded-full opacity-40 cursor-not-allowed">
            <span class="text-3xl">⌨️</span>
            <span class="text-[13px] font-bold mt-1.5">키패드</span>
          </button>
          <button @click="toggleSpeaker" :class="['flex flex-col items-center justify-center w-20 h-20 mx-auto rounded-full transition-all', isSpeakerOn ? 'bg-white text-slate-950 shadow-lg' : 'bg-white/10 hover:bg-white/20 text-white']">
            <span class="text-3xl">🔊</span>
            <span class="text-[13px] font-bold mt-1.5">스피커</span>
          </button>
          <button class="flex flex-col items-center justify-center w-20 h-20 mx-auto bg-white/10 hover:bg-white/20 text-white rounded-full opacity-40 cursor-not-allowed">
            <span class="text-3xl">➕</span>
            <span class="text-[13px] font-bold mt-1.5">통화 추가</span>
          </button>
          <button class="flex flex-col items-center justify-center w-20 h-20 mx-auto bg-white/10 hover:bg-white/20 text-white rounded-full opacity-40 cursor-not-allowed">
            <span class="text-3xl">🎥</span>
            <span class="text-[13px] font-bold mt-1.5">FaceTime</span>
          </button>
          <button class="flex flex-col items-center justify-center w-20 h-20 mx-auto bg-white/10 hover:bg-white/20 text-white rounded-full opacity-40 cursor-not-allowed">
            <span class="text-3xl">👤</span>
            <span class="text-[13px] font-bold mt-1.5">연락처</span>
          </button>
        </div>

        <!-- Hang Up Button -->
        <div class="flex flex-col items-center mt-auto mb-8">
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
