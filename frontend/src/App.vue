<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import MainLayout from './layout/MainLayout.vue'
import ProfilingPage from './component/ProfilingPage.vue'
import ResponsePage from './component/ResponsePage.vue'
import ReportPage from './component/ReportPage.vue'
import { callConnectionService } from './service/callConnectionService'

const activeTab = ref<'profiling' | 'response' | 'report'>('profiling')

const handleTabChange = (tab: 'profiling' | 'response' | 'report') => {
  activeTab.value = tab
}

onMounted(() => {
  // Connect to SSE simulation trigger channel on app load
  callConnectionService.connect('demo_user')
})

onUnmounted(() => {
  // Clean up connection
  callConnectionService.disconnect()
})
</script>

<template>
  <MainLayout :active-tab="activeTab" @tab-change="handleTabChange">
    <ProfilingPage v-if="activeTab === 'profiling'" />
    <ResponsePage v-else-if="activeTab === 'response'" />
    <ReportPage v-else-if="activeTab === 'report'" />
  </MainLayout>
</template>

