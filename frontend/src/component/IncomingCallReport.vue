<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  activeAnalysis: {
    date: string
    scenarioId: string
    scenarioTitle: string
    scenarioType: string
    duration: number
    hangUpStepIndex: number
    hangUpStepName: string
    vulnerabilityStatus: string
    vulnerabilityExplanation: string
    feedback: string
    techniques: Array<{
      step: string
      name: string
      desc: string
    }>
    result: string
  }
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const activeTab = ref('analysis')

// 주저리주저리 긴 DB 설명문을 걷어내고, 등급별로 1줄로 떨어지는 초간결 안심 분석 멘트 매핑
const compactExplanationMap: Record<string, string> = {
  SAFE: '피싱 의심 징후를 조기에 감지하고 통화를 종료하여 자산 피해를 성공적으로 차단했습니다.',
  WARNING: '통화를 중단했으나, 의심 멘트 노출 시간이 다소 길어 차단 지연 우려가 있습니다.',
  CRITICAL: '최종 이체 및 정보 강요 단계까지 통화가 유지되어 피싱 피해에 극도로 노출되었습니다.'
}

const compactFeedbackMap: Record<string, string> = {
  SAFE: '수사기관/금융사는 전화상으로 돈이나 정보를 요구하지 않습니다. 즉시 끊는 대처가 가장 안전합니다.',
  WARNING: '기관 사칭이나 금전 권유 낌새가 보이는 즉시 전화를 바로 끊는 습관을 기르십시오.',
  CRITICAL: '전화상으로 임시 가상계좌 송금이나 보안 입력을 요구하면 100% 사기이므로 즉시 끊으십시오.'
}
</script>

<template>
  <div class="flex-1 flex flex-col justify-between py-6 px-5 overflow-hidden animate-fade-in text-slate-800 bg-white h-full font-sans">
    
    <!-- 상단 등급 및 멘트 영역 -->
    <div class="text-left mb-4 mt-2 px-1 pb-4 border-b border-slate-100">
      <div v-if="activeAnalysis.vulnerabilityStatus === 'SAFE'" class="space-y-1">
        <div class="flex items-center gap-2">
          <h2 class="text-[22px] font-black tracking-tight text-slate-900">피싱 대처 결과</h2>
          <span class="bg-blue-50 text-blue-600 border border-blue-200 text-[10.5px] font-black px-2.5 py-0.5 rounded-full">안전</span>
        </div>
        <p class="text-xs text-slate-400 font-bold mt-1">즉각적인 통화 종료로 피해를 성공적으로 차단했습니다.</p>
      </div>
      <div v-else-if="activeAnalysis.vulnerabilityStatus === 'WARNING'" class="space-y-1">
        <div class="flex items-center gap-2">
          <h2 class="text-[22px] font-black tracking-tight text-slate-900">피싱 대처 결과</h2>
          <span class="bg-amber-50 text-amber-600 border border-amber-200 text-[10.5px] font-black px-2.5 py-0.5 rounded-full">주의</span>
        </div>
        <p class="text-xs text-slate-400 font-bold mt-1">대화 지속 시간이 다소 길어 주의가 필요합니다.</p>
      </div>
      <div v-else class="space-y-1">
        <div class="flex items-center gap-2">
          <h2 class="text-[22px] font-black tracking-tight text-slate-900">피싱 대처 결과</h2>
          <span class="bg-rose-50 text-rose-600 border border-rose-200 text-[10.5px] font-black px-2.5 py-0.5 rounded-full">위험</span>
        </div>
        <p class="text-xs text-slate-400 font-bold mt-1">이체 및 개인정보 유도 단계까지 노출되었습니다.</p>
      </div>
    </div>

    <!-- 최상단 전환 탭 메뉴 -->
    <div class="flex items-center justify-start gap-6 pb-2 px-1 border-b border-slate-100">
      <button 
        @click="activeTab = 'analysis'"
        :class="[
          'text-[14.5px] font-bold transition-all pb-2 px-1 relative focus:outline-none tracking-tight',
          activeTab === 'analysis' 
            ? 'text-blue-600 font-extrabold after:absolute after:bottom-0 after:left-0 after:right-0 after:h-[3px] after:bg-blue-600 after:rounded-full' 
            : 'text-slate-400 hover:text-slate-700'
        ]"
      >
        취약점 분석
      </button>
      <button 
        @click="activeTab = 'guide'"
        :class="[
          'text-[14.5px] font-bold transition-all pb-2 px-1 relative focus:outline-none tracking-tight',
          activeTab === 'guide' 
            ? 'text-blue-600 font-extrabold after:absolute after:bottom-0 after:left-0 after:right-0 after:h-[3px] after:bg-blue-600 after:rounded-full' 
            : 'text-slate-400 hover:text-slate-700'
        ]"
      >
        대응 가이드
      </button>
    </div>

    <!-- 내용 스크롤 영역 (개별 요소의 카드화 모듈 구조) -->
    <div class="flex-1 overflow-y-auto pr-1 text-xs scroll-container mt-4 px-1 space-y-4">
      
      <!-- TAB 1: 취약점 분석 -->
      <div v-if="activeTab === 'analysis'" class="space-y-4 animate-fade-in">
        
        <!-- CARD A: 잘 대처했어요 / 주의가 필요해요 대비 목록 -->
        <div class="bg-slate-50/60 border border-slate-100/80 rounded-2xl p-4.5 text-left space-y-4">
          <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">훈련 행동 정밀 진단</p>
          
          <!-- 1. 잘 대처했어요 -->
          <div class="space-y-2">
            <div class="text-[11px] text-blue-600 font-extrabold flex items-center gap-1 tracking-wider uppercase">
              <span>잘 대처했어요</span>
              <span class="text-xs font-black">↑</span>
            </div>
            <div class="flex flex-wrap gap-2">
              <template v-if="activeAnalysis.vulnerabilityStatus === 'SAFE'">
                <div class="bg-white border border-slate-100/80 shadow-3xs rounded-xl px-3 py-2 text-xs text-slate-700 font-bold">
                  골든타임 내 즉각 회피
                </div>
                <div class="bg-white border border-slate-100/80 shadow-3xs rounded-xl px-3 py-2 text-xs text-slate-700 font-bold">
                  신속한 통화 종료
                </div>
              </template>
              <template v-if="activeAnalysis.vulnerabilityStatus === 'WARNING'">
                <div class="bg-white border border-slate-100/80 shadow-3xs rounded-xl px-3 py-2 text-xs text-slate-700 font-bold">
                  최종 위험 단계 이전 통화 중단
                </div>
              </template>
              <template v-if="activeAnalysis.vulnerabilityStatus === 'CRITICAL'">
                <div class="bg-white border border-slate-100/80 shadow-3xs rounded-xl px-3 py-2 text-xs text-slate-700 font-bold">
                  모의 상황 조기 인지 및 훈련 종료
                </div>
              </template>
            </div>
          </div>

          <!-- 2. 주의가 필요해요 -->
          <div class="space-y-2 pt-4 border-t border-slate-200/50">
            <div class="text-[11px] text-rose-500 font-extrabold flex items-center gap-1 tracking-wider uppercase">
              <span>주의가 필요해요</span>
              <span class="text-xs font-black">↓</span>
            </div>
            <div class="flex flex-wrap gap-2">
              <template v-if="activeAnalysis.vulnerabilityStatus === 'SAFE'">
                <div class="bg-white border border-slate-100/80 shadow-3xs rounded-xl px-3 py-2 text-xs text-slate-700 font-bold">
                  모르는 번호 수신 차단 설정 필요
                </div>
              </template>
              <template v-if="activeAnalysis.vulnerabilityStatus === 'WARNING'">
                <div class="bg-white border border-slate-100/80 shadow-3xs rounded-xl px-3 py-2 text-xs text-slate-700 font-bold">
                  사기범과의 통화 지속 (10초 이상)
                </div>
                <div class="bg-white border border-slate-100/80 shadow-3xs rounded-xl px-3 py-2 text-xs text-slate-700 font-bold">
                  심리적 지배 노출 우려
                </div>
              </template>
              <template v-if="activeAnalysis.vulnerabilityStatus === 'CRITICAL'">
                <div class="bg-white border border-slate-100/80 shadow-3xs rounded-xl px-3 py-2 text-xs text-slate-700 font-bold">
                  강압적 영장 협박에 취약
                </div>
                <div class="bg-white border border-slate-100/80 shadow-3xs rounded-xl px-3 py-2 text-xs text-slate-700 font-bold">
                  임시 계좌 송금 유도 노출
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- CARD B: 종합 평가 소견 (초간결 매핑 출력) -->
        <div class="bg-slate-50/60 border border-slate-100/80 rounded-2xl p-4.5 text-left space-y-1">
          <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">진단 소견</p>
          <p class="text-slate-650 leading-relaxed font-bold text-[12px]">
            {{ compactExplanationMap[activeAnalysis.vulnerabilityStatus] || activeAnalysis.vulnerabilityExplanation }}
          </p>
        </div>

        <!-- CARD C: 시나리오 진행 흐름 타임라인 -->
        <div class="bg-slate-50/60 border border-slate-100/80 rounded-2xl p-4.5 text-left space-y-3">
          <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider pl-0.5">시나리오 진행 흐름</p>
          
          <div class="relative pl-4 space-y-3 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-[2px] before:bg-slate-200">
            <div 
              v-for="(tech, tIdx) in activeAnalysis.techniques" 
              :key="tIdx"
              :class="[
                'relative p-3.5 rounded-xl border transition-all text-left flex items-center justify-between shadow-3xs',
                tIdx === activeAnalysis.hangUpStepIndex 
                  ? 'border-blue-200 bg-white shadow-xs text-slate-800' 
                  : 'border-slate-150/40 opacity-60 text-slate-450 bg-white/70'
              ]"
            >
              <!-- 타임라인 불릿 디자인 -->
              <div 
                :class="[
                  'absolute -left-[17px] top-1/2 -translate-y-1/2 w-2 h-2 rounded-full border-2',
                  tIdx === activeAnalysis.hangUpStepIndex 
                    ? 'bg-blue-600 border-blue-200 ring-2 ring-blue-100' 
                    : 'bg-white border-slate-300'
                  ]"
              ></div>
              
              <h6 class="font-bold text-xs" :class="tIdx === activeAnalysis.hangUpStepIndex ? 'text-slate-900' : 'text-slate-500'">
                {{ tIdx + 1 }}단계. {{ tech.name }}
              </h6>
              <span v-if="tIdx === activeAnalysis.hangUpStepIndex" class="bg-blue-500 text-white text-[9px] font-extrabold px-2 py-0.5 rounded-lg shadow-2xs">
                차단
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- TAB 2: 안전 대응 가이드 -->
      <div v-else-if="activeTab === 'guide'" class="space-y-4 animate-fade-in mt-1">
        
        <!-- CARD A: 피해 예방 수칙 -->
        <div class="bg-slate-50/60 border border-slate-100/80 rounded-2xl p-4.5 text-left space-y-3">
          <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider pl-0.5">피해 예방 수칙</p>
          
          <div class="space-y-2.5">
            <div class="bg-white border border-slate-150/40 rounded-xl p-4 flex gap-4 items-center shadow-3xs">
              <span class="text-2xl font-black text-blue-600/80 leading-none">01</span>
              <span class="text-xs text-slate-800 font-bold leading-tight">의심 전화 즉시 끊기</span>
            </div>
            
            <div class="bg-white border border-slate-150/40 rounded-xl p-4 flex gap-4 items-center shadow-3xs">
              <span class="text-2xl font-black text-blue-600/80 leading-none">02</span>
              <span class="text-xs text-slate-800 font-bold leading-tight">출처 불명 앱/링크 설치 금지</span>
            </div>
            
            <div class="bg-white border border-slate-150/40 rounded-xl p-4 flex gap-4 items-center shadow-3xs">
              <span class="text-2xl font-black text-blue-600/80 leading-none">03</span>
              <span class="text-xs text-slate-800 font-bold leading-tight">공식 대표 번호 체크</span>
            </div>
          </div>
        </div>

        <!-- CARD B: 훈련 총평 (초간결 매핑 출력) -->
        <div class="bg-slate-50/60 border border-slate-100/80 rounded-2xl p-4.5 text-left space-y-1">
          <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">훈련 총평</p>
          <p class="text-slate-650 leading-relaxed font-bold text-[12px]">
            {{ compactFeedbackMap[activeAnalysis.vulnerabilityStatus] || activeAnalysis.feedback }}
          </p>
        </div>

        <!-- CARD C: 피해 신고처 -->
        <div class="bg-slate-50/60 border border-slate-100/80 rounded-2xl p-4.5 text-left space-y-3">
          <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider pl-0.5">피해 신고처</p>
          <div class="bg-white border border-slate-150/40 rounded-xl p-4 space-y-3 shadow-3xs">
            
            <div class="flex justify-between items-center text-slate-500 font-bold text-xs">
              <span>경찰청 (피해 신고)</span>
              <a href="tel:112" class="bg-slate-50 border border-slate-250/30 text-blue-600 font-black px-3.5 py-1.5 rounded-xl hover:bg-slate-100 active:scale-95 transition-all shadow-3xs">
                112
              </a>
            </div>
            
            <div class="flex justify-between items-center text-slate-500 font-bold text-xs pt-2.5 border-t border-slate-100">
              <span>금융감독원 (피해 상담)</span>
              <a href="tel:1332" class="bg-slate-50 border border-slate-250/30 text-blue-600 font-black px-3.5 py-1.5 rounded-xl hover:bg-slate-100 active:scale-95 transition-all shadow-3xs">
                1332
              </a>
            </div>
            
            <div class="flex justify-between items-center text-slate-500 font-bold text-xs pt-2.5 border-t border-slate-100">
              <span>KISA (스팸 제보)</span>
              <a href="tel:118" class="bg-slate-50 border border-slate-250/30 text-blue-600 font-black px-3.5 py-1.5 rounded-xl hover:bg-slate-100 active:scale-95 transition-all shadow-3xs">
                118
              </a>
            </div>

          </div>
        </div>
      </div>
    </div>

    <!-- Confirm Button -->
    <button 
      @click="emit('close')"
      class="w-full bg-blue-600 hover:bg-blue-700 active:scale-[0.99] transition-all text-white font-extrabold text-sm py-4 px-4 rounded-2xl shadow-[0_6px_24px_rgba(37,99,235,0.15)] focus:outline-none tracking-tight mt-4"
    >
      리포트 닫기 및 훈련 종료
    </button>
  </div>
</template>

<style scoped>
/* Custom Scrollbar for Report */
.scroll-container {
  scrollbar-width: thin;
}
.scroll-container::-webkit-scrollbar {
  width: 4px;
}
.scroll-container::-webkit-scrollbar-track {
  background: transparent;
}
.scroll-container::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
}
.scroll-container::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.15);
}
</style>
