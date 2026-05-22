<!-- 5-layer architecture: Component (Page) Layer for Vulnerability Report -->

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useReportStore } from '../state/reportStore'
import { reportService } from '../service/reportService'

const store = useReportStore()
const selectedItem = ref<any>(null)

onMounted(async () => {
  if (!store.report) {
    await reportService.loadMonthlyReport()
  }
})

const showDetails = (item: any) => {
  selectedItem.value = item
}

const closeModal = () => {
  selectedItem.value = null
}

const formatDuration = (sec?: number) => {
  if (sec === undefined || sec === 0) return '00:00'
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}
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
            @click="showDetails(h)"
            class="bg-slate-50/50 border border-slate-200 rounded-xl p-3 flex items-center justify-between gap-3 text-2xs cursor-pointer hover:bg-slate-100 hover:border-slate-350 transition-all duration-200 active:scale-[0.99]"
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

    <!-- E. DETAIL MODAL OVERLAY -->
    <Transition name="fade">
      <div 
        v-if="selectedItem" 
        class="absolute inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-6 animate-fade-in text-white"
        @click.self="closeModal"
      >
        <div class="bg-slate-900 border border-white/10 w-full h-[580px] max-w-sm rounded-[32px] overflow-hidden flex flex-col shadow-2xl relative">
          <!-- Modal Header -->
          <div class="p-5 border-b border-white/10 flex justify-between items-center bg-slate-950/20">
            <div>
              <h3 class="text-xs font-black text-slate-400 uppercase tracking-widest">피싱 훈련 진단 보고서</h3>
              <p class="text-[10px] text-slate-500 mt-0.5 font-bold">{{ selectedItem.date }} 실시</p>
            </div>
            <button 
              @click="closeModal"
              class="w-7 h-7 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-slate-300 hover:bg-white/10 hover:text-white transition-all"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <!-- Modal Body (Scrollable) -->
          <div class="flex-1 overflow-y-auto p-5 space-y-4 text-left scroll-container">
            
            <!-- Scenario Information Card -->
            <div class="bg-white/5 border border-white/10 rounded-2xl p-4 space-y-2">
              <div class="flex justify-between items-center text-[9px] text-slate-400 font-bold">
                <span>훈련 유형: {{ selectedItem.scenarioType === 'VOICE' ? '보이스피싱' : selectedItem.scenarioType === 'SMS' ? '문자 스미싱' : '이메일 피싱' }}</span>
                <span class="text-blue-400 font-extrabold">{{ selectedItem.result === 'SUCCESS' ? '예방 성공' : '대응 실패' }}</span>
              </div>
              <h4 class="text-xs font-black text-white mt-1">{{ selectedItem.title }}</h4>
              
              <div v-if="selectedItem.scenarioType === 'VOICE'" class="flex items-center gap-1.5 text-[10px] text-slate-400 font-bold mt-1">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12 6 12 12 16 12"/>
                </svg>
                <span>통화 유지 시간: {{ formatDuration(selectedItem.duration) }}</span>
              </div>
            </div>

            <!-- Vulnerability status -->
            <div 
              :class="[
                'border rounded-2xl p-4 text-center space-y-1',
                selectedItem.vulnerabilityStatus === 'SAFE' ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' :
                selectedItem.vulnerabilityStatus === 'WARNING' ? 'bg-amber-500/10 border-amber-500/20 text-amber-400' :
                'bg-rose-500/10 border-rose-500/20 text-rose-400'
              ]"
            >
              <p class="text-[9px] font-black uppercase tracking-wider">취약 진단 등급</p>
              <p class="text-sm font-black mt-1">
                {{ 
                  selectedItem.vulnerabilityStatus === 'SAFE' ? '안전 (즉각 대처)' : 
                  selectedItem.vulnerabilityStatus === 'WARNING' ? '주의 (경고 노출)' : 
                  '위험 (피해 가능성 높음)' 
                }}
              </p>
              <p v-if="selectedItem.hangUpStepName" class="text-[10px] text-slate-350 font-semibold mt-1">
                {{ selectedItem.hangUpStepName }} 에서 종료
              </p>
              <p v-else class="text-[10px] text-slate-350 font-semibold mt-1">
                {{ selectedItem.scenarioType === 'SMS' ? '문자 웹링크 클릭 정황 분석' : '이메일 계정 탈취 양식 노출 분석' }}
              </p>
            </div>

            <!-- Vulnerability analysis explanation -->
            <div class="space-y-1.5">
              <h5 class="font-bold text-slate-300 text-xs">🔍 대처 취약점 분석</h5>
              <div class="bg-white/5 border border-white/5 rounded-2xl p-3.5 text-slate-350 leading-relaxed font-medium text-[11px]">
                {{ selectedItem.vulnerabilityExplanation || '피싱 공격 훈련 데이터가 정상적으로 수집되었습니다.' }}
              </div>
            </div>

            <!-- Prevention Guide & Support Center -->
            <div class="space-y-1.5">
              <h5 class="font-bold text-slate-300 text-xs">💡 안전 대응 가이드</h5>
              <div class="bg-white/5 border border-white/5 rounded-2xl p-3.5 text-slate-350 leading-relaxed font-medium text-[11px]">
                <p class="mb-2.5">{{ selectedItem.feedback || '모르는 발신자의 요구는 즉시 무시하는 것이 최선의 방법입니다.' }}</p>
                <div class="border-t border-white/10 pt-2.5 mt-2.5 space-y-2 text-[10px]">
                  <div class="flex justify-between items-center text-slate-400">
                    <span>피해 발생 신고 (경찰청)</span>
                    <a href="tel:112" class="text-blue-400 font-extrabold hover:underline">112 바로연결</a>
                  </div>
                  <div class="flex justify-between items-center text-slate-400">
                    <span>피해 의심 상담 (금융감독원)</span>
                    <a href="tel:1332" class="text-blue-400 font-extrabold hover:underline">1332 바로연결</a>
                  </div>
                  <div class="flex justify-between items-center text-slate-400">
                    <span>스팸/번호변작 제보 (인터넷진흥원)</span>
                    <a href="tel:118" class="text-blue-400 font-extrabold hover:underline">118 바로연결</a>
                  </div>
                </div>
              </div>
            </div>

            <!-- Techniques Timeline -->
            <div v-if="selectedItem.techniques && selectedItem.techniques.length > 0" class="space-y-2">
              <h5 class="font-bold text-slate-300 text-xs">📊 공격 기법 단계별 구성</h5>
              <div class="space-y-2">
                <div 
                  v-for="(tech, tIdx) in selectedItem.techniques" 
                  :key="tIdx"
                  :class="[
                    'p-3.5 rounded-2xl border transition-all text-left flex gap-3',
                    tIdx === selectedItem.hangUpStepIndex 
                      ? 'bg-blue-600/10 border-blue-500/30 ring-1 ring-blue-500/20' 
                      : 'bg-white/5 border-white/5 opacity-60'
                  ]"
                >
                  <div 
                    :class="[
                      'w-5.5 h-5.5 rounded-full flex items-center justify-center text-[10px] font-black flex-shrink-0 mt-0.5',
                      tIdx === selectedItem.hangUpStepIndex ? 'bg-blue-600 text-white' : 'bg-white/10 text-slate-450'
                    ]"
                  >
                    {{ Number(tIdx) + 1 }}
                  </div>
                  <div class="space-y-1">
                    <div class="flex items-center gap-1.5">
                      <h6 class="font-bold text-slate-200 text-xs">{{ tech.name }}</h6>
                      <span v-if="tIdx === selectedItem.hangUpStepIndex" class="text-[9px] bg-blue-500 text-white font-bold px-1.5 py-0.2 rounded">
                        종료 지점
                      </span>
                    </div>
                    <p class="text-[10px] text-slate-400 leading-normal">{{ tech.desc }}</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- Privacy Notice -->
            <p class="text-[9px] text-slate-500 text-center leading-relaxed mt-4">
              ※ 본 훈련은 통화 음성 데이터 및 개인정보를 수집하거나 서버로 전송하지 않으며, 시나리오 진행도와 대응 타이밍만을 분석한 안심 리포트입니다.
            </p>

          </div>

          <!-- Close button -->
          <div class="p-4 border-t border-white/10 bg-slate-950/20">
            <button 
              @click="closeModal"
              class="w-full bg-blue-600 hover:bg-blue-500 active:scale-[0.98] transition-all text-white font-bold text-xs py-3 px-4 rounded-xl shadow-[0_4px_20px_rgba(37,99,235,0.4)]"
            >
              확인 및 닫기
            </button>
          </div>
        </div>
      </div>
    </Transition>
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
  background: rgba(255, 255, 255, 0.15);
  border-radius: 4px;
}
.scroll-container::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.25);
}
</style>
