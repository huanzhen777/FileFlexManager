<template>
  <div class="list-param-form">
    <div class="list-header">
      <span class="list-title">{{ param.name }}</span>
      <van-button
          type="primary"
          size="mini"
          icon="plus"
          @click="addItem"
      >添加
      </van-button>
    </div>

    <div class="list-content">
      <div v-if="items.length === 0" class="empty-tip">
        <van-empty description="暂无数据"/>
      </div>

      <div v-else class="list-items">
        <div v-for="(item, index) in items" :key="index" class="list-item">
          <div class="item-header">
            <span class="item-index">{{ index + 1 }}</span>
            <van-button
                type="danger"
                size="mini"
                icon="delete"
                @click="removeItem(index)"
            />
          </div>

          <div class="item-content">
            <ParamForm
                v-if="param.paramConfigs"
                :params="param.paramConfigs"
                :default-values="item"
                @update:params="(values) => updateItem(index, values)"
            />
            <van-field
                v-else
                v-model="items[index]"
                :placeholder="param.description"
                @update:model-value="emitUpdate"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {defineEmits, defineProps, ref, watch} from 'vue';
import ParamForm from './ParamForm.vue';
import {TaskParamConfig} from "@/types/task.ts";

const props = defineProps<{
  param: TaskParamConfig;
  modelValue?: any[];
}>();

const emit = defineEmits(['update:modelValue']);

const items = ref<any[]>([]);


// 初始化数据
watch(() => props.modelValue, (newValue) => {
  try {
    // 处理字符串形式的数组
    let parsedValue = newValue;
    if (typeof newValue === 'string') {
      try {
        parsedValue = JSON.parse(newValue);
      } catch (e) {
        parsedValue = [];
      }
    }
    items.value = Array.isArray(parsedValue) ? [...parsedValue] : [];
  } catch (e) {
    items.value = [];
  }
}, {immediate: true});

// 添加项目
const addItem = () => {
  const newItem = props.param.paramConfigs ? {} : '';
  items.value.push(newItem);
  emitUpdate();
};

// 删除项目
const removeItem = (index: number) => {
  items.value.splice(index, 1);
  emitUpdate();
};

// 更新项目
const updateItem = (index: number, values: any) => {
  items.value[index] = values;
  emitUpdate();
};

// 发送更新事件
const emitUpdate = () => {
  emit('update:modelValue', items.value);
};
</script>

<style scoped>
.list-param-form {
  background-color: var(--van-background-2);
  border-radius: 4px;
  padding: 8px;
  margin: 4px 0;
  display: flex;
  flex-direction: column;
  max-height: 60vh;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  flex-shrink: 0;
}

.list-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--van-text-color);
}

.list-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding-right: 4px;
}

.list-content::-webkit-scrollbar {
  width: 4px;
}

.list-content::-webkit-scrollbar-thumb {
  background-color: var(--van-gray-4);
  border-radius: 2px;
}

.list-content::-webkit-scrollbar-track {
  background-color: var(--van-gray-1);
  border-radius: 2px;
}

.list-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 1px;
}

.list-item {
  background-color: var(--van-background);
  border-radius: 4px;
  padding: 8px;
  border: 1px solid var(--van-gray-3);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.item-index {
  font-size: 12px;
  color: var(--van-gray-6);
  background-color: var(--van-gray-2);
  padding: 2px 8px;
  border-radius: 10px;
}

.item-content {
  padding-left: 4px;
  border-left: 2px solid var(--van-primary-color);
}

.empty-tip {
  padding: 16px 0;
}

:deep(.van-cell) {
  padding: 8px 12px;
}

:deep(.van-field__label) {
  width: 5em;
}

:deep(.van-button--mini) {
  min-width: unset;
  padding: 0 8px;
  height: 24px;
  line-height: 22px;
}

/* 嵌套的 ParamForm 样式优化 */
:deep(.task-param-form) {
  padding: 0;
}

:deep(.van-cell:not(:last-child)::after) {
  border-bottom: none;
}
</style> 