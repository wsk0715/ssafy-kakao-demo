<!-- 5-layer architecture: Layout Layer -->
<!-- Defines the app container and navigation tab shell -->

<script setup lang="ts">
import { useProfilingStore } from '../state/profilingStore'

defineProps<{
  activeTab: 'profiling' | 'training' | 'response' | 'report'
}>()

const emit = defineEmits<{
  (e: 'tabChange', tab: 'profiling' | 'training' | 'response' | 'report'): void
}>()

const profilingStore = useProfilingStore()
</script>

<template>
  <div class="min-h-screen bg-slate-100 text-slate-850 font-sans flex items-center justify-center p-0 md:p-4">
    <!-- Phone Frame Shell Simulator -->
    <div class="w-full max-w-md h-screen md:h-[840px] bg-white border border-slate-200/80 md:rounded-[40px] md:shadow-[0_24px_60px_-15px_rgba(15,23,42,0.12)] overflow-hidden flex flex-col relative md:ring-8 md:ring-slate-100">
      
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

      <!-- Bottom Phone App Navigation Bar -->
      <nav class="bg-white border-t border-slate-100 py-2.5 px-4 flex justify-around flex-shrink-0 z-20 shadow-[0_-4px_20px_-4px_rgba(0,0,0,0.03)]">
        <!-- 1. 자가진단 (Profiling) -->
        <button 
          @click="emit('tabChange', 'profiling')"
          :class="[
            'flex flex-col items-center gap-1 transition-all active:scale-95 duration-200',
            activeTab === 'profiling' ? 'text-blue-600 font-bold scale-105' : 'text-slate-400 hover:text-slate-650'
          ]"
        >
          <span class="text-lg">📋</span>
          <span class="text-[10px]">자가진단</span>
        </button>

        <!-- 2. 모의훈련 (Training) -->
        <button 
          @click="emit('tabChange', 'training')"
          :class="[
            'flex flex-col items-center gap-1 transition-all active:scale-95 duration-200',
            activeTab === 'training' ? 'text-blue-600 font-bold scale-105' : 'text-slate-400 hover:text-slate-650'
          ]"
        >
          <span class="text-lg">🎯</span>
          <span class="text-[10px]">모의훈련</span>
        </button>

        <!-- 3. 안전대응 (Response) -->
        <button 
          @click="emit('tabChange', 'response')"
          :class="[
            'flex flex-col items-center gap-1 transition-all active:scale-95 duration-200',
            activeTab === 'response' ? 'text-blue-600 font-bold scale-105' : 'text-slate-400 hover:text-slate-650'
          ]"
        >
          <span class="text-lg">🛡️</span>
          <span class="text-[10px]">안전대응</span>
        </button>

        <!-- 4. 리포트 (Report) -->
        <button 
          @click="emit('tabChange', 'report')"
          :class="[
            'flex flex-col items-center gap-1 transition-all active:scale-95 duration-200',
            activeTab === 'report' ? 'text-blue-600 font-bold scale-105' : 'text-slate-400 hover:text-slate-650'
          ]"
        >
          <span class="text-lg">📊</span>
          <span class="text-[10px]">리포트</span>
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
