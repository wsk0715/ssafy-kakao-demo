<script setup lang="ts">
import { ref } from 'vue'
import MainLayout from './layout/MainLayout.vue'
import ProfilingPage from './component/ProfilingPage.vue'
import TrainingPage from './component/TrainingPage.vue'
import ResponsePage from './component/ResponsePage.vue'
import ReportPage from './component/ReportPage.vue'

const activeTab = ref<'profiling' | 'training' | 'response' | 'report'>('profiling')

const handleTabChange = (tab: 'profiling' | 'training' | 'response' | 'report') => {
  activeTab.value = tab
}
</script>

<template>
  <MainLayout :active-tab="activeTab" @tab-change="handleTabChange">
    <ProfilingPage v-if="activeTab === 'profiling'" />
    <TrainingPage v-else-if="activeTab === 'training'" />
    <ResponsePage v-else-if="activeTab === 'response'" />
    <ReportPage v-else-if="activeTab === 'report'" />
  </MainLayout>
</template>

