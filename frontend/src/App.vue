<template>
  <div class="app" @touchstart="handleTouchStart" @touchmove="handleTouchMove" @touchend="handleTouchEnd">
    <router-view />
    <ActivityDrawer ref="activityDrawerRef" />
    
    <!-- 全局任务管理器 -->
    <van-popup
      v-model:show="taskManagerStore.visible"
      position="bottom"
      round
      :style="{ height: '80%' }"
      class="task-manager-popup"
      closeable
      close-icon-position="top-right"
    >
      <TaskManager
        :visible="taskManagerStore.visible"
        @update:show="taskManagerStore.hide"
        @update:count="taskManagerStore.updateCount"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ActivityDrawer, TaskManager } from '@/components'
import { useTaskManagerStore } from './stores/taskManager'

const activityDrawerRef = ref()
const taskManagerStore = useTaskManagerStore()

// 手势相关变量
let touchStartX = 0
let touchStartY = 0
const SWIPE_THRESHOLD = 100  // 滑动阈值
const ANGLE_THRESHOLD = 30   // 角度阈值

// 处理触摸开始
const handleTouchStart = (event: TouchEvent) => {
  touchStartX = event.touches[0].clientX
  touchStartY = event.touches[0].clientY
}

// 处理触摸移动
const handleTouchMove = (event: TouchEvent) => {
  if (touchStartX === 0) return

  const touchEndX = event.touches[0].clientX
  const touchEndY = event.touches[0].clientY
  
  // 计算水平和垂直移动距离
  const deltaX = touchEndX - touchStartX
  const deltaY = touchEndY - touchStartY
  
  // 计算滑动角度
  const angle = Math.abs(Math.atan2(deltaY, deltaX) * 180 / Math.PI)
  
  // 如果是从左向右滑动，且滑动距离超过阈值，且角度在阈值范围内
  if (deltaX > SWIPE_THRESHOLD && angle < ANGLE_THRESHOLD) {
    activityDrawerRef.value?.show()
    touchStartX = 0  // 重置触摸起始点，防止重复触发
  }
}

// 处理触摸结束
const handleTouchEnd = () => {
  touchStartX = 0
  touchStartY = 0
}
</script>

<style>
.app {
  width: 100%;
  min-height: 100vh;
  background-color: var(--van-background);
}
</style>