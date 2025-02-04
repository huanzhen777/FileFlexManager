import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useTaskManagerStore = defineStore('taskManager', () => {
  const visible = ref(false)
  const runningTasksCount = ref(0)

  const show = () => {
    visible.value = true
  }

  const hide = () => {
    visible.value = false
  }

  const updateCount = (count: number) => {
    runningTasksCount.value = count
  }

  return {
    visible,
    runningTasksCount,
    show,
    hide,
    updateCount
  }
}) 