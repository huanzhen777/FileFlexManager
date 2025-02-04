<template>
  <div class="task-param-form">
    <template v-for="param in params" :key="param.key">
      <!-- 文件夹选择 -->
      <van-field
          v-if="param.type === 'FOLDER' || param.type === 'FOLDER_MULTI_SELECT' || param.type === 'FOLDER_FILE_MULTI_SELECT'|| param.type === 'FOLDER_FILE'"
          :model-value="formatPathValue(param, formData[param.key])"
          :required="param.required"
          readonly
          is-link
          @click="showFolderPicker(param)"
      >
        <template #label>
          <LabelWithHelp :label="param.name" :description="param.description"/>
        </template>
        <!-- 多选时显示选中数量 -->
        <template #right-icon v-if="isMultiSelectParam(param) && Array.isArray(formData[param.key])">
          <span class="selected-count">已选 {{ formData[param.key].length }} 项</span>
        </template>
      </van-field>

      <!-- 文本输入 -->
      <van-field
          v-else-if="param.type === 'TEXT'"
          v-model="formData[param.key]"
          :placeholder="param.description"
          :required="param.required"
      >
        <template #label>
          <LabelWithHelp :label="param.name" :description="param.description"/>
        </template>
      </van-field>

      <!-- 数字输入 -->
      <van-field
          v-else-if="param.type === 'NUMBER'"
          v-model="formData[param.key]"
          :placeholder="param.description"
          type="number"
          :required="param.required"
      >
        <template #label>
          <LabelWithHelp :label="param.name" :description="param.description"/>
        </template>
      </van-field>

      <!-- 选择器 -->
      <van-field
          v-else-if="param.type === 'SELECT'"
          :model-value="getOptionLabel(param, formData[param.key])"
          :placeholder="param.description"
          readonly
          is-link
          :required="param.required"
          @click="showOptionPicker(param)"
      >
        <template #label>
          <LabelWithHelp :label="param.name" :description="param.description"/>
        </template>
      </van-field>

      <!-- 布尔值开关 -->
      <van-cell v-else-if="param.type === 'BOOLEAN'" center>
        <template #title>
          <LabelWithHelp :label="param.name" :description="param.description"/>
        </template>
        <template #right-icon>
          <van-switch v-model="formData[param.key]" size="20"/>
        </template>
      </van-cell>

      <!-- 列表参数 -->
      <ListParamForm
          v-else-if="param.type === 'LIST'"
          :param="param"
          v-model="formData[param.key]"
      />
    </template>

    <!-- 添加按钮组 -->
    <div class="form-actions" v-if="showActions">
      <van-button plain type="default" @click="handleCancel">取消</van-button>
      <van-button type="primary" @click="handleConfirm">确认</van-button>
    </div>

    <!-- 文件夹选择器 -->
    <FolderPicker
        v-model:visible="folderPickerVisible"
        :title="currentParam?.name || '选择文件夹'"
        @select="handleFolderSelect"
        teleport="body"
        :multiSelect="currentParam?.type === 'FOLDER_MULTI_SELECT' || currentParam?.type === 'FOLDER_FILE_MULTI_SELECT'"
        :allowFile="currentParam?.type === 'FOLDER_FILE' || currentParam?.type === 'FOLDER_FILE_MULTI_SELECT'"
    />

    <!-- 选项选择器 -->
    <van-popup v-model:show="optionPickerVisible" position="bottom">
      <van-picker
          :columns="currentOptions"
          :title="currentParam?.name || '请选择'"
          @confirm="handleOptionConfirm"
          @cancel="optionPickerVisible = false"
          show-toolbar
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import {defineEmits, defineProps, nextTick, ref, watch} from 'vue';
import {showToast} from 'vant';
import FolderPicker from '@/components/FolderPicker.vue';
import ListParamForm from '@/components/param/ListParamForm.vue';
import LabelWithHelp from '@/components/LabelWithHelp.vue';
import {TaskParamConfig} from "@/types/task.ts";

const props = defineProps<{
  params: TaskParamConfig[],
  defaultValues?: Record<string, any>,
  showActions?: boolean
}>();

const emit = defineEmits(['update:params', 'cancel', 'confirm']);

const formData = ref<Record<string, any>>({});

// 初始化表单数据
const initFormData = () => {
  let initialValues: Record<string, any> = {};

  // 处理默认值
  if (props.defaultValues) {
    try {
      initialValues = typeof props.defaultValues === 'string'
          ? JSON.parse(props.defaultValues)
          : {...props.defaultValues};
    } catch (e) {
      console.warn('Invalid defaultValues:', props.defaultValues);
      initialValues = {};
    }
  }

  // 设置参数配置中的默认值，如果没有被 defaultValues 覆盖的话
  props.params.forEach(param => {
    if (!(param.key in initialValues)) {
      let defaultValue: any = param.defaultValue;

      // 处理特殊类型的默认值
      if (defaultValue !== undefined) {
        initialValues[param.key] = defaultValue;
      }
    }
  });

  formData.value = initialValues;
};

// 监听表单数据变化，但排除递归更新
let isUpdating = false;
watch(formData, () => {
  if (!isUpdating) {
    isUpdating = true;
    emit('update:params', formData.value);
    nextTick(() => {
      isUpdating = false;
    });
  }
}, {deep: true});

// 监听 defaultValues 变化
watch(() => props.defaultValues, (newValues) => {
  if (newValues && !isUpdating) {
    isUpdating = true;
    try {
      const parsedValues = typeof newValues === 'string'
          ? JSON.parse(newValues)
          : newValues;

      formData.value = {
        ...formData.value,
        ...parsedValues
      };
    } catch (e) {
      console.warn('Invalid defaultValues:', newValues);
    }
    nextTick(() => {
      isUpdating = false;
    });
  }
}, {deep: true});

// 文件夹选择相关
const folderPickerVisible = ref(false);
const currentParam = ref<TaskParamConfig | null>(null);

// 显示文件夹选择器
const showFolderPicker = (param: TaskParamConfig) => {
  currentParam.value = param;
  folderPickerVisible.value = true;
};

// 处理文件夹选择
const handleFolderSelect = (paths: string[]) => {
  if (currentParam.value) {
    const isMultiSelect = currentParam.value.type === 'FOLDER_MULTI_SELECT' || currentParam.value.type === 'FOLDER_FILE_MULTI_SELECT';
    formData.value[currentParam.value.key] = isMultiSelect ? paths : paths[0];
  }
  folderPickerVisible.value = false;
};

// 选项选择器相关
const optionPickerVisible = ref(false);
const currentOptions = ref<{ text: string; value: string }[]>([]);

// 显示选项选择器
const showOptionPicker = (param: TaskParamConfig) => {
  if (!param.options || param.options.length === 0) {
    showToast('没有可选项');
    return;
  }
  currentParam.value = param;
  currentOptions.value = param.options.map(opt => ({
    text: opt.label,
    value: opt.value
  }));
  optionPickerVisible.value = true;
};

// 处理选项确认
const handleOptionConfirm = ({selectedOptions}: { selectedOptions: { text: string; value: string }[] }) => {
  if (currentParam.value && selectedOptions[0]) {
    formData.value[currentParam.value.key] = selectedOptions[0].value;
  }
  optionPickerVisible.value = false;
};

// 获取表单数据
const getFormData = () => {
  if (!validate()) {
    return false;
  }
  return formData.value;
};

// 重置表单数据
const resetForm = () => {
  formData.value = {};
  initFormData();
};

// 初始化
initFormData();

// 获取选项标签
const getOptionLabel = (param: TaskParamConfig, value: string): string => {
  if (!value || !param.options) return '';
  const option = param.options.find(opt => opt.value === value);
  return option?.label || '';
};

// 验证单个参数值
const validateValue = (param: TaskParamConfig, value: any): string[] => {
  if (!param.required) {
    return [];
  }

  if (param.type === 'LIST') {
    if (!value || value.length === 0) {
      return [param.name];
    }
    // 如果有子配置，递归验证每个子项
    if (param.paramConfigs && param.paramConfigs.length > 0) {
      const invalidFields: string[] = [];
      value.forEach((item: any, index: number) => {
        param.paramConfigs?.forEach(subParam => {
          const subValue = item[subParam.key];
          const subInvalidFields = validateValue(subParam, subValue);
          if (subInvalidFields.length > 0) {
            invalidFields.push(`${param.name}[${index + 1}]: ${subInvalidFields.join(', ')}`);
          }
        });
      });
      return invalidFields;
    }
  }

  return (value === undefined || value === null || value === '') ? [param.name] : [];
};

// 表单验证
const validate = (): boolean => {
  const invalidFields: string[] = [];

  props.params.forEach(param => {
    const value = formData.value[param.key];
    const fieldErrors = validateValue(param, value);
    invalidFields.push(...fieldErrors);
  });

  if (invalidFields.length > 0) {
    showToast(`请填写必填项: ${invalidFields.join('; ')}`);
    return false;
  }
  return true;
};

// 处理取消
const handleCancel = () => {
  emit('cancel');
};

// 处理确认
const handleConfirm = () => {
  if (validate()) {
    emit('confirm', formData.value);
  }
};

// 添加新的工具函数
const isMultiSelectParam = (param: TaskParamConfig): boolean => {
  return param.type === 'FOLDER_MULTI_SELECT' || param.type === 'FOLDER_FILE_MULTI_SELECT';
};

// 格式化路径值显示
const formatPathValue = (param: TaskParamConfig, value: string | string[]): string => {
  if (!value) return '';
  
  if (Array.isArray(value)) {
    if (value.length === 0) return '';
    if (value.length === 1) return value[0];
    return `${value[0]} 等 ${value.length} 个路径`;
  }
  
  return value;
};

defineExpose({
  getFormData,
  resetForm,
  validate
});
</script>

<style scoped>
.task-param-form {
  padding: 16px 0;
}

:deep(.van-field__label) {
  width: 6em;
}

.param-description {
  font-size: 12px;
  color: var(--van-gray-6);
  margin-top: 4px;
  display: block;
}

:deep(.van-cell) {
  padding: 12px 16px;
}

:deep(.van-switch) {
  margin-left: 8px;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  padding: 16px;
  border-top: 1px solid var(--van-gray-3);
  margin-top: 16px;
}
</style> 