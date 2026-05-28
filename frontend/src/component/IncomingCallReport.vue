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

</script>

<template>
  <div class="flex-1 flex flex-col justify-between py-6 px-6 overflow-hidden animate-fade-in text-slate-800 bg-white h-full">
    <!-- 최상단 전환 메뉴 (좌측 정렬 & 슬림 밑줄 피드백) -->
    <div class="flex items-center justify-start gap-6 py-1 px-1 border-b border-slate-100 mb-4">
      <button 
        @click="activeTab = 'analysis'"
        :class="[
          'text-xs font-bold transition-all pb-2 px-0.5 relative focus:outline-none',
          activeTab === 'analysis' 
            ? 'text-blue-600 font-extrabold after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-blue-600' 
            : 'text-slate-400 hover:text-slate-700'
        ]"
      >
        취약점 분석
      </button>
      <button 
        @click="activeTab = 'guide'"
        :class="[
          'text-xs font-bold transition-all pb-2 px-0.5 relative focus:outline-none',
          activeTab === 'guide' 
            ? 'text-blue-600 font-extrabold after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-blue-600' 
            : 'text-slate-400 hover:text-slate-700'
        ]"
      >
        대응 가이드
      </button>
    </div>

    <!-- Scrollable Report Contents -->
    <div class="flex-1 overflow-y-auto pr-1 text-xs scroll-container">
      
      <!-- TAB 1: 취약점 분석 -->
      <div v-if="activeTab === 'analysis'" class="space-y-4 animate-fade-in">
        
        <!-- 종합 취약 등급 (플랫/모노톤 디자인) -->
        <div class="border border-slate-100 rounded-xl p-3.5 bg-white text-left space-y-1">
          <p class="text-[9px] font-extrabold text-slate-400 uppercase tracking-widest">종합 취약 등급</p>
          <div class="flex items-baseline justify-between">
            <span class="text-base font-extrabold tracking-tight text-slate-800">
              {{ 
                activeAnalysis.vulnerabilityStatus === 'SAFE' ? '안전 (신속 대응)' : 
                activeAnalysis.vulnerabilityStatus === 'WARNING' ? '주의 (경보 인지)' : 
                '위험 (송금/해킹 고위험)' 
              }}
            </span>
            <span class="text-[10px] text-slate-500 font-bold">
              {{ activeAnalysis.hangUpStepName }} 단계 차단
            </span>
          </div>
        </div>

        <!-- 행동 지표 분석 카드 (우수/취약 요인 콤팩트 리스트) -->
        <div class="bg-white border border-slate-100 rounded-xl p-3.5 space-y-3 text-left">
          <div class="font-extrabold text-slate-800 text-[11px] pb-1 border-b border-slate-100">
            훈련 행동 정밀 진단
          </div>

          <!-- 우수 요인 -->
          <div class="space-y-1.5">
            <div class="text-[10px] text-blue-600 font-bold">우수 요인</div>
            <ul class="space-y-1 text-[10px] text-slate-500 font-semibold pl-1">
              <template v-if="activeAnalysis.vulnerabilityStatus === 'SAFE'">
                <li class="flex items-start gap-1">
                  <span>•</span>
                  <span><strong>즉각 회피:</strong> 골든타임 내에 전화를 신속히 끊고 피해를 원천 차단했습니다.</span>
                </li>
              </template>
              <template v-if="activeAnalysis.vulnerabilityStatus === 'WARNING'">
                <li class="flex items-start gap-1">
                  <span>•</span>
                  <span><strong>피해 방지:</strong> 금융 거래나 계좌 이체 등의 최종 위험 단계 이전에 중단했습니다.</span>
                </li>
              </template>
              <template v-if="activeAnalysis.vulnerabilityStatus === 'CRITICAL'">
                <li class="flex items-start gap-1">
                  <span>•</span>
                  <span><strong>훈련 종료:</strong> 모의 상황임을 조기에 인지하고 안전 조치를 진행했습니다.</span>
                </li>
              </template>
            </ul>
          </div>

          <!-- 취약 요인 -->
          <div class="space-y-1.5 pt-2 border-t border-slate-100/60">
            <div class="text-[10px] text-slate-400 font-bold">취약 요인</div>
            <ul class="space-y-1 text-[10px] text-slate-500 font-semibold pl-1">
              <template v-if="activeAnalysis.vulnerabilityStatus === 'SAFE'">
                <li class="flex items-start gap-1">
                  <span>•</span>
                  <span><strong>유입 주의:</strong> 모르는 번호로부터 오는 전화를 사전에 차단 및 방지하는 습관 권장.</span>
                </li>
              </template>
              <template v-if="activeAnalysis.vulnerabilityStatus === 'WARNING'">
                <li class="flex items-start gap-1">
                  <span>•</span>
                  <span><strong>대화 지속:</strong> 사기범과 10초 이상 통화하여 심리적 지배 상태에 노출될 우려가 있습니다.</span>
                </li>
              </template>
              <template v-if="activeAnalysis.vulnerabilityStatus === 'CRITICAL'">
                <li class="flex items-start gap-1">
                  <span>•</span>
                  <span><strong>고위험 노출:</strong> 고압적 영장 협박에 취약하여 가상 계좌 이체 단계까지 전화를 지속했습니다.</span>
                </li>
              </template>
            </ul>
          </div>
        </div>

        <!-- 진단 소견 요약 -->
        <div class="space-y-1 text-left">
          <div class="font-extrabold text-slate-500 text-[10px] pl-1">진단 소견</div>
          <div class="bg-white border border-slate-100 rounded-xl p-3.5 text-slate-650 leading-relaxed font-semibold text-[11px]">
            {{ activeAnalysis.vulnerabilityExplanation }}
          </div>
        </div>

        <!-- Phishing Techniques Timeline (수직선 타임라인 슬림화) -->
        <div class="space-y-2 text-left">
          <div class="font-extrabold text-slate-500 text-[10px] pl-1">시나리오 진행 흐름</div>
          
          <div class="relative pl-4 space-y-3 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-[1px] before:bg-slate-200">
            <div 
              v-for="(tech, tIdx) in activeAnalysis.techniques" 
              :key="tIdx"
              :class="[
                'relative p-2.5 rounded-lg border transition-all text-left flex flex-col gap-0.5',
                tIdx === activeAnalysis.hangUpStepIndex 
                  ? 'border-blue-100 bg-blue-50/20 text-slate-800' 
                  : 'border-slate-100/60 opacity-60 text-slate-400'
              ]"
            >
              <!-- 타임라인 불릿 -->
              <div 
                :class="[
                  'absolute -left-[15px] top-3.5 w-1.5 h-1.5 rounded-full border',
                  tIdx === activeAnalysis.hangUpStepIndex 
                    ? 'bg-blue-600 border-blue-600' 
                    : 'bg-slate-250 border-slate-200'
                ]"
              ></div>
              
              <div class="flex items-center gap-1.5">
                <h6 class="font-extrabold text-[11px]" :class="tIdx === activeAnalysis.hangUpStepIndex ? 'text-slate-800' : 'text-slate-500'">{{ tech.name }}</h6>
                <span v-if="tIdx === activeAnalysis.hangUpStepIndex" class="text-[8px] text-blue-600 font-bold">
                  (여기서 통화 차단)
                </span>
              </div>
              <p class="text-[10px] leading-relaxed" :class="tIdx === activeAnalysis.hangUpStepIndex ? 'text-slate-500' : 'text-slate-450'">{{ tech.desc }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- TAB 2: 안전 대응 가이드 -->
      <div v-else-if="activeTab === 'guide'" class="space-y-4 animate-fade-in mt-1">
        <!-- 3대 행동 강령 -->
        <div class="space-y-2 text-left">
          <div class="font-extrabold text-slate-500 text-[10px] pl-1">피해 예방 3대 수칙</div>
          
          <div class="bg-white border border-slate-100 rounded-xl p-3.5 space-y-3">
            <!-- 수칙 1 -->
            <div class="flex gap-2.5 items-start">
              <span class="text-blue-600 font-extrabold text-xs">01</span>
              <div class="space-y-0.5">
                <h6 class="font-extrabold text-xs text-slate-800">의심 전화 즉시 끊기</h6>
                <p class="text-[10px] text-slate-500 font-semibold leading-relaxed">
                  수사기관과 금융사는 절대 전화상으로 송금이나 개인정보를 요구하지 않습니다.
                </p>
              </div>
            </div>

            <!-- 수칙 2 -->
            <div class="flex gap-2.5 items-start pt-2.5 border-t border-slate-100/60">
              <span class="text-blue-600 font-extrabold text-xs">02</span>
              <div class="space-y-0.5">
                <h6 class="font-extrabold text-xs text-slate-800">출처 불명 앱/링크 설치 금지</h6>
                <p class="text-[10px] text-slate-500 font-semibold leading-relaxed">
                  문자로 온 검수 링크 클릭 및 원격제어 앱(.apk) 설치는 절대 삼가야 합니다.
                </p>
              </div>
            </div>

            <!-- 수칙 3 -->
            <div class="flex gap-2.5 items-start pt-2.5 border-t border-slate-100/60">
              <span class="text-blue-600 font-extrabold text-xs">03</span>
              <div class="space-y-0.5">
                <h6 class="font-extrabold text-xs text-slate-800">공식 대표 번호 더블 체크</h6>
                <p class="text-[10px] text-slate-500 font-semibold leading-relaxed">
                  전화를 끊은 뒤 포털 사이트에 검색한 해당 기관의 대표 전화로 직접 문의하십시오.
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- 훈련 총평 및 제언 -->
        <div class="space-y-1.5 text-left">
          <div class="font-extrabold text-slate-500 text-[10px] pl-1">훈련 총평</div>
          <div class="bg-white border border-slate-100 rounded-xl p-3.5 text-slate-650 leading-relaxed font-semibold text-[10px]">
            <p>{{ activeAnalysis.feedback }}</p>
          </div>
        </div>

        <!-- 긴급 연락처 다이얼러 -->
        <div class="space-y-1.5 text-left">
          <div class="font-extrabold text-slate-500 text-[10px] pl-1">민원 및 피해 신고처</div>
          <div class="bg-white border border-slate-100 rounded-xl p-3.5 space-y-2">
            
            <div class="flex justify-between items-center text-slate-500 font-semibold text-[10px]">
              <span>피해 및 송금 신고 (경찰청)</span>
              <a href="tel:112" class="text-blue-600 font-extrabold hover:underline">112</a>
            </div>
            
            <div class="flex justify-between items-center text-slate-500 font-semibold text-[10px] pt-2 border-t border-slate-100/60">
              <span>피해 상담 및 지급정지 (금감원)</span>
              <a href="tel:1332" class="text-blue-600 font-extrabold hover:underline">1332</a>
            </div>
            
            <div class="flex justify-between items-center text-slate-500 font-semibold text-[10px] pt-2 border-t border-slate-100/60">
              <span>스팸 및 해킹 제보 (KISA)</span>
              <a href="tel:118" class="text-blue-600 font-extrabold hover:underline">118</a>
            </div>

          </div>
        </div>
      </div>

      <!-- Privacy/Non-collection Notice -->
      <p class="text-[10px] text-slate-400 text-center leading-relaxed mt-4">
        ※ 본 훈련은 통화 음성 데이터 및 개인정보를 수집하거나 서버로 전송하지 않으며, 시나리오 진행도와 전화 끊기 타이밍만을 활용하여 기재된 안심 분석 리포트입니다.
      </p>

    </div>

    <!-- Confirm Button -->
    <button 
      @click="emit('close')"
      class="w-full bg-blue-600 hover:bg-blue-700 active:scale-[0.99] transition-all text-white font-bold text-xs py-3.5 px-4 rounded-xl shadow-[0_4px_20px_rgba(37,99,235,0.15)] mt-2 focus:outline-none"
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
