<!-- 5-layer architecture: Component (Page) Layer for Response Match -->

<script setup lang="ts">
import { computed } from 'vue'
import { useReportStore } from '../state/reportStore'

const reportStore = useReportStore()

const failedScenarios = computed(() => {
  if (!reportStore.report) return []
  return reportStore.report.history
    .filter(h => h.result === 'FAILED')
    .map(h => h.title)
})

const simulateDial = (phoneNumber: string, orgName: string) => {
  alert(`[안내] ${orgName}(으)로 모의 전화를 연결합니다: ${phoneNumber}\n실제 상황인 경우 해당 번호로 전화를 걸어 즉시 도움을 청하십시오.`)
}
</script>

<template>
  <div class="p-6 space-y-6 text-left">
    <div class="text-center py-2">
      <h2 class="text-xl font-bold text-slate-800">상황별 대응 안내</h2>
      <p class="text-sm text-slate-500 mt-1">훈련 이력에 맞춤화된 긴급 피싱 대응 연락처와 절차를 제시합니다.</p>
    </div>

    <!-- Failed vector analysis info -->
    <div v-if="failedScenarios.length > 0" class="bg-rose-50 border border-rose-100/80 rounded-2xl p-4 space-y-2">
      <h4 class="text-[10px] font-extrabold uppercase tracking-widest text-rose-600">
        최근 노출된 보안 취약점
      </h4>
      <p class="text-xs text-slate-600 leading-relaxed">
        최근 모의 훈련에서 <strong class="text-rose-600">{{ failedScenarios.length }}건</strong>의 피싱 위험에 노출되었습니다. 이에 따라 맞춤 대응 가이드를 연결합니다.
      </p>
    </div>

    <!-- 상황별 대응 긴급 신고처 (고객센터 레퍼런스 스타일 완벽 이식) -->
    <div class="space-y-2 mt-4 pb-2">
      
      <!-- 1. 경찰청 고객센터 -->
      <div class="space-y-3">
        <h3 class="text-[14px] font-extrabold text-slate-800 text-center">경찰청 피해 신고처</h3>
        <a 
          @click="simulateDial('112', '경찰청')"
          class="block bg-[#f8f9fc] rounded-[24px] p-6 flex justify-between items-center active:scale-[0.99] transition-all text-left mx-1 border border-slate-100/50 shadow-3xs cursor-pointer"
        >
          <div class="space-y-1">
            <span class="text-[17px] font-extrabold text-[#4a5fec] tracking-tight">112 (무료)</span>
            <p class="text-[11px] text-slate-400 font-bold">24시간 연중무휴 (피해 및 지급정지)</p>
          </div>
          
          <!-- 말풍선 + 수화기 결합 아이콘 -->
          <svg xmlns="http://www.w3.org/2000/svg" class="w-9 h-9 stroke-[#4a5fec] fill-none stroke-[1.5]" viewBox="0 0 24 24" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
            <rect x="13" y="1" width="10" height="7" rx="2" fill="white" stroke="#4a5fec" stroke-width="1.5" />
            <path d="M15 8l-1.5 1.5v-1.5h1.5z" fill="#4a5fec" stroke="#4a5fec" stroke-width="1" />
            <circle cx="16" cy="4.5" r="0.6" fill="#4a5fec" stroke="none" />
            <circle cx="18" cy="4.5" r="0.6" fill="#4a5fec" stroke="none" />
            <circle cx="20" cy="4.5" r="0.6" fill="#4a5fec" stroke="none" />
          </svg>
        </a>
      </div>

      <!-- 2. 금융감독원 피해 상담처 -->
      <div class="space-y-3 mt-8">
        <h3 class="text-[14px] font-extrabold text-slate-800 text-center">금융감독원 피해 상담처</h3>
        <a 
          @click="simulateDial('1332', '금융감독원')"
          class="block bg-[#f8f9fc] rounded-[24px] p-6 flex justify-between items-center active:scale-[0.99] transition-all text-left mx-1 border border-slate-100/50 shadow-3xs cursor-pointer"
        >
          <div class="space-y-1">
            <span class="text-[17px] font-extrabold text-[#4a5fec] tracking-tight">1332 (무료)</span>
            <p class="text-[11px] text-slate-400 font-bold">평일 09:00 ~ 18:00 (보이스피싱 피해 환급)</p>
          </div>
          
          <svg xmlns="http://www.w3.org/2000/svg" class="w-9 h-9 stroke-[#4a5fec] fill-none stroke-[1.5]" viewBox="0 0 24 24" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
            <rect x="13" y="1" width="10" height="7" rx="2" fill="white" stroke="#4a5fec" stroke-width="1.5" />
            <path d="M15 8l-1.5 1.5v-1.5h1.5z" fill="#4a5fec" stroke="#4a5fec" stroke-width="1" />
            <circle cx="16" cy="4.5" r="0.6" fill="#4a5fec" stroke="none" />
            <circle cx="18" cy="4.5" r="0.6" fill="#4a5fec" stroke="none" />
            <circle cx="20" cy="4.5" r="0.6" fill="#4a5fec" stroke="none" />
          </svg>
        </a>
      </div>

      <!-- 3. 인터넷진흥원 스팸 제보 -->
      <div class="space-y-3 mt-8">
        <h3 class="text-[14px] font-extrabold text-slate-800 text-center">인터넷진흥원 스팸 제보</h3>
        <a 
          @click="simulateDial('118', '한국인터넷진흥원')"
          class="block bg-[#f8f9fc] rounded-[24px] p-6 flex justify-between items-center active:scale-[0.99] transition-all text-left mx-1 border border-slate-100/50 shadow-3xs cursor-pointer"
        >
          <div class="space-y-1">
            <span class="text-[17px] font-extrabold text-[#4a5fec] tracking-tight">118 (무료)</span>
            <p class="text-[11px] text-slate-400 font-bold">24시간 연중무휴 (스팸 및 개인정보 침해)</p>
          </div>
          
          <svg xmlns="http://www.w3.org/2000/svg" class="w-9 h-9 stroke-[#4a5fec] fill-none stroke-[1.5]" viewBox="0 0 24 24" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
            <rect x="13" y="1" width="10" height="7" rx="2" fill="white" stroke="#4a5fec" stroke-width="1.5" />
            <path d="M15 8l-1.5 1.5v-1.5h1.5z" fill="#4a5fec" stroke="#4a5fec" stroke-width="1" />
            <circle cx="16" cy="4.5" r="0.6" fill="#4a5fec" stroke="none" />
            <circle cx="18" cy="4.5" r="0.6" fill="#4a5fec" stroke="none" />
            <circle cx="20" cy="4.5" r="0.6" fill="#4a5fec" stroke="none" />
          </svg>
        </a>
      </div>

      <!-- 4. 온라인 피해 신고 및 제보 (이메일 문의하기 UI 완벽 이식) -->
      <div class="text-center space-y-4 pb-4 mt-10">
        <h3 class="text-[14px] font-extrabold text-slate-800">온라인 피해 신고 및 제보</h3>
        
        <div>
          <a 
            href="https://ecrm.police.go.kr" 
            target="_blank"
            class="inline-block border border-[#4a5fec] text-[#4a5fec] font-extrabold px-8 py-3.5 rounded-full hover:bg-slate-50 active:scale-95 transition-all text-xs bg-white shadow-3xs"
          >
            보이스피싱 지킴이 바로가기
          </a>
        </div>
        
        <p class="text-[11px] text-slate-400 font-bold leading-relaxed max-w-[260px] mx-auto">
          문의 또는 제안내용을 입력해주시면 이메일로 신속하게 전달 드리겠습니다.
        </p>
      </div>

    </div>

  </div>
</template>

<style scoped>
.text-2xs {
  font-size: 0.7rem;
}
</style>
