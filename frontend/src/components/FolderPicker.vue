<template>
  <van-popup
    v-model:show="show"
    position="bottom"
    round
    :style="{ height: '80%' }"
    teleport="body"
  >
    <div class="folder-picker">
      <van-nav-bar
        :title="title"
        left-text="取消"
        right-text="确认"
        @click-left="handleCancel"
        @click-right="handleConfirm"
      />
      
      <FileList
        ref="fileListRef"
        :initial-path="initialPath"
        :mode="FileListModeEnum.FOLDER_SELECT"
        :multi-select="multiSelect"
        :allow-file="allowFile"
        @select="handleFolderSelect"
      />
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import FileList from '@/views/FileList.vue';
import { FileListModeEnum } from "@/types";

const props = defineProps<{
  visible: boolean;
  title?: string;
  initialPath?: string;
  multiSelect?: boolean;
  allowFile?: boolean;
}>();

const emit = defineEmits(['update:visible', 'select']);

const show = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
});

const selectedPaths = ref<string[]>([]);
const fileListRef = ref();

// 处理文件夹选择
const handleFolderSelect = (path: string, selected: boolean) => {
  if (props.multiSelect) {
    // 多选模式
    if (selected) {
      selectedPaths.value.push(path);
    } else {
      const index = selectedPaths.value.indexOf(path);
      if (index > -1) {
        selectedPaths.value.splice(index, 1);
      }
    }
  } else {
    // 单选模式
    selectedPaths.value = selected ? [path] : [];
  }
};

// 处理取消
const handleCancel = () => {
  selectedPaths.value = [];
  show.value = false;
};

// 处理确认
const handleConfirm = () => {
  if (selectedPaths.value.length > 0) {
    emit('select', selectedPaths.value);
  }
  show.value = false;
  selectedPaths.value = [];
};
</script>

<style scoped>
.folder-picker {
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.van-nav-bar) {
  flex-shrink: 0;
}
</style> 