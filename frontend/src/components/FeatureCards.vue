<template>
  <div class="feature-cards">
    <van-grid :column-num="columnNum" :gutter="16" :border="false">
      <van-grid-item
          class="feature-item"
          icon="folder-o"
          text="文件管理"
          @click="toFileList"
      >
        <template #icon>
          <div class="feature-icon-wrapper blue">
            <van-icon name="folder-o"/>
          </div>
        </template>
      </van-grid-item>
      <van-grid-item
          class="feature-item"
          icon="orders-o"
          text="任务管理"
          @click="taskManagerStore.show()"
      >
        <template #icon>
          <div class="feature-icon-wrapper orange">
            <van-icon name="orders-o"/>
            <van-badge
                :content="runningTasksCount || ''"
                v-if="runningTasksCount > 0"
                class="feature-badge"
            />
          </div>
        </template>
      </van-grid-item>

      <van-grid-item
          class="feature-item"
          icon="label-o"
          text="标签"
          to="/tag-browser"
          @click="emit('itemClick')"
      >
        <template #icon>
          <div class="feature-icon-wrapper green">
            <van-icon name="label-o"/>
          </div>
        </template>
      </van-grid-item>

      <van-grid-item
          class="feature-item"
          icon="setting-o"
          text="系统设置"
          to="/setting"
          @click="emit('itemClick')"
      >
        <template #icon>
          <div class="feature-icon-wrapper purple">
            <van-icon name="setting-o"/>
          </div>
        </template>
      </van-grid-item>
    </van-grid>
  </div>
</template>

<script setup lang="ts">
import {useTaskManagerStore} from '@/stores/taskManager'
import {taskService} from '@/api/taskService'
import {onMounted, ref} from 'vue'
import router from "@/router";

const props = defineProps<{
  columnNum?: number
}>()

const emit = defineEmits<{
  (e: 'itemClick'): void
}>()

const taskManagerStore = useTaskManagerStore()
const runningTasksCount = ref(0)

// 获取运行中的任务数量
const fetchRunningTasks = async () => {
  try {
    const tasks = await taskService.getRunningTasks()
    runningTasksCount.value = tasks.length
  } catch (error) {
    console.error('获取运行中任务失败:', error)
  }
}

const toFileList = () => {
  router.push({
    name: "FileList",
    query: {
      fromNav: 'true'
    }
  })
  emit('itemClick')
}

onMounted(() => {
  fetchRunningTasks()
})

// 设置默认值
const columnNum = props.columnNum || 3
</script>

<style scoped>
.feature-cards {
  margin: 8px 0;
}

.feature-item {
  padding: 8px;
}

.feature-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  margin-bottom: 8px;
}

.feature-icon-wrapper :deep(.van-icon) {
  font-size: 24px;
  color: white;
}

.blue {
  background: linear-gradient(135deg, #1989fa, #0960bd);
}

.green {
  background: linear-gradient(135deg, #07c160, #06ad56);
}

.orange {
  background: linear-gradient(135deg, #ff976a, #ff6b3c);
}

.feature-badge {
  position: absolute;
  top: -8px;
  right: -8px;
}

/* 任务管理器容器样式 */
.task-manager-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: var(--van-background);
  z-index: 1;
  display: flex;
  flex-direction: column;
}

.task-manager-header {
  flex-shrink: 0;
}

.task-manager-content {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* 动画效果 */
.task-manager-container {
  animation: slide-up 0.3s ease-out;
}

@keyframes slide-up {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

/* 桌面端优化 */
@media (min-width: 768px) {
  .feature-icon-wrapper {
    width: 56px;
    height: 56px;
  }

  .feature-icon-wrapper :deep(.van-icon) {
    font-size: 28px;
  }
}

/* 添加紫色渐变样式 */
.purple {
  background: linear-gradient(135deg, #9c27b0, #7b1fa2);
}
</style> 