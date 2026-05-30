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

const formatDuration = (sec: number) => {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const compactExplanationMap: Record<string, string> = {
  SAFE: '피싱 의심 징후를 조기에 감지하고 통화를 종료하였습니다.',
  WARNING: '통화는 중단했으나, 의심 멘트 노출 시간이 다소 지연되었습니다.',
  CRITICAL: '이체 및 정보 강요 단계까지 통화가 유지되어 피싱 위험에 극도로 노출되었습니다.'
}

const getEmergencyContactForStep = (stepName: string, index: number) => {
  const name = stepName.toLowerCase()
  if (name.includes('이체') || name.includes('송금') || name.includes('계좌') || name.includes('금감원') || index === 2) {
    return {
      agency: '금융감독원',
      number: '1332',
      action: '피해 상담 및 계좌 지급정지',
      requiredDocs: ['이체 거래 내역서', '피해구제 신청서']
    }
  } else if (name.includes('설치') || name.includes('악성') || name.includes('스팸') || name.includes('도용')) {
    return {
      agency: 'KISA',
      number: '118',
      action: '해킹/악성 앱 신고 및 스팸 제보',
      requiredDocs: ['스팸 문자 캡처본', '악성 APK 파일']
    }
  } else {
    return {
      agency: '경찰청',
      number: '112',
      action: '피해 신고 및 사칭 조사 문의',
      requiredDocs: ['신분증 사본', '통화 녹음 파일', '사건 증거 자료']
    }
  }
}
</script>

<template>
  <div class="flex-1 flex flex-col justify-between py-6 px-5 overflow-hidden animate-fade-in text-slate-800 bg-white h-full font-sans">
    
    <!-- 상단 등급 및 멘트 영역 -->
    <div class="text-left mb-3 mt-1 px-1 pb-3">
      <div v-if="activeAnalysis.vulnerabilityStatus === 'SAFE'" class="space-y-1">
        <div class="flex items-center gap-2">
          <h2 class="text-[22px] font-black tracking-tight text-slate-900">피싱 대처 결과</h2>
        </div>
        <p class="text-xs text-slate-400 font-bold mt-1">즉각적인 통화 종료로 피해를 성공적으로 차단했습니다.</p>
      </div>
      <div v-else-if="activeAnalysis.vulnerabilityStatus === 'WARNING'" class="space-y-1">
        <div class="flex items-center gap-2">
          <h2 class="text-[22px] font-black tracking-tight text-slate-900">피싱 대처 결과</h2>
        </div>
        <p class="text-xs text-slate-400 font-bold mt-1">대화 지속 시간이 다소 길어 주의가 필요합니다.</p>
      </div>
      <div v-else class="space-y-1">
        <div class="flex items-center gap-2">
          <h2 class="text-[22px] font-black tracking-tight text-slate-900">피싱 대처 결과</h2>
        </div>
        <p class="text-xs text-slate-400 font-bold mt-1">이체 및 개인정보 유도 단계까지 노출되었습니다.</p>
      </div>
    </div>

    <!-- 최상단 전환 탭 메뉴 (보라파란색 테마 매칭) -->
    <div class="flex items-center justify-start gap-6 pb-2 px-1 border-b border-slate-100">
      <button 
        @click="activeTab = 'analysis'"
        :class="[
          'text-[14.5px] font-bold transition-all pb-2 px-1 relative focus:outline-none tracking-tight',
          activeTab === 'analysis' 
            ? 'text-[#4a5fec] font-extrabold after:absolute after:bottom-0 after:left-0 after:right-0 after:h-[3px] after:bg-[#4a5fec] after:rounded-full' 
            : 'text-slate-400 hover:text-slate-700'
        ]"
      >
        요약
      </button>
      <button 
        @click="activeTab = 'guide'"
        :class="[
          'text-[14.5px] font-bold transition-all pb-2 px-1 relative focus:outline-none tracking-tight',
          activeTab === 'guide' 
            ? 'text-[#4a5fec] font-extrabold after:absolute after:bottom-0 after:left-0 after:right-0 after:h-[3px] after:bg-[#4a5fec] after:rounded-full' 
            : 'text-slate-400 hover:text-slate-700'
        ]"
      >
        상세 진단
      </button>
    </div>

    <!-- 내용 스크롤 영역 -->
    <div class="flex-1 overflow-y-auto overflow-x-hidden pr-1 scroll-container mt-4 px-1">
      
      <!-- TAB 1: 요약 -->
      <div v-if="activeTab === 'analysis'" class="space-y-4 animate-fade-in text-left pb-4">
        
        <!-- CARD: 취약 진단 등급 -->
        <div
          :class="[
            'rounded-2xl p-4 text-center space-y-1 border',
            activeAnalysis.vulnerabilityStatus === 'SAFE'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-700'
              : activeAnalysis.vulnerabilityStatus === 'WARNING'
              ? 'bg-amber-50 border-amber-200 text-amber-700'
              : 'bg-rose-50 border-rose-200 text-rose-700'
          ]"
        >
          <p class="text-[10px] font-black uppercase tracking-wider">취약 진단 등급</p>
          <p class="text-[15px] font-extrabold mt-1">
            {{
              activeAnalysis.vulnerabilityStatus === 'SAFE' ? '안전 (즉각 회피)' :
              activeAnalysis.vulnerabilityStatus === 'WARNING' ? '주의 (경고 노출)' :
              '위험 (금전 피해 고위험)'
            }}
          </p>
          <p class="text-[11px] font-semibold opacity-80 mt-0.5">
            {{ activeAnalysis.hangUpStepName }}에서 통화 종료
          </p>
        </div>

        <!-- CARD: 잘 대처했어요 / 주의가 필요해요 대비 목록 -->
        <div class="bg-slate-50/60 border border-slate-100/80 rounded-2xl p-4.5 space-y-4">
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

        <!-- CARD: 피드백 및 대처 조언 -->
        <div class="bg-slate-50/60 border border-slate-100/80 rounded-2xl p-4 space-y-3">
          <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">대처 조언</p>
          <p class="text-[12px] text-slate-600 leading-relaxed font-bold">{{ activeAnalysis.feedback }}</p>

          <!-- 상태별 단계 대처법 -->
          <div class="border-t border-slate-200/60 pt-3 space-y-2">

            <!-- SAFE: 잘 대처한 경우 -->
            <template v-if="activeAnalysis.vulnerabilityStatus === 'SAFE'">
              <p class="text-[10px] font-extrabold text-[#4a5fec] uppercase tracking-wider">이렇게 대응하세요</p>
              <div class="space-y-1.5">
                <div class="flex items-start gap-2">
                  <span class="text-slate-400 text-[11px] leading-snug flex-shrink-0">•</span>
                  <p class="text-[11px] text-slate-700 font-bold leading-snug">모르는 번호로 온 공공기관 사칭 전화는 무조건 즉시 끊으세요. 진짜 기관은 전화로 계좌 이체나 개인정보를 요구하지 않습니다.</p>
                </div>
                <div class="flex items-start gap-2">
                  <span class="text-slate-400 text-[11px] leading-snug flex-shrink-0">•</span>
                  <p class="text-[11px] text-slate-700 font-bold leading-snug">의심 번호는 스팸 차단 앱(후후·T전화)에 즉시 신고해 동일 피해를 차단하세요.</p>
                </div>
                <div class="flex items-start gap-2">
                  <span class="text-slate-400 text-[11px] leading-snug flex-shrink-0">•</span>
                  <p class="text-[11px] text-slate-700 font-bold leading-snug">가족·지인에게 동일 수법을 공유해 주변 피해를 예방하세요.</p>
                </div>
              </div>
            </template>

            <!-- WARNING: 주의가 필요한 경우 -->
            <template v-else-if="activeAnalysis.vulnerabilityStatus === 'WARNING'">
              <p class="text-[10px] font-extrabold text-[#4a5fec] uppercase tracking-wider">이렇게 대응하세요</p>
              <div class="space-y-1.5">
                <div class="flex items-start gap-2">
                  <span class="text-slate-400 text-[11px] leading-snug flex-shrink-0">•</span>
                  <p class="text-[11px] text-slate-700 font-bold leading-snug">통화 중 개인정보(이름·주민번호·계좌번호)를 말했다면 즉시 해당 은행에 전화해 계좌 지급정지를 요청하세요.</p>
                </div>
                <div class="flex items-start gap-2">
                  <span class="text-slate-400 text-[11px] leading-snug flex-shrink-0">•</span>
                  <p class="text-[11px] text-slate-700 font-bold leading-snug">앞으로는 통화 시작 후 10초 이내에 공공기관 사칭 여부를 판단하고 바로 끊는 연습이 필요합니다.</p>
                </div>
                <div class="flex items-start gap-2">
                  <span class="text-slate-400 text-[11px] leading-snug flex-shrink-0">•</span>
                  <p class="text-[11px] text-slate-700 font-bold leading-snug">"금융감독원·검찰청 직원"이라고 하면 100% 사기입니다. 직함이나 사건번호를 말해도 믿지 마세요.</p>
                </div>
              </div>
            </template>

            <!-- CRITICAL: 위험한 경우 -->
            <template v-else>
              <p class="text-[10px] font-extrabold text-rose-600 uppercase tracking-wider">이렇게 대응하세요</p>
              <div class="space-y-1.5">
                <div class="flex items-start gap-2">
                  <span class="text-slate-400 text-[11px] leading-snug flex-shrink-0">•</span>
                  <p class="text-[11px] text-slate-700 font-bold leading-snug">모든 거래 은행에 즉시 전화해 계좌 지급정지·OTP 차단을 요청하고, 금감원(1332)에 피해 사실을 신고하세요.</p>
                </div>
                <div class="flex items-start gap-2">
                  <span class="text-slate-400 text-[11px] leading-snug flex-shrink-0">•</span>
                  <p class="text-[11px] text-slate-700 font-bold leading-snug">주민번호·공인인증서 등 개인정보가 노출됐다면 명의도용방지 서비스(NICE·KCB)에서 즉시 신용 차단 신청을 하세요.</p>
                </div>
                <div class="flex items-start gap-2">
                  <span class="text-slate-400 text-[11px] leading-snug flex-shrink-0">•</span>
                  <p class="text-[11px] text-slate-700 font-bold leading-snug">앱 설치나 원격 제어 앱을 설치했다면 즉시 삭제하고, 스마트폰을 공장 초기화하는 것을 권장합니다.</p>
                </div>
                <div class="flex items-start gap-2">
                  <span class="text-slate-400 text-[11px] leading-snug flex-shrink-0">•</span>
                  <p class="text-[11px] text-slate-700 font-bold leading-snug">경찰(112)에 신고 후 통화 녹음·문자 캡처 등 증거를 보관하세요. 빠를수록 피해 회복 가능성이 높아집니다.</p>
                </div>
              </div>
            </template>
          </div>

          <!-- 긴급 연락처 -->
          <!-- <div class="border-t border-slate-200/60 pt-3 space-y-2">
            <p class="text-[10px] font-extrabold text-slate-400 uppercase tracking-wider mb-1">긴급 연락처</p>
            <div class="flex justify-between items-center bg-white rounded-xl px-3 py-2 border border-slate-100 shadow-3xs">
              <span class="text-[11px] text-slate-700 font-bold">경찰청 <span class="text-slate-400 font-semibold">· 피해 신고 접수</span></span>
              <span class="text-[13px] text-[#4a5fec] font-extrabold">☎ 112</span>
            </div>
            <div class="flex justify-between items-center bg-white rounded-xl px-3 py-2 border border-slate-100 shadow-3xs">
              <span class="text-[11px] text-slate-700 font-bold">금융감독원 <span class="text-slate-400 font-semibold">· 피해 상담 및 환급</span></span>
              <span class="text-[13px] text-[#4a5fec] font-extrabold">☎ 1332</span>
            </div>
            <div class="flex justify-between items-center bg-white rounded-xl px-3 py-2 border border-slate-100 shadow-3xs">
              <span class="text-[11px] text-slate-700 font-bold">KISA <span class="text-slate-400 font-semibold">· 스팸·번호도용 제보</span></span>
              <span class="text-[13px] text-[#4a5fec] font-extrabold">☎ 118</span>
            </div>
          </div> -->
        </div>
        <!-- CARD: 피싱 수법 단계별 타임라인 -->
        <!-- <div class="bg-slate-50/60 border border-slate-100/80 rounded-2xl p-4 space-y-3">
          <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">보이스피싱 시나리오 분석</p>
          <div class="space-y-2">
            <div
              v-for="(tech, tIdx) in activeAnalysis.techniques"
              :key="tIdx"
              :class="[
                'p-3 rounded-xl border transition-all text-left flex gap-3 shadow-3xs',
                tIdx === activeAnalysis.hangUpStepIndex
                  ? 'bg-white border-blue-200 ring-1 ring-blue-100 text-slate-800'
                  : 'bg-white/70 border-slate-150/40 opacity-55 text-slate-500'
              ]"
            >
              <div
                :class="[
                  'w-6 h-6 rounded-full flex items-center justify-center text-[11px] font-black flex-shrink-0 mt-0.5',
                  tIdx === activeAnalysis.hangUpStepIndex ? 'bg-[#4a5fec] text-white' : 'bg-slate-200 text-slate-500'
                ]"
              >
                {{ tIdx + 1 }}
              </div>
              <div class="space-y-0.5">
                <div class="flex items-center gap-1.5">
                  <h6 class="font-bold text-xs" :class="tIdx === activeAnalysis.hangUpStepIndex ? 'text-slate-800' : 'text-slate-500'">
                    {{ tech.name }}
                  </h6>
                  <span v-if="tIdx === activeAnalysis.hangUpStepIndex" class="text-[9px] bg-[#4a5fec] text-white font-extrabold px-1.5 py-0.5 rounded-md">
                    종료 지점
                  </span>
                </div>
                <p class="text-[11px] leading-normal" :class="tIdx === activeAnalysis.hangUpStepIndex ? 'text-slate-600' : 'text-slate-400'">
                  {{ tech.desc }}
                </p>
              </div>
            </div>
          </div>
        </div> -->
      </div>

      <!-- TAB 2: 상세 진단 내역 -->
      <div v-else-if="activeTab === 'guide'" class="space-y-4 animate-fade-in text-left pb-4">

        <!-- CARD: 시나리오 정보 & 타임라인 분석 통합 카드 -->
        <div class="bg-slate-50/60 border border-slate-100/80 rounded-2xl p-4 space-y-3">
          
          <!-- 시나리오 기본 헤더 -->
          <div class="space-y-1">
            <div class="flex justify-between items-center">
              <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">훈련 시나리오</p>
              <span class="text-[10px] font-bold text-slate-400">{{ activeAnalysis.date }}</span>
            </div>
            <div class="flex justify-between items-center gap-4">
              <h4 class="text-[13px] font-extrabold text-slate-800 leading-snug flex-1">{{ activeAnalysis.scenarioTitle?.replace('\n', ' ') }}</h4>
              <div class="flex items-center gap-1 text-[11px] text-[#4a5fec] font-extrabold flex-shrink-0">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12 6 12 12 16 12"/>
                </svg>
                <span>{{ formatDuration(activeAnalysis.duration) }}</span>
              </div>
            </div>
          </div>

          <!-- 타임라인 별 취약점 분석 및 맞춤 행동 대응 -->
          <div class="border-t border-slate-200/60 pt-3 space-y-3">
            <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">타임라인</p>
            
            <div class="space-y-3">
              <div 
                v-for="(tech, tIdx) in activeAnalysis.techniques" 
                :key="tIdx"
                :class="[
                  'p-3.5 rounded-xl border bg-white shadow-3xs transition-all text-left space-y-3',
                  tIdx === activeAnalysis.hangUpStepIndex 
                    ? 'border-blue-200 ring-1 ring-blue-100' 
                    : tIdx < activeAnalysis.hangUpStepIndex
                    ? 'border-slate-150 opacity-80'
                    : 'border-slate-150/40 opacity-50'
                ]"
              >
                <!-- 단계 타이틀 -->
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-2">
                    <span 
                      :class="[
                        'w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-black',
                        tIdx <= activeAnalysis.hangUpStepIndex ? 'bg-[#4a5fec] text-white' : 'bg-slate-200 text-slate-500'
                      ]"
                    >
                      {{ tIdx + 1 }}
                    </span>
                    <h6 class="font-extrabold text-xs text-slate-800">
                      {{ tech.name }}
                    </h6>
                  </div>
                  
                  <span 
                    v-if="tIdx === activeAnalysis.hangUpStepIndex" 
                    class="bg-blue-500 text-white text-[9px] font-extrabold px-1.5 py-0.5 rounded"
                  >
                    차단 지점
                  </span>
                  <span 
                    v-else-if="tIdx < activeAnalysis.hangUpStepIndex" 
                    class="text-[9px] text-rose-600 font-bold"
                  >
                    통과
                  </span>
                  <span 
                    v-else 
                    class="text-[9px] text-slate-450 font-bold"
                  >
                    미도달
                  </span>
                </div>

                <!-- 단계 설명 -->
                <p class="text-[11px] text-slate-500 leading-normal pl-7">
                  {{ tech.desc }}
                </p>

                <!-- 차단(종료) 지점일 경우 취약점 진단 분석 노출 -->
                <div 
                  v-if="tIdx === activeAnalysis.hangUpStepIndex" 
                  class="bg-slate-50 rounded-lg p-2.5 space-y-1.5 border border-slate-100 ml-7"
                >
                  <div>
                    <span class="text-[9px] font-black text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded">취약 진단</span>
                    <p class="text-[11px] text-slate-700 font-bold leading-normal mt-1">
                      {{ activeAnalysis.vulnerabilityExplanation }}
                    </p>
                  </div>
                </div>

                <!-- 단계별 위험에 비례한 전담 긴급 대응 기관 매핑 -->
                <div 
                  v-if="tIdx <= activeAnalysis.hangUpStepIndex" 
                  class="border-t border-slate-100 pt-2.5 ml-7 space-y-2 text-[11px]"
                >
                  <div class="flex items-center justify-between">
                    <div class="flex flex-col">
                      <span class="font-bold text-slate-750">
                        {{ getEmergencyContactForStep(tech.name, tIdx).agency }} 
                        <span class="text-[10px] text-slate-400 font-medium">
                          ({{ getEmergencyContactForStep(tech.name, tIdx).action }})
                        </span>
                      </span>
                    </div>
                    <span class="font-extrabold text-[#4a5fec] flex-shrink-0">
                      ☎ {{ getEmergencyContactForStep(tech.name, tIdx).number }}
                    </span>
                  </div>
                  <!-- 필요 제출 문서 표시 -->
                  <div class="flex flex-wrap items-center gap-1.5 pt-1 border-t border-dashed border-slate-100">
                    <span class="text-[9px] text-slate-400 font-bold">필요 서류:</span>
                    <span 
                      v-for="(doc, dIdx) in getEmergencyContactForStep(tech.name, tIdx).requiredDocs" 
                      :key="dIdx"
                      class="bg-slate-100 text-slate-600 text-[9px] font-semibold px-1.5 py-0.5 rounded"
                    >
                      {{ doc }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- Confirm Button -->
    <button 
      @click="emit('close')"
      class="w-full bg-[#4a5fec] hover:bg-[#394dcd] active:scale-[0.99] transition-all text-white font-extrabold text-sm py-4.5 px-4 rounded-2xl shadow-[0_8px_30px_rgba(74,95,236,0.18)] focus:outline-none tracking-tight mt-2"
    >
      리포트 닫기 및 훈련 종료
    </button>
  </div>
</template>

<style scoped>
/* Hide Scrollbar for Report Container */
.scroll-container {
  -ms-overflow-style: none; /* IE and Edge */
  scrollbar-width: none; /* Firefox */
}
.scroll-container::-webkit-scrollbar {
  display: none; /* Chrome, Safari and Opera */
}
</style>
