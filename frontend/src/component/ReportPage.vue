<!-- 5-layer architecture: Component (Page) Layer for Vulnerability Report -->

<script setup lang="ts">
import { onMounted } from 'vue'
import { useReportStore } from '../state/reportStore'
import { reportService } from '../service/reportService'

const store = useReportStore()

onMounted(async () => {
  if (!store.report) {
    await reportService.loadMonthlyReport()
  }
})
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="text-center py-2">
      <h2 class="text-xl font-bold text-slate-800">월간 취약점 리포트</h2>
      <p class="text-xs text-slate-500 mt-1">사용자의 가상 훈련 대응 데이터를 분석한 결과 보고서입니다.</p>
    </div>

    <!-- Loading State -->
    <div v-if="store.isLoading || !store.report" class="flex flex-col items-center justify-center py-12 space-y-3">
      <span class="animate-spin h-6 w-6 border-2 border-blue-600 border-t-transparent rounded-full"></span>
      <p class="text-xs text-slate-550">분석 데이터를 집계하고 있습니다...</p>
    </div>

    <!-- Report Dashboard -->
    <div v-else class="space-y-5 animate-fade-in">
      
      <!-- Key Stats Panel -->
      <div class="bg-white border border-slate-200/80 rounded-2xl p-5 space-y-4 shadow-[0_4px_20px_-4px_rgba(0,0,0,0.02)]">
        <div class="flex justify-between items-center border-b border-slate-100 pb-3">
          <span class="text-xs font-bold text-slate-400">분석 기간</span>
          <span class="text-xs font-black text-slate-800">{{ store.report.month }}</span>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div class="bg-slate-50/50 p-[18px] rounded-xl border border-slate-200 text-center">
            <p class="text-[10px] font-bold text-slate-450">총 훈련 횟수</p>
            <p class="text-xl font-black text-slate-800 mt-1.5">{{ store.report.totalSimulations }}회</p>
          </div>
          <div class="bg-slate-50/50 p-[18px] rounded-xl border border-slate-200 text-center">
            <p class="text-[10px] font-bold text-slate-450">경고 행동 노출</p>
            <p class="text-xl font-black text-rose-600 mt-1.5">{{ store.report.riskyActionsCount }}회</p>
          </div>
        </div>

        <!-- Vulnerability Progress rate -->
        <div class="space-y-2 pt-2">
          <div class="flex justify-between items-center text-xs">
            <span class="text-slate-550 font-bold">종합 보안 취약율</span>
            <span class="font-extrabold text-rose-600">{{ store.report.vulnerabilityRate }}%</span>
          </div>
          <div class="w-full bg-slate-100 rounded-full h-2 overflow-hidden border border-slate-200/50">
            <div 
              class="bg-rose-500 h-full rounded-full transition-all duration-500" 
              :style="{ width: `${store.report.vulnerabilityRate}%` }"
            ></div>
          </div>
        </div>
      </div>

      <!-- Top Vulnerability Types -->
      <div class="bg-white border border-slate-200/80 rounded-2xl p-5 space-y-4 shadow-[0_4px_20px_-4px_rgba(0,0,0,0.02)]">
        <h3 class="text-xs font-bold uppercase tracking-wider text-slate-400">
          나의 취약 피싱 유형 TOP 3
        </h3>
        
        <div class="space-y-4">
          <div 
            v-for="(v, idx) in store.report.topVulnerabilities" 
            :key="idx"
            class="space-y-2"
          >
            <div class="flex justify-between text-2xs font-bold">
              <span class="text-slate-700">{{ idx + 1 }}. {{ v.type }}</span>
              <span class="text-slate-500">{{ v.score }}점</span>
            </div>
            <div class="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden">
              <div 
                class="bg-blue-600 h-full rounded-full" 
                :style="{ width: `${v.score}%` }"
              ></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Training History Log -->
      <div class="bg-white border border-slate-200/80 rounded-2xl p-5 space-y-4 shadow-[0_4px_20px_-4px_rgba(0,0,0,0.02)]">
        <h3 class="text-xs font-bold uppercase tracking-wider text-slate-400">
          훈련 대응 상세 로그
        </h3>

        <div class="max-h-60 overflow-y-auto pr-1 space-y-2.5">
          <div 
            v-for="(h, idx) in store.report.history" 
            :key="idx"
            class="bg-slate-50/50 border border-slate-200 rounded-xl p-3 flex items-center justify-between gap-3 text-2xs"
          >
            <div class="min-w-0">
              <span class="text-slate-400 text-[10px] font-semibold">{{ h.date }}</span>
              <h4 class="truncate font-bold text-slate-700 mt-0.5">{{ h.title }}</h4>
            </div>
            
            <span 
              :class="[
                'text-[9px] font-extrabold px-2 py-0.5 rounded border tracking-wide',
                h.result === 'SUCCESS' ? 'bg-emerald-50 text-emerald-600 border-emerald-100' : 'bg-rose-50 text-rose-600 border-rose-100'
              ]"
            >
              {{ h.result === 'SUCCESS' ? '예방성공' : '대응실패' }}
            </span>
          </div>
        </div>
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
</style>
