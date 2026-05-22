<!-- 5-layer architecture: Component (Page) Layer for Profiling -->

<script setup lang="ts">
import { onMounted } from 'vue'
import { useProfilingStore } from '../state/profilingStore'
import { useTrainingStore } from '../state/trainingStore'
import { profilingService } from '../service/profilingService'
import { trainingService } from '../service/trainingService'

const store = useProfilingStore()
const trainingStore = useTrainingStore()

onMounted(async () => {
  if (store.questions.length === 0) {
    await profilingService.loadQuestions()
  }
})

const selectOption = (questionId: string, optionValue: string) => {
  profilingService.submitAnswer(questionId, optionValue)
}

const analyze = async () => {
  await profilingService.analyzeRiskProfile()
}

const reset = () => {
  profilingService.reset()
}

const startTailoredTraining = async () => {
  if (trainingStore.scenarios.length === 0) {
    await trainingService.loadScenarios()
  }

  let scenarioId = 'voice_prosecutor'
  if (store.result) {
    const textToMatch = (store.result.riskType || '') + ' ' + (store.result.vulnerabilities || []).join(' ')
    if (textToMatch.includes('대출') || textToMatch.includes('금융기관')) {
      scenarioId = 'voice_loan'
    }
  }

  const scenario = trainingStore.scenarios.find(s => s.id === scenarioId)
  if (scenario) {
    trainingService.startSimulation(scenario)
  } else {
    console.error('Tailored scenario not found in store:', scenarioId)
  }
}
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="text-center py-2">
      <h2 class="text-xl font-bold text-slate-850">개인 맞춤형 위험 진단</h2>
      <p class="text-sm text-slate-500 mt-1">간단한 문항을 통해 본인의 피싱 위협 취약성을 평가합니다.</p>
    </div>

    <!-- Active Survey Questions -->
    <div v-if="!store.result" class="space-y-6">
      <div 
        v-for="q in store.questions" 
        :key="q.id" 
        class="bg-white border border-slate-200/80 rounded-2xl p-5 space-y-4 shadow-[0_4px_20px_-4px_rgba(0,0,0,0.02)]"
      >
        <p class="text-sm font-bold text-slate-700">
          {{ q.text }}
        </p>
        
        <div class="grid grid-cols-1 gap-2.5">
          <button 
            v-for="opt in q.options"
            :key="opt.value"
            @click="selectOption(q.id, opt.value)"
            :class="[
              'w-full text-left text-sm py-3 px-4 rounded-xl border transition-all duration-200 active:scale-[0.99]',
              store.answers[q.id] === opt.value
                ? 'bg-blue-50/80 border-blue-600 text-blue-600 font-bold shadow-sm shadow-blue-600/5'
                : 'bg-slate-50/50 border-slate-200 text-slate-600 hover:border-slate-350 hover:bg-slate-50'
            ]"
          >
            {{ opt.label }}
          </button>
        </div>
      </div>

      <!-- Submit Action -->
      <button 
        @click="analyze"
        :disabled="Object.keys(store.answers).length < store.questions.length || store.isLoading"
        class="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-slate-100 disabled:text-slate-400 disabled:shadow-none disabled:cursor-not-allowed text-white font-bold py-3.5 px-4 rounded-xl shadow-[0_4px_12px_rgba(37,99,235,0.2)] transition-all active:scale-[0.99] flex items-center justify-center gap-2 text-sm"
      >
        <span v-if="store.isLoading" class="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full"></span>
        결과 분석하기
      </button>
    </div>

    <!-- Results Display -->
    <div v-else class="bg-white border border-slate-200/85 rounded-2xl p-5 space-y-5 shadow-lg shadow-slate-100/50">
      <div class="text-center pb-3 border-b border-slate-100">
        <span 
          :class="[
            'text-xs font-extrabold px-3 py-1 rounded-full uppercase tracking-wider border',
            store.result.riskLevel === 'HIGH' ? 'bg-rose-50 text-rose-600 border-rose-100' :
            store.result.riskLevel === 'MEDIUM' ? 'bg-amber-50 text-amber-600 border-amber-100' : 'bg-emerald-50 text-emerald-600 border-emerald-100'
          ]"
        >
          위험등급: {{ store.result.riskLevel }}
        </span>
        <h3 class="text-lg font-bold text-slate-800 mt-3">{{ store.result.riskType }}</h3>
      </div>

      <div class="space-y-4">
        <div>
          <h4 class="text-xs font-bold text-slate-400 uppercase tracking-wider">분석 내용</h4>
          <p class="text-sm text-slate-600 mt-1 leading-relaxed">{{ store.result.description }}</p>
        </div>

        <div>
          <h4 class="text-xs font-bold text-slate-400 uppercase tracking-wider">본인에게 취약한 범죄 수법</h4>
          <ul class="mt-2 space-y-2">
            <li 
              v-for="(vuln, idx) in store.result.vulnerabilities" 
              :key="idx"
              class="text-sm text-slate-650 flex items-start gap-2 bg-slate-50 p-2.5 rounded-xl border border-slate-100"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 fill-none stroke-rose-500 stroke-2 flex-shrink-0 mt-0.5" viewBox="0 0 24 24">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                <line x1="12" y1="9" x2="12" y2="13"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
              {{ vuln }}
            </li>
          </ul>
        </div>
      </div>

      <div class="pt-2 space-y-2.5">
        <button 
          @click="startTailoredTraining"
          class="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3.5 px-4 rounded-xl transition-all active:scale-[0.99] text-sm shadow-md shadow-blue-600/10 flex items-center justify-center gap-2"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
          </svg>
          나에게 맞춘 모의 훈련 시작하기
        </button>

        <button 
          @click="reset"
          class="w-full bg-slate-100 hover:bg-slate-200 text-slate-600 font-bold py-3 px-4 rounded-xl transition-all active:scale-[0.99] text-sm border border-slate-200/40"
        >
          다시 진단하기
        </button>
      </div>
    </div>
  </div>
</template>
