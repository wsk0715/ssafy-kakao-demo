<!-- 5-layer architecture: Layout Layer -->
<!-- Defines the app container and navigation tab shell -->

<script setup lang="ts">
import { useProfilingStore } from '../state/profilingStore'
import { useTrainingStore } from '../state/trainingStore'
import IncomingCallOverlay from '../component/IncomingCallOverlay.vue'
import TrainingPage from '../component/TrainingPage.vue'

defineProps<{
  activeTab: 'profiling' | 'response' | 'report'
}>()

const emit = defineEmits<{
  (e: 'tabChange', tab: 'profiling' | 'response' | 'report'): void
}>()

const profilingStore = useProfilingStore()
const trainingStore = useTrainingStore()
</script>

<template>
  <div class="min-h-screen bg-slate-100 text-slate-850 font-sans flex items-center justify-center p-0 xs:p-4">
    <!-- Phone Frame Shell Simulator -->
    <div class="w-full xs:max-w-md h-screen xs:h-[840px] bg-white border border-slate-200/80 xs:rounded-[40px] xs:shadow-[0_24px_60px_-15px_rgba(15,23,42,0.12)] overflow-hidden flex flex-col relative xs:ring-8 xs:ring-slate-100">
      
      <!-- Top Status/Header Bar -->
      <header class="bg-slate-50/90 backdrop-blur-md border-b border-slate-100 px-6 py-4 flex items-center justify-between flex-shrink-0 z-20">
        <div class="flex items-center gap-2">
          <div class="bg-blue-600 text-white font-black px-2 py-0.5 rounded-lg text-sm tracking-tighter">P</div>
          <span class="font-extrabold text-sm tracking-tight text-slate-800">피싱 예방 시뮬레이터</span>
        </div>
        
        <!-- Top Status indicators -->
        <div class="flex items-center gap-2">
          <span 
            v-if="profilingStore.result" 
            :class="[
              'text-[10px] px-2.5 py-0.5 rounded-full font-bold border',
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

      <!-- Main Scrollable Screen Slot -->
      <main class="flex-1 overflow-y-auto bg-slate-50/30">
        <slot />
      </main>

      <!-- Global Incoming Call Overlay -->
      <IncomingCallOverlay />

      <!-- Global Training Overlay for SMS/Email/Warning -->
      <TrainingPage 
        v-if="trainingStore.simStatus === 'SMS_RECEIVED' || trainingStore.simStatus === 'EMAIL_OPENED' || trainingStore.simStatus === 'WARNING_SCREEN'" 
        class="absolute inset-0 z-30 bg-white flex flex-col"
      />

      <!-- Bottom Phone App Navigation Bar -->
      <nav class="bg-white border-t border-slate-100 py-2.5 px-4 flex justify-around flex-shrink-0 z-20 shadow-[0_-4px_20px_-4px_rgba(0,0,0,0.03)]">
        <!-- 1. 자가진단 (Profiling) -->
        <button 
          @click="emit('tabChange', 'profiling')"
          :class="[
            'flex flex-col items-center gap-1 transition-all duration-200 active:scale-[0.98]',
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
            'flex flex-col items-center gap-1 transition-all duration-200 active:scale-[0.98]',
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
            'flex flex-col items-center gap-1 transition-all duration-200 active:scale-[0.98]',
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
