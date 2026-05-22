<!-- 5-layer architecture: Component (Page) Layer for Response Match -->

<script setup lang="ts">
import { computed } from 'vue'
import { useReportStore } from '../state/reportStore'
import { reportService } from '../service/reportService'

const reportStore = useReportStore()

const failedScenarios = computed(() => {
  if (!reportStore.report) return []
  return reportStore.report.history
    .filter(h => h.result === 'FAILED')
    .map(h => h.title)
})

const recommendedContacts = computed(() => {
  return reportService.getResponseContacts(failedScenarios.value)
})

const simulateDial = (phoneNumber: string, orgName: string) => {
  alert(`[안내] ${orgName}(으)로 모의 전화를 연결합니다: ${phoneNumber}\n실제 상황인 경우 해당 번호로 전화를 걸어 즉시 도움을 청하십시오.`)
}
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="text-center py-2">
      <h2 class="text-xl font-bold text-slate-800">상황별 대응 안내</h2>
      <p class="text-sm text-slate-500 mt-1">훈련 이력에 맞춤화된 긴급 피싱 대응 연락처와 절차를 제시합니다.</p>
    </div>

    <!-- Failed vector analysis info -->
    <div v-if="failedScenarios.length > 0" class="bg-rose-50 border border-rose-100 rounded-2xl p-4 space-y-2">
      <h4 class="text-2xs font-extrabold uppercase tracking-widest text-rose-600">
        최근 노출된 보안 취약점
      </h4>
      <p class="text-xs text-slate-600 leading-relaxed">
        최근 모의 훈련에서 <strong class="text-rose-600">{{ failedScenarios.length }}건</strong>의 피싱 위험에 노출되었습니다. 이에 따라 맞춤 대응 가이드를 연결합니다.
      </p>
    </div>

    <!-- Dynamically Matched Contacts -->
    <div class="space-y-3.5">
      <div 
        v-for="(c, idx) in recommendedContacts" 
        :key="idx"
        class="bg-white border border-slate-200/80 rounded-2xl p-[18px] space-y-3 shadow-[0_4px_20px_-4px_rgba(0,0,0,0.02)] animate-fade-in"
      >
        <div class="flex items-center justify-between">
          <div>
            <span 
              :class="[
                'text-[10px] font-extrabold px-2 py-0.5 rounded-full tracking-wider uppercase border',
                c.urgency === 'HIGH' ? 'bg-rose-50 text-rose-600 border-rose-100' : 'bg-amber-50 text-amber-600 border-amber-100'
              ]"
            >
              {{ c.urgency === 'HIGH' ? '긴급' : '보통' }}
            </span>
            <h3 class="text-sm font-bold text-slate-800 mt-1.5">{{ c.orgName }}</h3>
          </div>
          <span class="text-sm font-bold text-blue-600 tracking-wider">{{ c.phoneNumber }}</span>
        </div>

        <p class="text-xs text-slate-550 leading-normal">
          {{ c.description }}
        </p>

        <button 
          @click="simulateDial(c.phoneNumber, c.orgName)"
          class="w-full bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200 font-bold text-sm py-2.5 px-4 rounded-xl transition-all active:scale-[0.99] flex items-center justify-center gap-1.5"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
          </svg>
          연결 상담 요청
        </button>
      </div>
    </div>

    <!-- Quick action links for banks -->
    <div class="bg-white border border-slate-200/80 rounded-2xl p-5 space-y-4 shadow-[0_4px_20px_-4px_rgba(0,0,0,0.02)]">
      <h4 class="text-sm font-bold text-slate-700">주요 금융기관 고객센터</h4>
      <div class="grid grid-cols-2 gap-2.5 text-xs">
        <a 
          href="https://www.kbstar.com" 
          target="_blank"
          class="bg-slate-50/50 border border-slate-200 p-3 rounded-xl text-slate-600 font-bold hover:bg-slate-50 hover:border-slate-350 text-center flex flex-col justify-center items-center gap-1.5 transition-all active:scale-[0.99]"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 fill-none stroke-slate-500 stroke-2" viewBox="0 0 24 24">
            <path d="M3 21h18"/>
            <path d="M19 21v-7"/>
            <path d="M5 21v-7"/>
            <path d="M12 21v-7"/>
            <path d="M2 11l10-8 10 8"/>
          </svg>
          <span>국민은행 (1588-9999)</span>
        </a>
        <a 
          href="https://www.shinhan.com" 
          target="_blank"
          class="bg-slate-50/50 border border-slate-200 p-3 rounded-xl text-slate-600 font-bold hover:bg-slate-50 hover:border-slate-350 text-center flex flex-col justify-center items-center gap-1.5 transition-all active:scale-[0.99]"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 fill-none stroke-slate-500 stroke-2" viewBox="0 0 24 24">
            <path d="M3 21h18"/>
            <path d="M19 21v-7"/>
            <path d="M5 21v-7"/>
            <path d="M12 21v-7"/>
            <path d="M2 11l10-8 10 8"/>
          </svg>
          <span>신한은행 (1599-8000)</span>
        </a>
      </div>
    </div>
  </div>
</template>

<style scoped>
.text-2xs {
  font-size: 0.7rem;
}
</style>
