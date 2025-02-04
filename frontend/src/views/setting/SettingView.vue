<template>
  <div class="setting-page">
    <van-nav-bar
      title="系统设置"
      left-arrow
      @click-left="router.back()"
    />

    <div class="setting-content">
      <!-- 按类型分组显示设置项 -->
      <div v-for="(configs, type) in groupedConfigs" :key="type" class="setting-group">
        <van-cell-group :title="configs[0]?.typeDesc">
          <van-cell
            v-for="config in configs"
            :key="config.name"
            :value="formatValue(config)"
            is-link
            @click="showEditDialog(config)"
          >
            <template #title>
              <LabelWithHelp :label="config.title" :description="config.description" />
            </template>
          </van-cell>
        </van-cell-group>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <van-dialog
      v-model:show="showDialog"
      :title="currentConfig?.title"
      :show-cancel-button="!currentConfig?.paramConfig"
      :show-confirm-button="!currentConfig?.paramConfig"
      @confirm="handleConfirm"
    >
      <div class="dialog-content">
        <div class="config-desc">
          {{ currentConfig?.description }}
        </div>
        <!-- 使用 TaskParamForm 组件 -->
        <task-param-form
          v-if="currentConfig?.paramConfig"
          ref="paramFormRef"
          :params="[currentConfig.paramConfig]"
          :default-values="{ [currentConfig.paramConfig.key]: currentConfig.value }"
          :show-actions="true"
          @cancel="showDialog = false"
          @confirm="handleParamFormConfirm"
        />
        <!-- 对于没有 paramConfig 的配置项，保持原有的输入框 -->
        <van-field
          v-else
          v-model="editValue"
          :placeholder="`请输入${currentConfig?.title}`"
        />
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getConfigs, updateConfig, type ConfigItem } from '@/api/setting'
import { ParamForm as TaskParamForm, LabelWithHelp } from '@/components'

const router = useRouter()

const configs = ref<ConfigItem[]>([])
const showDialog = ref(false)
const currentConfig = ref<ConfigItem | null>(null)
const editValue = ref('')
const paramFormRef = ref<InstanceType<typeof TaskParamForm> | null>(null)

// 按类型分组的配置
const groupedConfigs = computed(() => {
  const groups: Record<string, ConfigItem[]> = {}
  configs.value.forEach(config => {
    if (!groups[config.type]) {
      groups[config.type] = []
    }
    groups[config.type].push(config)
  })
  return groups
})

// 显示编辑弹窗
const showEditDialog = (config: ConfigItem) => {
  currentConfig.value = config
  editValue.value = config.value
  showDialog.value = true
}

// 确认修改
const handleConfirm = async () => {
  if (!currentConfig.value) return

  try {
    // 对于没有参数配置的普通输入框
    await updateConfig({
      name: currentConfig.value.name,
      value: editValue.value
    })
    // 更新本地数据
    const config = configs.value.find(c => c.name === currentConfig.value?.name)
    if (config) {
      config.value = editValue.value
    }
    showDialog.value = false
  } catch (error) {
    // 错误已经被 api 拦截器处理
  }
}

// 处理参数表单确认
const handleParamFormConfirm = async (formData: Record<string, any>) => {
  if (!currentConfig.value?.paramConfig) return

  try {
    const newValue = formData[currentConfig.value.paramConfig.key]
    await updateConfig({
      name: currentConfig.value.name,
      value: newValue
    })
    // 更新本地数据
    const config = configs.value.find(c => c.name === currentConfig.value?.name)
    if (config) {
      config.value = newValue
    }
    showDialog.value = false
  } catch (error) {
    // 错误已经被 api 拦截器处理
  }
}

// 获取配置列表
const fetchConfigs = async () => {
  try {
    const res = await getConfigs()
    configs.value = res.data
  } catch (error) {
    // 错误已经被 api 拦截器处理
  }
}

const formatValue = (config: ConfigItem) => {
  if (config.paramConfig?.type === 'LIST') {
    return ""
  }
  return config.value.toString()
}

onMounted(() => {
  fetchConfigs()
})
</script>

<style scoped>
.setting-page {
  height: 100vh;
  background-color: #f7f8fa;
}

.setting-content {
  padding: 12px 0;
}

.setting-group {
  margin-bottom: 12px;
}

.dialog-content {
  padding: 16px;
}

.config-desc {
  font-size: 13px;
  color: #666;
  padding: 0 0 16px;
  line-height: 1.5;
}
</style> 