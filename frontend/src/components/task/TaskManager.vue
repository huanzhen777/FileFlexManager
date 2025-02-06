<template>
  <div class="task-manager">
    <!-- 顶部导航栏 -->
    <van-nav-bar
        title="任务管理器"
        left-text="关闭"
        left-arrow
        @click-left="$emit('update:show', false)"
    />

    <!-- 任务过滤器 -->
    <div class="filter-bar">
      <van-tabs v-model:active="activeTab" shrink>
        <van-tab title="运行中" name="running"/>
        <van-tab title="定时任务" name="scheduled"/>
        <van-tab title="全部" name="all"/>
      </van-tabs>
    </div>

    <!-- 添加定时任务按钮 -->
    <van-button
        v-if="activeTab === 'scheduled'"
        type="primary"
        icon="plus"
        class="add-scheduled-task"
        @click="showCreateScheduledTask"
    >
      添加定时任务
    </van-button>

    <!-- 任务列表 -->
    <div class="task-list">
      <van-cell-group>
        <van-cell
            v-for="task in filteredTasks"
            :key="task.id"
            class="task-cell"
            @click="showTaskDetail(task)"
        >
          <!-- 标题区域 -->
          <template #title>
            <div class="task-header">
              <span class="task-desc">{{ task.desc }}</span>
              <van-tag :type="getTaskStatusType(task.status)" size="medium">
                {{ getStatusText(task.status) }}
              </van-tag>
            </div>
          </template>

          <!-- 内容区域 -->
          <template #label>
            <div class="task-info">
              <!-- 基本信息行 -->
              <div class="task-info-row">
                <div class="task-basic-info">
                  <van-tag type="primary" plain>{{ task.typeDesc }}</van-tag>
                  <span class="update-time">
                    <van-icon name="clock-o"/>
                    {{ task.status === 'COMPLETED' ? '完成于: ' : '更新于: ' }}
                    {{ formatTime(task.status === 'COMPLETED' ? task.endTime : task.updateTime) }}
                  </span>
                </div>
              </div>

              <!-- 进度条（运行中任务显示） -->
              <div v-if="task.status === 'RUNNING'" class="progress-bar">
                <van-progress
                    :percentage="task.progress === -1 ? 50 : task.progress"
                    :show-pivot="false"
                    :color="getProgressColor(task)"
                    stroke-width="4"
                />
                <span class="progress-text">{{ task.progress === -1 ? '进行中' : `${task.progress}%` }}</span>
              </div>

              <!-- 定时任务特有信息 -->
              <div v-if="task.scheduled" class="scheduled-info">
                <div class="scheduled-info-row">
                  <van-icon name="clock-o"/>
                  <span>{{ task.cronExpression }}</span>
                  <span class="next-execute">
                    下次执行: {{ formatTime(task.nextExecuteTime) }}
                  </span>
                </div>
                <div class="scheduled-actions">
                  <van-switch
                      v-model="task.enabled"
                      size="small"
                      @change="toggleScheduledTask(task)"
                  />
                  <van-button
                      size="mini"
                      icon="delete"
                      type="danger"
                      plain
                      @click.stop="confirmDeleteTask(task)"
                  />
                </div>
              </div>
            </div>
          </template>
        </van-cell>
      </van-cell-group>
    </div>

    <!-- 任务详情弹窗 -->
    <van-dialog
        v-model:show="taskDetailVisible"
        :title="currentTask?.desc || '任务详情'"
        class="task-detail-dialog"
        :show-confirm-button="false"
        close-on-click-overlay
    >
      <div class="task-detail">
        <!-- 任务状态和进度 -->
        <div class="detail-header">
          <van-tag :type="getTaskStatusType(currentTask?.status || '')" size="medium">
            {{ getStatusText(currentTask?.status || '') }}
          </van-tag>
          <van-tag type="primary" plain>{{ currentTask?.typeDesc }}</van-tag>
        </div>

        <!-- 进度显示（仅运行中任务显示） -->
        <div v-if="currentTask?.status === 'RUNNING'" class="progress-section">
          <van-progress
              :percentage="currentTask?.progress === -1 ? 50 : (currentTask?.progress || 0)"
              :show-pivot="false"
              :color="getProgressColor(currentTask)"
              stroke-width="4"
          />
          <span class="progress-text">{{ currentTask?.progress === -1 ? '进行中' : `${currentTask?.progress}%` }}</span>
        </div>

        <!-- 任务信息列表 -->
        <div class="detail-info">
          <div class="info-item">
            <van-icon name="clock-o"/>
            <span class="label">开始时间</span>
            <span class="value">{{ formatTime(currentTask?.beginTime) }}</span>
          </div>
          <div class="info-item">
            <van-icon name="clock-o"/>
            <span class="label">结束时间</span>
            <span class="value">{{ formatTime(currentTask?.endTime) }}</span>
          </div>
          <div v-if="currentTask?.message" class="info-item message">
            <van-icon name="info-o"/>
            <span class="label">消息</span>
            <span class="value preserve-whitespace">{{ currentTask?.message }}</span>
          </div>
        </div>

        <!-- 定时任务信息 -->
        <div v-if="currentTask?.scheduled" class="scheduled-section">
          <div class="info-item">
            <van-icon name="clock-o"/>
            <span class="label">Cron</span>
            <span class="value">{{ currentTask?.cronExpression }}</span>
          </div>
          <div class="info-item">
            <van-icon name="clock-o"/>
            <span class="label">下次执行</span>
            <span class="value">{{ formatTime(currentTask?.nextExecuteTime) }}</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="detail-actions" v-if="currentTask?.status === 'RUNNING'">
          <van-button
              type="danger"
              size="small"
              icon="close"
              @click="cancelTask"
          >
            取消任务
          </van-button>
        </div>
      </div>
    </van-dialog>

    <!-- 创建定时任务弹窗 -->
    <van-dialog
        v-model:show="createScheduledVisible"
        title="创建定时任务"
        @confirm="submitScheduledTask"
        show-cancel-button
    >
      <van-form>
        <!-- 任务类型选择 -->
        <van-field
            :model-value="scheduledForm.desc || '请选择任务类型'"
            label="任务类型"
            readonly
            is-link
            @click="showTaskTypePopup"
        />

        <!-- 任务描述输入 -->
        <van-field
            v-model="scheduledForm.desc"
            label="任务描述"
            type="textarea"
            rows="2"
            placeholder="请输入任务描述"
            :rules="[{ required: true, message: '请输入任务描述' }]"
        />

        <!-- 任务参数表单 -->
        <task-param-form
            v-if="selectedTaskType"
            ref="paramFormRef"
            :params="selectedTaskType.paramConfigs"
        />

        <!-- Cron表达式输入 -->
        <van-field
            v-model="scheduledForm.cronExpression"
            label="Cron表达式"
            placeholder="输入Cron表达式"
        >
          <template #right-icon>
            <van-popover
                v-model:show="showCronHelp"
                placement="bottom-end"
                theme="dark"
            >
              <div class="cron-help">
                <p>常用Cron表达式：</p>
                <p>每分钟：0 * * * * *</p>
                <p>每小时：0 0 * * * *</p>
                <p>每天凌晨：0 0 0 * * *</p>
                <p>每周一凌晨：0 0 0 * * MON</p>
              </div>
              <template #reference>
                <van-icon name="question-o"/>
              </template>
            </van-popover>
          </template>
        </van-field>
      </van-form>
    </van-dialog>

    <!-- 任务类型选择弹窗 -->
    <van-popup
        v-model:show="taskTypeVisible"
        position="bottom"
        round
    >
      <van-picker
          :columns="taskTypeColumns"
          @confirm="onTaskTypeConfirm"
          @cancel="taskTypeVisible = false"
          show-toolbar
          title="选择任务类型"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import {ref, onMounted, onBeforeUnmount, computed, watch} from 'vue'
import {showToast, showDialog} from 'vant'
import {taskService} from '@/api/taskService.ts'
import TaskParamForm from '@/components/param/ParamForm.vue'
import {Task, TaskType} from "@/types/task.ts";

const props = defineProps<{
  visible?: boolean
}>()

const emit = defineEmits(['update:show', 'update:count'])

// 状态管理
const tasks = ref<Task[]>([])
const loading = ref(false)
const taskDetailVisible = ref(false)
const currentTask = ref<Task | null>(null)
const activeTab = ref<'running' | 'all' | 'scheduled'>('running')
let pollingTimer: number | null = null

// 定时任务相关状态
const createScheduledVisible = ref(false)
const scheduledForm = ref({
  type: '',
  desc: '',
  cronExpression: '',
  payload: {} as Record<string, any>
})
const scheduledTaskTypes = ref<TaskType[]>([])
const taskTypeVisible = ref(false)
const showCronHelp = ref(false)

// 过滤任务
const filteredTasks = computed(() => {
  if (activeTab.value === 'running') {
    return tasks.value.filter(task => task.status === 'RUNNING')
  }
  if (activeTab.value === 'scheduled') {
    return tasks.value.filter(task => task.scheduled)
  }
  return tasks.value
})

// 任务状态样式
const getTaskStatusType = (status: string) => {
  switch (status) {
    case 'RUNNING':
      return 'primary'
    case 'COMPLETED':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'CANCELLED':
      return 'warning'
    default:
      return 'default'
  }
}

// 进度条颜色
const getProgressColor = (task: Task | null) => {
  if (!task) return '#1989fa'
  switch (task.status) {
    case 'COMPLETED':
      return '#07c160'
    case 'FAILED':
      return '#ee0a24'
    case 'CANCELLED':
      return '#ff976a'
    default:
      return '#1989fa'
  }
}

// 格式化时间
const formatTime = (timestamp?: number) => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString()
}

// 通知父组件更新任务数量
const updateParentTaskCount = () => {
  const runningCount = tasks.value.filter(task => task.status === 'RUNNING').length
  if (runningCount !== lastRunningCount) {
    emit('update:count', runningCount)
    lastRunningCount = runningCount
  }
}

// 记录上一次的运行中任务数量
let lastRunningCount = 0

// 修改 loadTasks 函数
const loadTasks = async () => {
  try {
    let response = await taskService.getAllTasks({
      page: 1,
      size: 1000,  // 一次性加载1000条数据
      includeCompleted: activeTab.value === 'all'
    });

    tasks.value = response.records;
    updateParentTaskCount();

    const hasRunningTasks = tasks.value.some(task => task.status === 'RUNNING');
    if (hasRunningTasks && props.visible) {
      startPolling();
    }
  } catch (error: any) {
    showToast(error.message || '加载任务失败');
  }
}

// 修改标签页切换监听
watch(activeTab, () => {
  tasks.value = [];
  loadTasks();
})

// 显示任务详情
const showTaskDetail = (task: Task) => {
  currentTask.value = task
  taskDetailVisible.value = true
}

// 取消任务
const cancelTask = async () => {
  if (!currentTask.value) return

  try {
    await taskService.cancelTask(currentTask.value.id)
    showToast('任务已取消')
    taskDetailVisible.value = false
    loadTasks()
  } catch (error: any) {
    showToast(error.message || '取消任务失败')
  }
}

// 轮询任务状态
const startPolling = () => {
  // 果已经有定时器，先清除
  stopPolling()

  // 立即开始第一次轮询
  pollTasks()

  pollingTimer = window.setInterval(async () => {
    await pollTasks()
  }, 500);
}

const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

// 轮询任务
const pollTasks = async () => {
  try {
    const runningTasks = await taskService.getRunningTasks()
    tasks.value = tasks.value.map(task => {
      const updatedTask = runningTasks.find((t: Task) => t.id === task.id)
      if (updatedTask) {
        return updatedTask
      }
      if (task.status === 'RUNNING') {
        return {...task, status: 'COMPLETED'}
      }
      return task
    })
    updateParentTaskCount()

    // 如果没有行中的任务了，停止轮询
    if (runningTasks.length === 0) {
      stopPolling()
    }
  } catch (error) {
    console.error('轮询任务状态失败:', error)
  }
}

// 生命周期
onMounted(() => {
  loadTasks()
  loadScheduledTaskTypes()  // 加载定时任务类型
})

onBeforeUnmount(() => {
  stopPolling()
})

// 监听组件可见性
watch(() => props.visible, async (newVisible) => {
  if (newVisible) {
    await Promise.all([
      loadTasks(),
      loadScheduledTaskTypes()  // 每次显示时重新加载类型列表
    ])
  } else {
    stopPolling()
  }
})

// 显示创建定时任务弹窗
const showCreateScheduledTask = () => {
  if (scheduledTaskTypes.value.length === 0) {
    showToast('没有可用的定时任务类型')
    return
  }
  resetScheduledForm()
  createScheduledVisible.value = true
}

// 提交定时任务
const submitScheduledTask = async () => {
  try {
    // 验证必填字段
    if (!scheduledForm.value.type) {
      showToast('请选择任务类型');
      return;
    }
    if (!scheduledForm.value.cronExpression) {
      showToast('请输入Cron表达式');
      return;
    }

    // 获取参数表单数据
    const params = paramFormRef.value?.getFormData();
    if (!params) {
      showToast('请填写任务参数');
      return;
    }

    // 验证必填参
    if (selectedTaskType.value?.paramConfigs) {
      const requiredParams = selectedTaskType.value.paramConfigs.filter(param => param.required);
      for (const param of requiredParams) {
        if (!params[param.key]) {
          showToast(`请填写${param.name}`);
          return;
        }
      }
    }

    // 提交任务
    await taskService.createScheduledTask({
      type: scheduledForm.value.type,
      cronExpression: scheduledForm.value.cronExpression,
      desc: scheduledForm.value.desc,
      payload: params
    });

    showToast('创建成功');
    createScheduledVisible.value = false;
    loadTasks();
  } catch (error: any) {
    showToast(error.message || '创建失败');
  }
};

// 切换定时任务状态
const toggleScheduledTask = async (task: Task) => {
  try {
    if (task.enabled) {
      await taskService.enableScheduledTask(task.id)
    } else {
      await taskService.disableScheduledTask(task.id)
    }
    showToast(task.enabled ? '已启用' : '已禁用')
  } catch (error: any) {
    showToast(error.message || '操作失败')
    // 恢复开关状态
    task.enabled = !task.enabled
  }
}

// 显示任务类型选择器
const showTaskTypePopup = () => {
  taskTypeVisible.value = true
}

// 计算属性：将任务类型转换为选择器需要的格式
const taskTypeColumns = computed(() => {
  return scheduledTaskTypes.value.filter(t => t.manuallyAdd).map(type => ({
    text: type.description,
    value: type.type
  }));
});

// 修改默认选择任务类型的处理方法
const onTaskTypeConfirm = (value: { selectedValues: Array<string> }) => {
  const selectedType = scheduledTaskTypes.value.find(type => type.type === value.selectedValues[0]);
  if (selectedType) {
    scheduledForm.value.type = selectedType.type;
    scheduledForm.value.desc = selectedType.description;
    selectedTaskType.value = selectedType;  // 设置选中的任务类型
  }
  taskTypeVisible.value = false;
};

// 加载定时任务类型
const loadScheduledTaskTypes = async () => {
  try {
    scheduledTaskTypes.value = await taskService.getScheduledTaskTypes()
  } catch (error: any) {
    console.error('获取定时任务类型失败:', error)
    showToast(error.message || '获取任务类型失败')
  }
}

// 重置表单
const resetScheduledForm = () => {
  scheduledForm.value = {
    type: '',
    desc: '',
    cronExpression: '',
    payload: {}
  };
  selectedTaskType.value = null;  // 重置选中的任务类型
};

const paramFormRef = ref<InstanceType<typeof TaskParamForm> | null>(null);

// 添加选中的任务类型状态
const selectedTaskType = ref<TaskType | null>(null);

// 添加删除相关方法
const confirmDeleteTask = (task: Task) => {
  showDialog({
    title: '确认删除',
    message: '确定要删除这个定时任务吗？',
    showCancelButton: true,
  }).then(async () => {
    try {
      await taskService.deleteScheduledTask(task.id);
      showToast('删除成功');
      loadTasks();
    } catch (error: any) {
      showToast(error.message || '删除失败');
    }
  });
};

// 添加状态文本转换函数
const getStatusText = (status: string) => {
  switch (status) {
    case 'RUNNING':
      return '运行中'
    case 'COMPLETED':
      return '已完成'
    case 'FAILED':
      return '失败'
    case 'CANCELLED':
      return '已取消'
    default:
      return status
  }
}
</script>

<style scoped>
.task-manager {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--van-background);
}

.task-list {
  flex: 1;
  overflow-y: auto;
  background-color: var(--van-background-2);
}

.task-detail {
  padding: 16px;
}

.detail-header {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.progress-section {
  margin: 12px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 0;
  border-top: 1px solid var(--van-border-color);
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  line-height: 1.5;
}

.info-item .van-icon {
  font-size: 16px;
  color: var(--van-gray-5);
  flex-shrink: 0;
}

.info-item .label {
  color: var(--van-gray-6);
  min-width: 56px;
  flex-shrink: 0;
}

.info-item .value {
  color: var(--van-text-color);
  flex: 1;
}

.info-item.message {
  align-items: flex-start;
}

.info-item.message .value {
  word-break: break-all;
}

.scheduled-section {
  margin-top: 12px;
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-top: 1px solid var(--van-border-color);
}

.detail-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

:deep(.van-dialog__header) {
  padding: 16px 16px 0;
  text-align: left;
  font-size: 15px;
  font-weight: normal;
  color: var(--van-text-color);
}

:deep(.van-dialog__content) {
  padding: 0;
}

:deep(.van-button--small) {
  padding: 0 12px;
  font-size: 13px;
}

/* 移动端适配 */
@media (max-width: 767px) {
  .task-detail {
    padding: 12px;
  }

  .detail-header {
    margin-bottom: 12px;
  }

  .info-item {
    font-size: 12px;
  }

  .info-item .label {
    min-width: 52px;
  }
}

.task-cell {
  margin-bottom: 1px;
  background-color: var(--van-background);
}

:deep(.van-cell) {
  padding: 12px 16px;
  align-items: flex-start;
}

:deep(.van-cell__title) {
  flex: 1;
  min-width: 0; /* 确保可以收缩 */
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0; /* 确保可以收缩 */
}

.task-desc {
  font-size: 14px;
  color: var(--van-text-color);
  flex: 1;
  min-width: 0; /* 确保可以收缩 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 8px; /* 确保文字不会贴着状态标签 */
}

:deep(.van-tag--medium) {
  font-size: 12px;
  padding: 0 6px;
  height: 18px;
  line-height: 18px;
  flex-shrink: 0;
  white-space: nowrap; /* 确保标签文字不换行 */
}

.task-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.task-basic-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

:deep(.van-tag--plain) {
  background: transparent;
  font-size: 12px;
  padding: 0 4px;
  height: 16px;
  line-height: 14px;
}

.update-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--van-gray-6);
}

.progress-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 4px 0;
}

.progress-text {
  font-size: 12px;
  color: var(--van-primary-color);
  min-width: 40px;
}

:deep(.van-progress) {
  flex: 1;
  --van-progress-height: 3px;
  --van-progress-background: var(--van-background-2);
}

.scheduled-info {
  margin-top: 4px;
  padding-top: 4px;
  border-top: 1px solid var(--van-border-color);
}

.scheduled-info-row {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--van-gray-6);
}

.next-execute {
  margin-left: 8px;
  color: var(--van-gray-6);
}

.scheduled-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}

:deep(.van-switch--small) {
  font-size: 16px;
}

:deep(.van-button--mini) {
  min-width: 20px;
  height: 20px;
  padding: 0 4px;
}

/* 移动端适配 */
@media (max-width: 767px) {
  .task-basic-info {
    gap: 6px;
  }

  .next-execute {
    margin-left: 6px;
  }
}

/* 状态标签颜色优化 */
:deep(.van-tag--primary) {
  --van-tag-primary-color: var(--van-primary-color);
}

:deep(.van-tag--success) {
  --van-tag-success-color: var(--van-success-color);
}

:deep(.van-tag--warning) {
  --van-tag-warning-color: var(--van-warning-color);
}

:deep(.van-tag--danger) {
  --van-tag-danger-color: var(--van-danger-color);
}

/* 图标样式统一 */
.van-icon {
  font-size: 14px;
  color: var(--van-gray-5);
}

/* 删除按钮样式优化 */
:deep(.van-button--plain.van-button--danger) {
  opacity: 0.9;
  border-color: var(--van-danger-color);
  color: var(--van-danger-color);
}

:deep(.van-button--plain.van-button--danger:active) {
  opacity: 1;
  background-color: var(--van-danger-color);
  color: white;
}

.filter-bar {
  border-bottom: 1px solid var(--van-border-color);
}

:deep(.van-tabs__wrap) {
  height: 44px;
}

:deep(.van-tab) {
  padding: 0 24px;
}

.add-scheduled-task {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 99;
}

.scheduled-task-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: var(--van-gray-6);
}

.cron-help {
  padding: 8px;
  font-size: 12px;
  line-height: 1.5;
}

.cron-help p {
  margin: 4px 0;
  white-space: nowrap;
}

:deep(.task-type-column) {
  padding: 0 16px;
}

:deep(.van-picker-column__item) {
  padding: 0 4px;
}

.scheduled-task-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

:deep(.van-button--mini) {
  min-width: 24px;
  height: 24px;
  padding: 0 5px;
}

:deep(.van-button--plain.van-button--danger) {
  opacity: 0.8;
}

:deep(.van-button--plain.van-button--danger:active) {
  opacity: 1;
}

.indefinite-progress {
  flex: 1;
  --van-loading-color: var(--van-primary-color);
}

:deep(.van-loading__spinner) {
  width: 100%;
}

:deep(.van-circle__text) {
  font-size: 14px;
}

/* 任务列表样式优化 */
.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.task-desc {
  font-size: 14px;
  font-weight: normal;
  color: var(--van-text-color);
  flex: 1;
  margin-right: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-basic-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.update-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--van-gray-6);
}

.next-execute {
  margin-left: 12px;
  color: var(--van-gray-6);
  font-weight: 500;
}

:deep(.van-tag--plain) {
  background: transparent;
  font-size: 12px;
  padding: 0 6px;
  height: 20px;
  line-height: 18px;
}

.task-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 移动端适配 */
@media (max-width: 767px) {
  .task-basic-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .update-time {
    margin-top: 4px;
  }

  .next-execute {
    margin-left: 8px;
  }
}

.preserve-whitespace {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>